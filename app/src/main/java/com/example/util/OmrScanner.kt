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
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object OmrScanner {
    private const val TAG = "OmrScanner"

    init {
        try {
            System.loadLibrary("opencv_java4")
        } catch (_: Throwable) {}
    }

    data class ScanResult(
        val studentId: String,
        val paperSet: String,
        val answers: List<Int>,
        val annotatedBitmap: Bitmap,
        val optionCoords: List<List<Pair<Float, Float>>> = emptyList(),
        val isCalibrated: Boolean = true,
        val detectedRollNo: String? = null
    )

    fun scan(
        bitmap: Bitmap,
        numQuestions: Int,
        numOptions: Int,
        templateType: String = "Standard",
        manualRotation: Int = 0,
        sensitivityMultiplier: Float = 1.0f
    ): ScanResult {
        val workingBitmap = if (manualRotation % 360 != 0) {
            val normDeg = ((manualRotation % 360) + 360) % 360
            val matrix = android.graphics.Matrix().apply { postRotate(normDeg.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        val mat = Mat()
        Utils.bitmapToMat(workingBitmap, mat)
        
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        
        // Exact A4 dimensions matching OmrGenerator (1000 x 1414)
        val w = 1000.0
        val h = 1414.0
        
        val warped = Mat()
        val warpedGray = Mat()
        var calibrationSuccess = false

        // Inverted binary threshold of raw image for global marker discovery
        val rawThresh = Mat()
        Imgproc.adaptiveThreshold(gray, rawThresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 31, 15.0)

        // =========================================================================
        // STAGE 1: GLOBAL 4-CORNER REGISTRATION MARKER DETECTION (High Precision)
        // If the 4 black 40x40 squares are found directly in the camera image,
        // warp directly to canonical positions: (50,50), (950,50), (950,1364), (50,1364)
        // =========================================================================
        val globalMarkers = findRegistrationMarkersGlobal(rawThresh)
        if (globalMarkers != null && globalMarkers.size == 4) {
            val srcMat = MatOfPoint2f(*globalMarkers.toTypedArray())
            val dstMat = MatOfPoint2f(
                Point(50.0, 50.0), Point(950.0, 50.0), Point(950.0, 1364.0), Point(50.0, 1364.0)
            )
            val pTransform = Imgproc.getPerspectiveTransform(srcMat, dstMat)
            Imgproc.warpPerspective(mat, warped, pTransform, Size(w, h))
            Imgproc.warpPerspective(gray, warpedGray, pTransform, Size(w, h))
            srcMat.release()
            dstMat.release()
            pTransform.release()
            calibrationSuccess = true
            Log.d(TAG, "Stage 1: Direct 4-corner marker registration successful")
        } else {
            // =========================================================================
            // STAGE 2: ADAPTIVE PAPER SHEET CONTOUR DETECTION & PERSPECTIVE WARP
            // Detect paper quadrilateral even if photo is tilted or desk background is visible
            // =========================================================================
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
                Log.d(TAG, "Stage 2: Document boundary de-skew successful")
            } else {
                // Fallback: Smart aspect-ratio preserved crop to 1:1.414
                val origW = mat.width().toDouble()
                val origH = mat.height().toDouble()
                val targetAspect = h / w // 1.414
                val currentAspect = origH / origW

                if (Math.abs(currentAspect - targetAspect) > 0.1) {
                    val cropRect = if (currentAspect > targetAspect) {
                        // Image is taller than A4, crop top & bottom
                        val newH = (origW * targetAspect).toInt()
                        val topY = ((origH - newH) / 2.0).toInt().coerceAtLeast(0)
                        Rect(0, topY, origW.toInt(), newH.coerceAtMost(origH.toInt() - topY))
                    } else {
                        // Image is wider than A4, crop left & right
                        val newW = (origH / targetAspect).toInt()
                        val leftX = ((origW - newW) / 2.0).toInt().coerceAtLeast(0)
                        Rect(leftX, 0, newW.coerceAtMost(origW.toInt() - leftX), origH.toInt())
                    }
                    val croppedMat = mat.submat(cropRect)
                    val croppedGray = gray.submat(cropRect)
                    Imgproc.resize(croppedMat, warped, Size(w, h))
                    Imgproc.resize(croppedGray, warpedGray, Size(w, h))
                    croppedMat.release()
                    croppedGray.release()
                } else {
                    Imgproc.resize(mat, warped, Size(w, h))
                    Imgproc.resize(gray, warpedGray, Size(w, h))
                }
                Log.d(TAG, "Stage 2 fallback: Centered aspect-ratio resize used")
            }
        }
        rawThresh.release()

        var warpedBlurred = Mat()
        Imgproc.GaussianBlur(warpedGray, warpedBlurred, Size(5.0, 5.0), 0.0)
        
        var warpedThresh = Mat()
        Imgproc.adaptiveThreshold(warpedBlurred, warpedThresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 31, 15.0)

        // =========================================================================
        // STAGE 3: ROBUST MULTI-SIGNAL UPRIGHT ORIENTATION VERIFICATION
        // An upright OMR has:
        // 1. Header and Roll No in upper region (y: 60..500)
        // 2. Heavy solid black band at y: 510..550
        // 3. Question blocks in lower region (y: 580..1360) -> high ink in lower half
        // 4. Set options column on LEFT, Question columns on RIGHT
        // We only invert if multiple signals explicitly prove the sheet is upside-down.
        // =========================================================================
        var isUpsideDown = false
        try {
            // Signal 1: Question bubbles vertical distribution
            // In an upright sheet, lower region has 400 questions -> MUCH more ink than upper region
            val upperAreaRoi = warpedThresh.submat(Rect(270, 80, 650, 400))
            val lowerAreaRoi = warpedThresh.submat(Rect(270, 900, 650, 400))
            val upperAreaInk = Core.countNonZero(upperAreaRoi).toDouble() / (upperAreaRoi.width() * upperAreaRoi.height())
            val lowerAreaInk = Core.countNonZero(lowerAreaRoi).toDouble() / (lowerAreaRoi.width() * lowerAreaRoi.height())
            upperAreaRoi.release()
            lowerAreaRoi.release()

            // Signal 2: The solid black header bar position (normally y=510..550, inverted y=864..904)
            val barUpperRoi = warpedThresh.submat(Rect(150, 500, 700, 55))
            val barLowerRoi = warpedThresh.submat(Rect(150, 859, 700, 55))
            val barUpperInk = Core.countNonZero(barUpperRoi).toDouble() / (barUpperRoi.width() * barUpperRoi.height())
            val barLowerInk = Core.countNonZero(barLowerRoi).toDouble() / (barLowerRoi.width() * barLowerRoi.height())
            barUpperRoi.release()
            barLowerRoi.release()

            // Signal 3: Horizontal asymmetry (Questions on right, 1 Set column on left)
            val midLeftRoi = warpedThresh.submat(Rect(120, 650, 150, 500))
            val midRightRoi = warpedThresh.submat(Rect(350, 650, 550, 500))
            val midLeftInk = Core.countNonZero(midLeftRoi).toDouble() / (midLeftRoi.width() * midLeftRoi.height())
            val midRightInk = Core.countNonZero(midRightRoi).toDouble() / (midRightRoi.width() * midRightRoi.height())
            midLeftRoi.release()
            midRightRoi.release()

            var upsideDownVotes = 0
            if (upperAreaInk > lowerAreaInk * 1.55) upsideDownVotes += 2
            if (barLowerInk > barUpperInk * 1.6) upsideDownVotes += 2
            if (midLeftInk > midRightInk * 1.35) upsideDownVotes += 1

            if (upsideDownVotes >= 3) {
                isUpsideDown = true
                Log.w(TAG, "Stage 3: Multiple signals detected inverted sheet ($upsideDownVotes votes). Rotating 180° to upright.")
            } else {
                Log.d(TAG, "Stage 3: Confirmed sheet is upright (upside-down votes: $upsideDownVotes). Keeping current orientation.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in orientation check", e)
        }

        if (isUpsideDown) {
            Core.rotate(warped, warped, Core.ROTATE_180)
            Core.rotate(warpedGray, warpedGray, Core.ROTATE_180)
            Core.rotate(warpedThresh, warpedThresh, Core.ROTATE_180)
        }

        // =========================================================================
        // STAGE 4: FINE SUB-PIXEL CALIBRATION (Registration Markers in 1000x1414)
        // Expected centers: (50,50), (950,50), (950,1364), (50,1364)
        // =========================================================================
        var finalWarped = warped
        var finalThresh = warpedThresh
        val regCorners = findRegistrationMarkers(warpedThresh)
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
            calibrationSuccess = true
            
            srcMat.release()
            dstMat.release()
            fineTransform.release()
            Log.d(TAG, "Stage 4: Sub-pixel fine registration markers locked")
        }

        val warpedAnnotated = finalWarped.clone()
        val colorRed = Scalar(239.0, 68.0, 68.0, 255.0)
        val colorGreen = Scalar(34.0, 197.0, 94.0, 255.0)
        val colorBlue = Scalar(59.0, 130.0, 246.0, 255.0)
        val colorYellow = Scalar(234.0, 179.0, 8.0, 255.0)

        // Draw green registration indicator boxes around the 4 corners to visually confirm calibration
        val markerIndicatorColor = if (calibrationSuccess) colorGreen else colorYellow
        Imgproc.rectangle(warpedAnnotated, Rect(25, 25, 50, 50), markerIndicatorColor, 2)
        Imgproc.rectangle(warpedAnnotated, Rect(925, 25, 50, 50), markerIndicatorColor, 2)
        Imgproc.rectangle(warpedAnnotated, Rect(925, 1339, 50, 50), markerIndicatorColor, 2)
        Imgproc.rectangle(warpedAnnotated, Rect(25, 1339, 50, 50), markerIndicatorColor, 2)

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

            val fillPercentage = getFillPercentage(finalThresh, rawCx, rawCy, setBubbleRadius)
            Imgproc.circle(warpedAnnotated, Point(rawCx, rawCy), setBubbleRadius.toInt(), colorBlue, 2)

            if (fillPercentage > maxSetDarkness) {
                secondMaxSetDarkness = maxSetDarkness
                maxSetDarkness = fillPercentage
                bestSetRow = i
            } else if (fillPercentage > secondMaxSetDarkness) {
                secondMaxSetDarkness = fillPercentage
            }
        }

        val fillThreshold = (0.22 * sensitivityMultiplier).coerceIn(0.10, 0.45)
        val marginThreshold = (0.10 * sensitivityMultiplier).coerceIn(0.05, 0.25)

        val paperSet = if (maxSetDarkness > fillThreshold && bestSetRow >= 0) {
            if (secondMaxSetDarkness > maxSetDarkness * 0.75 && (maxSetDarkness - secondMaxSetDarkness) < marginThreshold) {
                "MULTIPLE"
            } else {
                val rawCx = setStartX
                val rawCy = setStartY + bestSetRow * setSpacingY
                Imgproc.circle(warpedAnnotated, Point(rawCx, rawCy), setBubbleRadius.toInt(), colorGreen, -1)
                setSets[bestSetRow]
            }
        } else {
            "BLANK"
        }

        // ==========================================
        // ROLL NUMBER MATRIX EXTRACTION (7 Columns, Digits 0-9)
        // Matching OmrGenerator layout:
        // rStartX = 150.0, rStartY = 120.0, rSpacingX = 42.0, rSpacingY = 28.0
        // ==========================================
        val rStartX = 150.0
        val rStartY = 120.0
        val rSpacingX = 42.0
        val rSpacingY = 28.0
        val rBubbleRadius = 11.0
        val rollDigits = StringBuilder()

        for (col in 0 until 7) {
            val cx = rStartX + col * rSpacingX
            var bestDigit = -1
            var maxDigitDarkness = 0.0
            var secondMaxDigitDarkness = 0.0

            for (row in 0..9) {
                val cy = rStartY + 28.0 + row * rSpacingY
                val fill = getFillPercentage(finalThresh, cx, cy, rBubbleRadius)
                Imgproc.circle(warpedAnnotated, Point(cx, cy), rBubbleRadius.toInt(), colorBlue, 1)

                if (fill > maxDigitDarkness) {
                    secondMaxDigitDarkness = maxDigitDarkness
                    maxDigitDarkness = fill
                    bestDigit = row
                } else if (fill > secondMaxDigitDarkness) {
                    secondMaxDigitDarkness = fill
                }
            }

            if (maxDigitDarkness > fillThreshold && bestDigit >= 0) {
                val cy = rStartY + 28.0 + bestDigit * rSpacingY
                Imgproc.circle(warpedAnnotated, Point(cx, cy), rBubbleRadius.toInt(), colorGreen, -1)
                rollDigits.append(bestDigit)
            }
        }
        val detectedRollNo = if (rollDigits.length >= 4) rollDigits.toString() else null

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
            val qCenterY = headerBottom + row * qRowHeight + (qRowHeight / 2.0)

            var maxDarkness = 0.0
            var secondMaxDarkness = 0.0
            var bestOpt = -1

            val currentOptionCoords = mutableListOf<Pair<Float, Float>>()

            for (opt in 0 until numOptions) {
                val rawCx = bubblesStartX + opt * optSpacing
                val rawCy = qCenterY

                currentOptionCoords.add(Pair(rawCx.toFloat(), rawCy.toFloat()))

                val fillPercentage = getFillPercentage(finalThresh, rawCx, rawCy, qBubbleRadius)
                Imgproc.circle(warpedAnnotated, Point(rawCx, rawCy), qBubbleRadius.toInt(), colorBlue, 2)

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
                Imgproc.circle(warpedAnnotated, Point(rawCx, rawCy), qBubbleRadius.toInt(), colorGreen, -1)
            } else if (studentAns == -2) {
                val rawCx = bubblesStartX + bestOpt * optSpacing
                val rawCy = qCenterY
                Imgproc.circle(warpedAnnotated, Point(rawCx, rawCy), qBubbleRadius.toInt(), colorYellow, -1)
            }

            answers.add(studentAns)
        }

        val finalBitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warpedAnnotated, finalBitmap)

        // Read QR code for candidate identity
        var studentId = detectedRollNo ?: "UNKNOWN"
        try {
            val qr = readQr(bitmap) ?: readQr(finalBitmap)
            if (qr != null && qr.isNotBlank()) {
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

        return ScanResult(studentId, paperSet, answers, finalBitmap, allOptionCoords, calibrationSuccess, detectedRollNo)
    }

    /**
     * Orders 4 arbitrary polygon points to: [Top-Left, Top-Right, Bottom-Right, Bottom-Left].
     * Resilient against tilt, perspective distortion, and corrects landscape orientation.
     */
    private fun orderPoints(pts: List<Point>): List<Point> {
        require(pts.size == 4)

        // Top-Left has minimum (x + y)
        val tl = pts.minByOrNull { it.x + it.y }!!
        // Bottom-Right has maximum (x + y)
        val br = pts.maxByOrNull { it.x + it.y }!!

        // Top-Right and Bottom-Left from remaining points
        val remaining = pts.filter { it != tl && it != br }
        val tr: Point
        val bl: Point
        if (remaining.size == 2) {
            if ((remaining[0].y - remaining[0].x) < (remaining[1].y - remaining[1].x)) {
                tr = remaining[0]
                bl = remaining[1]
            } else {
                tr = remaining[1]
                bl = remaining[0]
            }
        } else {
            tr = pts.maxByOrNull { it.x - it.y } ?: pts[1]
            bl = pts.minByOrNull { it.x - it.y } ?: pts[3]
        }

        // Orientation check: If width > height * 1.05, photo is rotated landscape.
        // Rotate corners so sheet is upright portrait.
        val wTop = Math.hypot(tr.x - tl.x, tr.y - tl.y)
        val wBottom = Math.hypot(br.x - bl.x, br.y - bl.y)
        val maxWidth = Math.max(wTop, wBottom)

        val hLeft = Math.hypot(bl.x - tl.x, bl.y - tl.y)
        val hRight = Math.hypot(br.x - tr.x, br.y - tr.y)
        val maxHeight = Math.max(hLeft, hRight)

        if (maxWidth > maxHeight * 1.05) {
            return listOf(tr, br, bl, tl)
        }

        return listOf(tl, tr, br, bl)
    }

    /**
     * Discovers the 4 solid black 40x40 calibration markers across the raw unwarped image.
     */
    private fun findRegistrationMarkersGlobal(thresh: Mat): List<Point>? {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh.clone(), contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        val imgArea = thresh.width().toDouble() * thresh.height()
        val candidates = mutableListOf<Point>()

        for (c in contours) {
            val area = Imgproc.contourArea(c)
            // Solid square marker should be roughly 0.02% to 4% of total image area
            if (area in (imgArea * 0.0002)..(imgArea * 0.04)) {
                val rect = Imgproc.boundingRect(c)
                val ratio = rect.width.toDouble() / rect.height
                if (ratio in 0.65..1.55) {
                    val solidity = area / (rect.width * rect.height)
                    if (solidity >= 0.60) {
                        candidates.add(Point(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0))
                    }
                }
            }
        }
        hierarchy.release()

        if (candidates.size < 4) return null

        // Find 4 extreme candidate points
        val tl = candidates.minByOrNull { it.x + it.y } ?: return null
        val br = candidates.maxByOrNull { it.x + it.y } ?: return null
        val tr = candidates.maxByOrNull { it.x - it.y } ?: return null
        val bl = candidates.minByOrNull { it.x - it.y } ?: return null

        val four = listOf(tl, tr, br, bl).distinct()
        if (four.size != 4) return null

        val matOfPt = MatOfPoint(*four.toTypedArray())
        val isConvex = Imgproc.isContourConvex(matOfPt)
        matOfPt.release()
        if (!isConvex) return null

        val ordered = orderPoints(four)
        val wTop = Math.hypot(ordered[1].x - ordered[0].x, ordered[1].y - ordered[0].y)
        val hLeft = Math.hypot(ordered[3].x - ordered[0].x, ordered[3].y - ordered[0].y)
        val ratio = hLeft / wTop.coerceAtLeast(1.0)

        // The 4 markers have ratio (1314 / 900) = 1.46
        if (ratio in 1.15..1.85) {
            return ordered
        }
        return null
    }

    /**
     * Finds the 4 corners of the paper sheet in the raw image with multi-pass adaptive edge detection.
     */
    private fun findDocumentCorners(gray: Mat): List<Point>? {
        val origW = gray.width().toDouble()
        val origH = gray.height().toDouble()
        val targetDim = 800.0
        val scale = targetDim / Math.max(origW, origH)

        val scaled = Mat()
        Imgproc.resize(gray, scaled, Size(origW * scale, origH * scale))

        val blurred = Mat()
        Imgproc.GaussianBlur(scaled, blurred, Size(5.0, 5.0), 0.0)

        val scaledArea = scaled.width() * scaled.height()
        val minArea = scaledArea * 0.08 // accept paper if >= 8% of frame

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(7.0, 7.0))

        data class Candidate(val points: List<Point>, val score: Double)
        val candidateList = mutableListOf<Candidate>()

        fun evaluateContour(contour: MatOfPoint) {
            val area = Imgproc.contourArea(contour)
            if (area < minArea) return

            val contour2f = MatOfPoint2f(*contour.toArray())
            val peri = Imgproc.arcLength(contour2f, true)

            for (eps in listOf(0.015, 0.02, 0.03, 0.04, 0.05)) {
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(contour2f, approx, eps * peri, true)
                if (approx.total() == 4L) {
                    val pts = approx.toList()
                    val m = MatOfPoint(*pts.toTypedArray())
                    if (Imgproc.isContourConvex(m)) {
                        val ordered = orderPoints(pts)
                        val wT = Math.hypot(ordered[1].x - ordered[0].x, ordered[1].y - ordered[0].y)
                        val hL = Math.hypot(ordered[3].x - ordered[0].x, ordered[3].y - ordered[0].y)
                        val aspect = Math.max(wT, hL) / Math.min(wT, hL).coerceAtLeast(1.0)
                        val aspectDiff = Math.abs(aspect - 1.414)
                        val score = area * (1.0 - (aspectDiff / 1.414).coerceAtMost(0.8))
                        candidateList.add(Candidate(ordered, score))
                    }
                    m.release()
                }
                approx.release()
            }

            // Also test convex hull
            val hull = MatOfInt()
            Imgproc.convexHull(contour, hull)
            val hullIndices = hull.toList()
            if (hullIndices.size >= 4) {
                val allPts = contour.toList()
                val hullPoints = hullIndices.map { allPts[it] }
                val hullMat2f = MatOfPoint2f(*hullPoints.toTypedArray())
                val hullPeri = Imgproc.arcLength(hullMat2f, true)
                for (eps in listOf(0.02, 0.03, 0.04)) {
                    val approx = MatOfPoint2f()
                    Imgproc.approxPolyDP(hullMat2f, approx, eps * hullPeri, true)
                    if (approx.total() == 4L) {
                        val pts = approx.toList()
                        val m = MatOfPoint(*pts.toTypedArray())
                        if (Imgproc.isContourConvex(m)) {
                            val ordered = orderPoints(pts)
                            val wT = Math.hypot(ordered[1].x - ordered[0].x, ordered[1].y - ordered[0].y)
                            val hL = Math.hypot(ordered[3].x - ordered[0].x, ordered[3].y - ordered[0].y)
                            val aspect = Math.max(wT, hL) / Math.min(wT, hL).coerceAtLeast(1.0)
                            val aspectDiff = Math.abs(aspect - 1.414)
                            val score = area * (1.0 - (aspectDiff / 1.414).coerceAtMost(0.8)) * 0.95
                            candidateList.add(Candidate(ordered, score))
                        }
                        m.release()
                    }
                    approx.release()
                }
                hullMat2f.release()
            }
            hull.release()

            // Fallback: minAreaRect
            val rotRect = Imgproc.minAreaRect(contour2f)
            val boxPts = arrayOfNulls<Point>(4)
            rotRect.points(boxPts)
            val boxList = boxPts.filterNotNull()
            if (boxList.size == 4) {
                val ordered = orderPoints(boxList)
                val wT = Math.hypot(ordered[1].x - ordered[0].x, ordered[1].y - ordered[0].y)
                val hL = Math.hypot(ordered[3].x - ordered[0].x, ordered[3].y - ordered[0].y)
                val aspect = Math.max(wT, hL) / Math.min(wT, hL).coerceAtLeast(1.0)
                val aspectDiff = Math.abs(aspect - 1.414)
                val score = area * (1.0 - (aspectDiff / 1.414).coerceAtMost(0.8)) * 0.85
                candidateList.add(Candidate(ordered, score))
            }
            contour2f.release()
        }

        // Pass 1: Adaptive Threshold + Morphological Close
        val threshPass = Mat()
        Imgproc.adaptiveThreshold(blurred, threshPass, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 31, 10.0)
        Imgproc.morphologyEx(threshPass, threshPass, Imgproc.MORPH_CLOSE, kernel)
        val contours1 = ArrayList<MatOfPoint>()
        val hier1 = Mat()
        Imgproc.findContours(threshPass, contours1, hier1, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        contours1.forEach { evaluateContour(it) }
        threshPass.release()
        hier1.release()

        // Pass 2: Canny Edge + Morphological Close
        val edged = Mat()
        val meanVal = Core.mean(scaled).`val`[0]
        val lower = Math.max(20.0, 0.66 * meanVal)
        val upper = Math.min(220.0, 1.33 * meanVal)
        Imgproc.Canny(blurred, edged, lower, upper)
        Imgproc.morphologyEx(edged, edged, Imgproc.MORPH_CLOSE, kernel)
        val contours2 = ArrayList<MatOfPoint>()
        val hier2 = Mat()
        Imgproc.findContours(edged, contours2, hier2, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        contours2.forEach { evaluateContour(it) }
        edged.release()
        hier2.release()

        scaled.release()
        blurred.release()
        kernel.release()

        val best = candidateList.maxByOrNull { it.score } ?: return null
        return best.points.map { Point(it.x / scale, it.y / scale) }
    }

    /**
     * Sub-pixel fine registration markers search inside the 1000x1414 de-skewed sheet.
     */
    private fun findRegistrationMarkers(thresh: Mat): List<Point>? {
        val width = thresh.width()
        val height = thresh.height()
        
        // Define search regions around expected positions of 40x40 black calibration boxes
        // Expected centers: TL(50,50), TR(950,50), BR(950,1364), BL(50,1364)
        val rois = listOf(
            Rect(10, 10, 180, 180),                         // Top-Left
            Rect(width - 190, 10, 180, 180),                // Top-Right
            Rect(width - 190, height - 190, 180, 180),      // Bottom-Right
            Rect(10, height - 190, 180, 180)                // Bottom-Left
        )

        val expectedCenters = listOf(
            Point(50.0, 50.0),
            Point(950.0, 50.0),
            Point(950.0, 1364.0),
            Point(50.0, 1364.0)
        )
        
        val centers = mutableListOf<Point>()
        
        for (i in rois.indices) {
            val roiRect = rois[i]
            val expected = expectedCenters[i]
            if (roiRect.x < 0 || roiRect.y < 0 || roiRect.x + roiRect.width > width || roiRect.y + roiRect.height > height) {
                return null
            }
            val subRoi = thresh.submat(roiRect)
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(subRoi, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            
            var bestCenter: Point? = null
            var bestScore = Double.MAX_VALUE
            val targetArea = 1600.0 // 40x40 square
            
            for (c in contours) {
                val area = Imgproc.contourArea(c)
                if (area in 300.0..5000.0) {
                    val rect = Imgproc.boundingRect(c)
                    val ratio = rect.width.toDouble() / rect.height
                    if (ratio in 0.6..1.6) {
                        val pt = Point(
                            roiRect.x + rect.x + rect.width / 2.0,
                            roiRect.y + rect.y + rect.height / 2.0
                        )
                        val distToExpected = Math.hypot(pt.x - expected.x, pt.y - expected.y)
                        val areaDiff = Math.abs(area - targetArea)
                        val score = distToExpected * 2.0 + areaDiff * 0.05
                        if (score < bestScore) {
                            bestScore = score
                            bestCenter = pt
                        }
                    }
                }
            }
            
            subRoi.release()
            hierarchy.release()
            
            if (bestCenter != null) {
                centers.add(bestCenter)
            } else {
                return null
            }
        }
        
        return if (centers.size == 4) centers else null
    }

    /**
     * Samples the bubble fill percentage in a clean circular interior with local peak search.
     */
    private fun getFillPercentage(threshInv: Mat, cx: Double, cy: Double, radius: Double): Double {
        var maxFill = 0.0
        val r = (radius * 0.70).toInt().coerceAtLeast(3) // 70% of radius isolates filled ink from outer border

        for (dx in -2..2 step 2) {
            for (dy in -2..2 step 2) {
                val x = (cx + dx - r).toInt()
                val y = (cy + dy - r).toInt()
                val w = r * 2
                val h = r * 2

                if (x < 0 || y < 0 || x + w > threshInv.width() || y + h > threshInv.height()) continue

                val roi = threshInv.submat(Rect(x, y, w, h))
                val mask = Mat.zeros(roi.size(), CvType.CV_8U)
                Imgproc.circle(mask, Point(roi.width() / 2.0, roi.height() / 2.0), r, Scalar(255.0), -1)

                val maskedRoi = Mat()
                Core.bitwise_and(roi, mask, maskedRoi)
                val filled = Core.countNonZero(maskedRoi)
                val total = Core.countNonZero(mask)

                roi.release()
                mask.release()
                maskedRoi.release()

                val fill = if (total > 0) filled.toDouble() / total else 0.0
                if (fill > maxFill) maxFill = fill
            }
        }
        return maxFill
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
}
