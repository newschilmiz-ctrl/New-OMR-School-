with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write("""package com.example.util

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
        
        val w = 1000.0
        val h = 1000.0
        
        // ML Kit already provides a cropped document. We just need to warp/resize it to our 1000x1000 canvas.
        val warped = Mat()
        val warpedGray = Mat()
        
        val corners = findDocumentCorners(gray)
        if (corners != null && corners.size == 4) {
            val srcMat = MatOfPoint2f(*corners.toTypedArray())
            val dstMat = MatOfPoint2f(
                Point(0.0, 0.0), Point(w, 0.0), Point(w, h), Point(0.0, h)
            )
            val pTransform = Imgproc.getPerspectiveTransform(srcMat, dstMat)
            Imgproc.warpPerspective(mat, warped, pTransform, Size(w, h))
            Imgproc.warpPerspective(gray, warpedGray, pTransform, Size(w, h))
        } else {
            Imgproc.resize(mat, warped, Size(w, h))
            Imgproc.resize(gray, warpedGray, Size(w, h))
        }

        val warpedBlurred = Mat()
        Imgproc.GaussianBlur(warpedGray, warpedBlurred, Size(5.0, 5.0), 0.0)
        
        val warpedThresh = Mat()
        Imgproc.adaptiveThreshold(warpedBlurred, warpedThresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 31, 15.0)

        val warpedAnnotated = warped.clone()
        val colorRed = Scalar(255.0, 0.0, 0.0, 255.0)
        val colorGreen = Scalar(0.0, 255.0, 0.0, 255.0)
        val colorBlue = Scalar(0.0, 0.0, 255.0, 255.0)
        val colorYellow = Scalar(255.0, 255.0, 0.0, 255.0)

        // 1. Timing Marks Detection
        val (leftMarks, rightMarks) = detectTimingMarks(warpedThresh)
        
        // Fallback for timing marks if they fail
        val topY = if (leftMarks.isNotEmpty()) leftMarks.first().y else 50.0
        val bottomY = if (leftMarks.isNotEmpty()) leftMarks.last().y else 950.0
        val numRows = Math.max(1, leftMarks.size - 1)
        val rowStep = (bottomY - topY) / 19.0 // As requested: (bottomY - topY) / 19.0

        // 2. Bubble Detection Upgrade
        val bubbleCenters = mutableListOf<Point>()
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(warpedThresh.clone(), contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > 80 && area < 1200) {
                val contour2f = MatOfPoint2f(*contour.toArray())
                val perimeter = Imgproc.arcLength(contour2f, true)
                if (perimeter > 0) {
                    val circularity = 4 * Math.PI * area / (perimeter * perimeter)
                    if (circularity > 0.65) {
                        val rect = Imgproc.boundingRect(contour)
                        val cx = rect.x + rect.width / 2.0
                        val cy = rect.y + rect.height / 2.0
                        bubbleCenters.add(Point(cx, cy))
                    }
                }
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

        // SET detection
        val setStartX = 120.0
        val setStartY = 50.0
        val setSpacingY = 94.736
        val setSets = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        var bestSetRow = -1
        var maxSetDarkness = 0.0
        var secondMaxSetDarkness = 0.0
        val bubbleRadius = 14.0

        for (i in setSets.indices) {
            var cx = setStartX
            var cy = setStartY + i * setSpacingY
            
            // Snap to nearest detected bubble
            val snapped = snapToNearest(cx, cy, bubbleCenters, 35.0)
            cx = snapped.x
            cy = snapped.y

            val fillPercentage = getFillPercentage(warpedThresh, cx, cy, bubbleRadius)
            Imgproc.circle(warpedAnnotated, Point(cx, cy), bubbleRadius.toInt(), colorBlue, 2)

            if (fillPercentage > maxSetDarkness) {
                secondMaxSetDarkness = maxSetDarkness
                maxSetDarkness = fillPercentage
                bestSetRow = i
            } else if (fillPercentage > secondMaxSetDarkness) {
                secondMaxSetDarkness = fillPercentage
            }
        }

        // 6. Multiple Mark Detection Improve
        val fillThreshold = 0.25
        val marginThreshold = 0.20

        val paperSet = if (maxSetDarkness > fillThreshold) {
            if (secondMaxSetDarkness > maxSetDarkness * 0.75 || (maxSetDarkness - secondMaxSetDarkness) < marginThreshold) {
                "MULTIPLE"
            } else {
                var cx = setStartX
                var cy = setStartY + bestSetRow * setSpacingY
                val snapped = snapToNearest(cx, cy, bubbleCenters, 35.0)
                Imgproc.circle(warpedAnnotated, snapped, bubbleRadius.toInt(), colorGreen, -1)
                setSets[bestSetRow]
            }
        } else {
            "BLANK"
        }

        // 4. Auto Column Detection
        val answerLeft = 280.0
        val answerRight = 900.0
        val numCols = 5
        // (answerRight - answerLeft) / 4.0 gives 5 valid column centers
        val colWidth = if (numCols > 1) (answerRight - answerLeft) / (numCols - 1) else 0.0
        val ansSpacingX = 30.0
        val questionsPerColumn = 20

        val answers = mutableListOf<Int>()
        val allOptionCoords = mutableListOf<List<Pair<Float, Float>>>()

        for (q in 0 until numQuestions) {
            val col = q / questionsPerColumn
            val row = q % questionsPerColumn
            
            // Column center based on request
            val qStartX = answerLeft + col * colWidth - (ansSpacingX * 1.5) // Adjust left to start at 'A'

            // 3. Dynamic Grid Generation (Timing Marks)
            val qStartY = if (leftMarks.isNotEmpty()) {
                topY + row * rowStep
            } else {
                50.0 + row * 47.368 // Fallback
            }

            var maxDarkness = 0.0
            var secondMaxDarkness = 0.0
            var bestOpt = -1

            val currentOptionCoords = mutableListOf<Pair<Float, Float>>()

            for (opt in 0 until numOptions) {
                var cx = qStartX + opt * ansSpacingX
                var cy = qStartY
                
                // Snap to nearest detected bubble
                val snapped = snapToNearest(cx, cy, bubbleCenters, 20.0)
                cx = snapped.x
                cy = snapped.y

                currentOptionCoords.add(Pair(cx.toFloat(), cy.toFloat()))

                val fillPercentage = getFillPercentage(warpedThresh, cx, cy, bubbleRadius)
                Imgproc.circle(warpedAnnotated, Point(cx, cy), bubbleRadius.toInt(), colorBlue, 2)

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

            if (maxDarkness > fillThreshold) {
                if (secondMaxDarkness > maxDarkness * 0.75 || (maxDarkness - secondMaxDarkness) < marginThreshold) {
                    studentAns = -2
                } else {
                    studentAns = bestOpt
                }
            }

            if (studentAns >= 0) {
                var cx = qStartX + studentAns * ansSpacingX
                var cy = qStartY
                val snapped = snapToNearest(cx, cy, bubbleCenters, 20.0)
                Imgproc.circle(warpedAnnotated, snapped, bubbleRadius.toInt(), colorRed, -1)
            } else if (studentAns == -2) {
                var cx = qStartX + bestOpt * ansSpacingX
                var cy = qStartY
                val snapped = snapToNearest(cx, cy, bubbleCenters, 20.0)
                Imgproc.circle(warpedAnnotated, snapped, bubbleRadius.toInt(), colorYellow, 2)
            }

            answers.add(studentAns)
        }

        val finalBitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warpedAnnotated, finalBitmap)

        // 7. QR Student ID
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
        warpedAnnotated.release()
        hierarchy.release()

        return ScanResult(studentId, paperSet, answers, finalBitmap, allOptionCoords)
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
"""
    )
print("Rewrote OmrScanner.kt")
