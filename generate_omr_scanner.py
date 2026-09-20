import re

code = """
package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
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
    data class ScanResult(
        val studentId: String,
        val paperSet: String,
        val answers: List<Int>,
        val annotatedBitmap: Bitmap,
        val optionCoords: List<List<Pair<Float, Float>>> = emptyList()
    )

    fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int, templateType: String = "Standard"): ScanResult {
        // 1. Convert to Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // 2. Preprocess
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        
        // 3. Find Corner Markers
        val corners = findCornersOpenCV(gray)
        
        val w = 1000.0
        val h = 1000.0
        
        val srcPoints = if (corners != null) {
            corners
        } else {
            // Absolute fallback
            listOf(
                Point(0.0, 0.0),
                Point(bitmap.width.toDouble(), 0.0),
                Point(0.0, bitmap.height.toDouble()),
                Point(bitmap.width.toDouble(), bitmap.height.toDouble())
            )
        }

        // 4. Perspective Transform
        val srcMat = MatOfPoint2f(*srcPoints.toTypedArray())
        val dstMat = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(w, 0.0),
            Point(0.0, h),
            Point(w, h)
        )
        
        val perspectiveTransform = Imgproc.getPerspectiveTransform(srcMat, dstMat)
        
        val warped = Mat()
        Imgproc.warpPerspective(mat, warped, perspectiveTransform, Size(w, h))
        
        val warpedGray = Mat()
        Imgproc.warpPerspective(gray, warpedGray, perspectiveTransform, Size(w, h))
        
        // Apply Thresholding on warped image for bubble detection
        val warpedThresh = Mat()
        // Adaptive thresholding to handle uneven lighting
        Imgproc.adaptiveThreshold(warpedGray, warpedThresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 31, 15.0)

        // Draw debug overlay on warped image
        val warpedAnnotated = warped.clone()
        val colorRed = Scalar(255.0, 0.0, 0.0, 255.0)
        val colorGreen = Scalar(0.0, 255.0, 0.0, 255.0)
        val colorBlue = Scalar(0.0, 0.0, 255.0, 255.0)
        
        // 5. Bubble Mapping & Fill Detection
        
        // Read Set
        val setStartX = 120.0
        val setStartY = 50.0
        val setSpacingY = 94.736
        val setSets = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        
        var bestSetRow = -1
        var maxSetDarkness = 0.0
        var secondMaxSetDarkness = 0.0
        
        val bubbleRadius = 14.0
        
        for (i in setSets.indices) {
            val cx = setStartX
            val cy = setStartY + i * setSpacingY
            
            val fillPercentage = getFillPercentage(warpedThresh, cx, cy, bubbleRadius)
            
            // Draw ROI for debug
            Imgproc.circle(warpedAnnotated, Point(cx, cy), bubbleRadius.toInt(), colorBlue, 2)
            
            if (fillPercentage > maxSetDarkness) {
                secondMaxSetDarkness = maxSetDarkness
                maxSetDarkness = fillPercentage
                bestSetRow = i
            } else if (fillPercentage > secondMaxSetDarkness) {
                secondMaxSetDarkness = fillPercentage
            }
        }
        
        // Configurable threshold for considering a bubble "filled"
        val fillThreshold = 0.35 // 35% of pixels in the ROI are dark
        val marginThreshold = 0.15 // Difference between top and second top
        
        val paperSet = if (maxSetDarkness > fillThreshold && (maxSetDarkness - secondMaxSetDarkness) > marginThreshold && bestSetRow != -1) {
            val cx = setStartX
            val cy = setStartY + bestSetRow * setSpacingY
            Imgproc.circle(warpedAnnotated, Point(cx, cy), bubbleRadius.toInt(), colorGreen, -1)
            setSets[bestSetRow] 
        } else {
            "?" // MULTIPLE_MARKED or NOT_ATTEMPTED
        }
        
        // Student ID (Assuming it's filled in bubbles or we can just return a placeholder for now since we focused on SET and Answers)
        // If templateType == "Standard", we can try QR/Barcode on the warped image, but ZXing needs Bitmap.
        // For now, let's keep it simple.
        var studentId = "?"
        
        // Read Answers
        val ansBaseX = 230.0
        val ansBaseY = 50.0
        val ansColStrideX = 160.0
        val ansSpacingX = 30.0
        val ansSpacingY = 47.368
        val ansBubbleRadius = 11.0
        
        val questionsPerColumn = 20
        val answers = mutableListOf<Int>()
        val allOptionCoords = mutableListOf<List<Pair<Float, Float>>>()
        
        for (q in 0 until numQuestions) {
            val col = q / questionsPerColumn
            val row = q % questionsPerColumn
            
            val qStartX = ansBaseX + col * ansColStrideX
            val qStartY = ansBaseY + row * ansSpacingY
            
            var maxDarkness = 0.0
            var secondMaxDarkness = 0.0
            var bestOpt = -1
            
            val currentOptionCoords = mutableListOf<Pair<Float, Float>>()
            
            for (opt in 0 until numOptions) {
                val cx = qStartX + opt * ansSpacingX
                val cy = qStartY
                
                // Store mapped coordinates for UI overlay
                currentOptionCoords.add(Pair(cx.toFloat(), cy.toFloat()))
                
                val fillPercentage = getFillPercentage(warpedThresh, cx, cy, ansBubbleRadius)
                
                Imgproc.circle(warpedAnnotated, Point(cx, cy), ansBubbleRadius.toInt(), colorBlue, 2)
                
                if (fillPercentage > maxDarkness) {
                    secondMaxDarkness = maxDarkness
                    maxDarkness = fillPercentage
                    bestOpt = opt
                } else if (fillPercentage > secondMaxDarkness) {
                    secondMaxDarkness = fillPercentage
                }
            }
            
            allOptionCoords.add(currentOptionCoords)
            
            val studentAns = if (maxDarkness > fillThreshold && (maxDarkness - secondMaxDarkness) > marginThreshold && bestOpt != -1) bestOpt else -1
            
            if (studentAns != -1) {
                val cx = qStartX + studentAns * ansSpacingX
                val cy = qStartY
                Imgproc.circle(warpedAnnotated, Point(cx, cy), ansBubbleRadius.toInt(), colorRed, -1)
            }
            
            answers.add(studentAns)
        }
        
        // Convert back to Bitmap
        val finalBitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warpedAnnotated, finalBitmap)
        
        return ScanResult(studentId, paperSet, answers, finalBitmap, allOptionCoords)
    }

    private fun getFillPercentage(threshInv: Mat, cx: Double, cy: Double, radius: Double): Double {
        val r = (radius * 0.8).toInt() // Slightly smaller ROI to avoid bubble border
        
        val x = (cx - r).toInt().coerceAtLeast(0)
        val y = (cy - r).toInt().coerceAtLeast(0)
        val w = (r * 2).coerceAtMost(threshInv.width() - x)
        val h = (r * 2).coerceAtMost(threshInv.height() - y)
        
        if (w <= 0 || h <= 0) return 0.0
        
        val roi = threshInv.submat(Rect(x, y, w, h))
        val nonZero = Core.countNonZero(roi)
        val totalPixels = w * h
        
        return nonZero.toDouble() / totalPixels
    }

    private fun findCornersOpenCV(gray: Mat): List<Point>? {
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
        
        val thresh = Mat()
        Imgproc.adaptiveThreshold(blurred, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 25, 10.0)
        
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
        
        val squareCenters = mutableListOf<Point>()
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > 300 && area < gray.width() * gray.height() / 10) { 
                val contour2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(contour2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(contour2f, approx, 0.04 * peri, true)
                
                if (approx.total() == 4L) {
                    val rect = Imgproc.boundingRect(contour)
                    val aspectRatio = rect.width.toDouble() / rect.height
                    
                    if (aspectRatio > 0.7 && aspectRatio < 1.3) {
                        val mask = Mat.zeros(gray.size(), CvType.CV_8U)
                        Imgproc.drawContours(mask, listOf(contour), -1, Scalar(255.0), -1)
                        val mean = Core.mean(gray, mask).`val`[0]
                        if (mean < 120) { 
                            squareCenters.add(Point(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0))
                        }
                    }
                }
            }
        }
        
        if (squareCenters.size >= 4) {
            val w = gray.width().toDouble()
            val h = gray.height().toDouble()
            
            var tl = Point(w, h)
            var tr = Point(0.0, h)
            var bl = Point(w, 0.0)
            var br = Point(0.0, 0.0)
            
            for (pt in squareCenters) {
                if (pt.x < w/2 && pt.y < h/2 && (pt.x + pt.y < tl.x + tl.y)) tl = pt
                if (pt.x > w/2 && pt.y < h/2 && ((w - pt.x) + pt.y < (w - tr.x) + tr.y)) tr = pt
                if (pt.x < w/2 && pt.y > h/2 && (pt.x + (h - pt.y) < bl.x + (h - bl.y))) bl = pt
                if (pt.x > w/2 && pt.y > h/2 && ((w - pt.x) + (h - pt.y) < (w - br.x) + (h - br.y))) br = pt
            }
            
            if (tl.x < w && tr.x > 0 && bl.x < w && br.x > 0) {
                return listOf(tl, tr, bl, br)
            }
        }
        
        // Fallback: Use OpenCV to find the largest document contour
        var maxArea = 0.0
        var bestApprox = MatOfPoint2f()
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > 10000) { 
                val contour2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(contour2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)
                
                if (approx.total() == 4L && area > maxArea) {
                    maxArea = area
                    approx.copyTo(bestApprox)
                }
            }
        }
        
        if (bestApprox.total() == 4L) {
             val points = bestApprox.toList()
             val sortedByY = points.sortedBy { it.y }
             val top = sortedByY.take(2).sortedBy { it.x }
             val bottom = sortedByY.drop(2).sortedBy { it.x }
             return listOf(top[0], top[1], bottom[0], bottom[1])
        }
        
        return null
    }
}
"""
with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(code)

