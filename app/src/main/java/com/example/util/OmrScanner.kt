package com.example.util

import android.graphics.Bitmap
import android.util.Log
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object OmrScanner {
    private const val TAG = "OmrScanner"

    data class ScanResult(
        val studentId: String,
        val paperSet: String,
        val answers: List<Int>,
        val annotatedBitmap: Bitmap,
        val optionCoords: List<List<Pair<Float, Float>>> = emptyList()
    )

    fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int, templateType: String = "Standard"): ScanResult {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        
        // Exact A4 dimensions matching OmrGenerator (1000 x 1414)
        val w = 1000.0
        val h = 1414.0
        
        val warped = Mat()
        val warpedGray = Mat()
        
        // Step 1: Detect paper contour or resize to standard A4
        val corners = findDocumentCorners(gray)
        if (corners != null && corners.size == 4) {
            val srcMat = MatOfPoint2f(*corners.toTypedArray())
            val dstMat = MatOfPoint2f(
                Point(0.0, 0.0), Point(w, 0.0), Point(w, h), Point(0.0, h)
            )
            val pTransform = Imgproc.getPerspectiveTransform(srcMat, dstMat)
            Imgproc.warpPerspective(mat, warped, pTransform, Size(w, h))
            Imgproc.warpPerspective(gray, warpedGray, pTransform, Size(w, h))
            srcMat.release()
            dstMat.release()
            pTransform.release()
        } else {
            Imgproc.resize(mat, warped, Size(w, h))
            Imgproc.resize(gray, warpedGray, Size(w, h))
        }

        var warpedBlurred = Mat()
        Imgproc.GaussianBlur(warpedGray, warpedBlurred, Size(5.0, 5.0), 0.0)
        
        var warpedThresh = Mat()
        Imgproc.adaptiveThreshold(warpedBlurred, warpedThresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 31, 15.0)

        // Step 2: Fine-calibration using the 4 solid black corner registration markers (40x40 squares at centers 50,50 / 950,50 / 50,1364 / 950,1364)
        val regCorners = findRegistrationMarkers(warpedThresh)
        var finalWarped = warped
        var finalThresh = warpedThresh
        if (regCorners != null && regCorners.size == 4) {
            val srcMat = MatOfPoint2f(*regCorners.toTypedArray())
            val dstMat = MatOfPoint2f(
                Point(50.0, 50.0), Point(950.0, 50.0), Point(950.0, 1364.0), Point(50.0, 1364.0)
            )
            val fineTransform = Imgproc.getPerspectiveTransform(srcMat, dstMat)
            val warpedFine = Mat()
            val threshFine = Mat()
            Imgproc.warpPerspective(warped, warpedFine, fineTransform, Size(w, h))
            Imgproc.warpPerspective(warpedThresh, threshFine, fineTransform, Size(w, h))
            
            finalWarped = warpedFine
            finalThresh = threshFine
            
            srcMat.release()
            dstMat.release()
            fineTransform.release()
        }

        val warpedAnnotated = finalWarped.clone()
        val colorRed = Scalar(239.0, 68.0, 68.0, 255.0)
        val colorGreen = Scalar(34.0, 197.0, 94.0, 255.0)
        val colorBlue = Scalar(59.0, 130.0, 246.0, 255.0)
        val colorYellow = Scalar(234.0, 179.0, 8.0, 255.0)

        // Find bubble contours for micro-snapping (within 10px radius)
        val bubbleCenters = mutableListOf<Point>()
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(finalThresh.clone(), contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > 80 && area < 1500) {
                val contour2f = MatOfPoint2f(*contour.toArray())
                val perimeter = Imgproc.arcLength(contour2f, true)
                if (perimeter > 0) {
                    val circularity = 4 * Math.PI * area / (perimeter * perimeter)
                    if (circularity > 0.55) {
                        val rect = Imgproc.boundingRect(contour)
                        val cx = rect.x + rect.width / 2.0
                        val cy = rect.y + rect.height / 2.0
                        bubbleCenters.add(Point(cx, cy))
                    }
                }
                contour2f.release()
            }
        }

        fun snapToNearest(cx: Double, cy: Double, centers: List<Point>, maxDist: Double): Point {
            var bestPt = Point(cx, cy)
            var minDist = maxDist
            for (pt in centers) {
                val dist = Math.hypot(pt.x - cx, pt.y - cy)
                if (dist < minDist) {
                    minDist = dist
                    bestPt = pt
                }
            }
            return bestPt
        }

        // ==========================================
        // EXACT SET DETECTION (Matches OmrGenerator)
        // boxLeft = 110, boxTop = 510
        // setGridStartX = 215.0, setGridStartY = 590.0, rowHeight = 60.0
        // ==========================================
        val setStartX = 215.0
        val setStartY = 590.0
        val setSpacingY = 60.0
        val setSets = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        var bestSetRow = -1
        var maxSetDarkness = 0.0
        var secondMaxSetDarkness = 0.0
        val setBubbleRadius = 13.0

        for (i in setSets.indices) {
            val rawCx = setStartX
            val rawCy = setStartY + i * setSpacingY
            
            // Micro-snap within 10px
            val snapped = snapToNearest(rawCx, rawCy, bubbleCenters, 10.0)
            val cx = snapped.x
            val cy = snapped.y

            val fillPercentage = getFillPercentage(finalThresh, cx, cy, setBubbleRadius)
            Imgproc.circle(warpedAnnotated, Point(cx, cy), setBubbleRadius.toInt(), colorBlue, 2)

            if (fillPercentage > maxSetDarkness) {
                secondMaxSetDarkness = maxSetDarkness
                maxSetDarkness = fillPercentage
                bestSetRow = i
            } else if (fillPercentage > secondMaxSetDarkness) {
                secondMaxSetDarkness = fillPercentage
            }
        }

        val fillThreshold = 0.28
        val marginThreshold = 0.15

        val paperSet = if (maxSetDarkness > fillThreshold && bestSetRow >= 0) {
            if (secondMaxSetDarkness > maxSetDarkness * 0.75 && (maxSetDarkness - secondMaxSetDarkness) < marginThreshold) {
                "MULTIPLE"
            } else {
                val rawCx = setStartX
                val rawCy = setStartY + bestSetRow * setSpacingY
                val snapped = snapToNearest(rawCx, rawCy, bubbleCenters, 10.0)
                Imgproc.circle(warpedAnnotated, snapped, setBubbleRadius.toInt(), colorGreen, -1)
                setSets[bestSetRow]
            }
        } else {
            "BLANK"
        }

        // ==========================================
        // EXACT QUESTION BUBBLE GRID (Matches OmrGenerator)
        // splitX = 270.0, colWidth = 130.0
        // headerBottom = 580.0, qRowHeight = 39.0
        // bubblesStartX = colStartX + 46.0
        // cy = 580.0 + row * 39.0 + 19.5 = 599.5 + row * 39.0
        // optSpacing = 21.0, bubbleRadius = 10.0
        // ==========================================
        val splitX = 270.0
        val colWidth = 130.0
        val headerBottom = 580.0
        val qRowHeight = 39.0
        val optSpacing = 21.0
        val qBubbleRadius = 10.0
        val questionsPerColumn = 20

        val answers = mutableListOf<Int>()
        val allOptionCoords = mutableListOf<List<Pair<Float, Float>>>()

        for (q in 0 until numQuestions) {
            val col = q / questionsPerColumn
            val row = q % questionsPerColumn
            
            val bubblesStartX = splitX + col * colWidth + 46.0
            val qCenterY = headerBottom + row * qRowHeight + (qRowHeight / 2.0) // 599.5 + row * 39.0

            var maxDarkness = 0.0
            var secondMaxDarkness = 0.0
            var bestOpt = -1

            val currentOptionCoords = mutableListOf<Pair<Float, Float>>()

            for (opt in 0 until numOptions) {
                val rawCx = bubblesStartX + opt * optSpacing
                val rawCy = qCenterY
                
                // Micro-snap within 9px
                val snapped = snapToNearest(rawCx, rawCy, bubbleCenters, 9.0)
                val cx = snapped.x
                val cy = snapped.y

                currentOptionCoords.add(Pair(cx.toFloat(), cy.toFloat()))

                val fillPercentage = getFillPercentage(finalThresh, cx, cy, qBubbleRadius)
                Imgproc.circle(warpedAnnotated, Point(cx, cy), qBubbleRadius.toInt(), colorBlue, 2)

                if (fillPercentage > maxDarkness) {
                    secondMaxDarkness = maxDarkness
                    maxDarkness = fillPercentage
                    bestOpt = opt
                } else if (fillPercentage > secondMaxDarkness) {
                    secondMaxDarkness = fillPercentage
                }
            }

            allOptionCoords.add(currentOptionCoords)
            var studentAns = -1

            if (maxDarkness > fillThreshold && bestOpt >= 0) {
                if (secondMaxDarkness > maxDarkness * 0.75 && (maxDarkness - secondMaxDarkness) < marginThreshold) {
                    studentAns = -2 // Multiple marked
                } else {
                    studentAns = bestOpt
                }
            }

            if (studentAns >= 0) {
                val rawCx = bubblesStartX + studentAns * optSpacing
                val rawCy = qCenterY
                val snapped = snapToNearest(rawCx, rawCy, bubbleCenters, 9.0)
                Imgproc.circle(warpedAnnotated, snapped, qBubbleRadius.toInt(), colorGreen, -1)
            } else if (studentAns == -2) {
                val rawCx = bubblesStartX + bestOpt * optSpacing
                val rawCy = qCenterY
                val snapped = snapToNearest(rawCx, rawCy, bubbleCenters, 9.0)
                Imgproc.circle(warpedAnnotated, snapped, qBubbleRadius.toInt(), colorYellow, -1)
            }

            answers.add(studentAns)
        }

        val finalBitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warpedAnnotated, finalBitmap)

        // Read QR code for candidate identity
        var studentId = "UNKNOWN"
        try {
            val qr = readQr(bitmap) ?: readQr(finalBitmap)
            if (qr != null) {
                studentId = qr
            }
        } catch (e: Exception) {
            Log.e(TAG, "QR read error", e)
        }

        // Memory cleanup
        mat.release()
        gray.release()
        warped.release()
        warpedGray.release()
        warpedBlurred.release()
        warpedThresh.release()
        if (finalWarped != warped) finalWarped.release()
        if (finalThresh != warpedThresh) finalThresh.release()
        warpedAnnotated.release()
        hierarchy.release()

        return ScanResult(studentId, paperSet, answers, finalBitmap, allOptionCoords)
    }

    private fun findRegistrationMarkers(thresh: Mat): List<Point>? {
        val width = thresh.width()
        val height = thresh.height()
        
        // Define 4 search regions around the expected positions of 40x40 black calibration boxes
        // Expected centers: TL(50,50), TR(950,50), BL(50,1364), BR(950,1364)
        val rois = listOf(
            Rect(10, 10, 160, 160),                         // Top-Left
            Rect(width - 170, 10, 160, 160),                // Top-Right
            Rect(width - 170, height - 170, 160, 160),      // Bottom-Right
            Rect(10, height - 170, 160, 160)                // Bottom-Left
        )
        
        val centers = mutableListOf<Point>()
        
        for (roiRect in rois) {
            if (roiRect.x < 0 || roiRect.y < 0 || roiRect.x + roiRect.width > width || roiRect.y + roiRect.height > height) {
                return null
            }
            val subRoi = thresh.submat(roiRect)
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(subRoi, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            
            var bestCenter: Point? = null
            var bestAreaDiff = Double.MAX_VALUE
            val targetArea = 1600.0 // 40x40 square
            
            for (c in contours) {
                val area = Imgproc.contourArea(c)
                if (area in 400.0..4000.0) {
                    val rect = Imgproc.boundingRect(c)
                    val ratio = rect.width.toDouble() / rect.height
                    if (ratio in 0.6..1.6) {
                        val diff = Math.abs(area - targetArea)
                        if (diff < bestAreaDiff) {
                            bestAreaDiff = diff
                            bestCenter = Point(
                                roiRect.x + rect.x + rect.width / 2.0,
                                roiRect.y + rect.y + rect.height / 2.0
                            )
                        }
                    }
                }
            }
            
            subRoi.release()
            hierarchy.release()
            
            if (bestCenter != null) {
                centers.add(bestCenter)
            } else {
                return null // Not all 4 markers could be confidently identified
            }
        }
        
        return if (centers.size == 4) centers else null
    }

    private fun detectTimingMarks(thresh: Mat): Pair<List<Point>, List<Point>> {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh.clone(), contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        val leftMarks = mutableListOf<Point>()
        val rightMarks = mutableListOf<Point>()
        val width = thresh.width()

        for (c in contours) {
            val rect = Imgproc.boundingRect(c)
            val area = rect.width * rect.height

            if (area < 50 || area > 1500) continue

            val ratio = rect.width.toDouble() / rect.height
            if (ratio < 1.5) continue

            val center = Point(
                rect.x + rect.width / 2.0,
                rect.y + rect.height / 2.0
            )

            if (center.x < width * 0.15) {
                leftMarks.add(center)
            }
            if (center.x > width * 0.85) {
                rightMarks.add(center)
            }
        }

        leftMarks.sortBy { it.y }
        rightMarks.sortBy { it.y }
        
        hierarchy.release()
        return Pair(leftMarks, rightMarks)
    }

    // 5. Better Fill Percentage
    private fun getFillPercentage(threshInv: Mat, cx: Double, cy: Double, radius: Double): Double {
        val r = (radius * 0.5).toInt() // User requested 0.5
        val x = (cx - r).toInt()
        val y = (cy - r).toInt()
        val w = r * 2
        val h = r * 2

        if (x < 0 || y < 0 || x + w > threshInv.width() || y + h > threshInv.height() || w <= 0 || h <= 0) {
            return 0.0
        }

        val roi = threshInv.submat(Rect(x, y, w, h))
        val mask = Mat.zeros(roi.size(), CvType.CV_8U)
        
        Imgproc.circle(mask, Point(roi.width() / 2.0, roi.height() / 2.0), r, Scalar(255.0), -1)

        val maskedRoi = roi.clone()
        Core.bitwise_and(maskedRoi, mask, maskedRoi)

        val filled = Core.countNonZero(maskedRoi)
        val total = Core.countNonZero(mask)

        roi.release()
        mask.release()
        maskedRoi.release()

        return if (total > 0) filled.toDouble() / total else 0.0
    }

    private fun readQr(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binary = BinaryBitmap(HybridBinarizer(source))

            val hints = java.util.EnumMap<com.google.zxing.DecodeHintType, Any>(com.google.zxing.DecodeHintType::class.java)
            hints[com.google.zxing.DecodeHintType.TRY_HARDER] = true

            MultiFormatReader().decode(binary, hints).text
        } catch (e: Exception) {
            null
        }
    }
    
    private fun findDocumentCorners(gray: Mat): List<Point>? {
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
        val edged = Mat()
        Imgproc.Canny(blurred, edged, 75.0, 200.0)
        
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(edged, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        
        contours.sortByDescending { Imgproc.contourArea(it) }
        
        for (contour in contours) {
            val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.02 * peri, true)
            
            if (approx.total() == 4L) {
                val area = Imgproc.contourArea(contour)
                if (area > gray.width() * gray.height() * 0.5) {
                    val points = approx.toList()
                    val sortedByY = points.sortedBy { it.y }
                    val top = sortedByY.take(2).sortedBy { it.x }
                    val bottom = sortedByY.drop(2).sortedBy { it.x }
                    
                    blurred.release()
                    edged.release()
                    hierarchy.release()
                    return listOf(top[0], top[1], bottom[1], bottom[0])
                }
            }
        }
        blurred.release()
        edged.release()
        hierarchy.release()
        return null
    }
}
