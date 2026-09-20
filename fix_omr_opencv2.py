import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

better_find_corners = """
    private fun findCornersOpenCV(bitmap: Bitmap): List<Pair<Float, Float>> {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        
        // Thresholding
        val thresh = Mat()
        Imgproc.adaptiveThreshold(gray, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 25, 10.0)
        
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
        
        val squareCenters = mutableListOf<Point>()
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            // The marker is 40x40 on A4, so around 1600. Allow a range depending on image size.
            if (area > 300 && area < bitmap.width * bitmap.height / 10) { 
                val contour2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(contour2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(contour2f, approx, 0.04 * peri, true)
                
                if (approx.total() == 4L) {
                    val rect = Imgproc.boundingRect(contour)
                    val aspectRatio = rect.width.toDouble() / rect.height
                    
                    if (aspectRatio > 0.8 && aspectRatio < 1.2) {
                        // Found a square-ish contour. Check if it's solid (filled).
                        val mask = Mat.zeros(gray.size(), CvType.CV_8U)
                        Imgproc.drawContours(mask, listOf(contour), -1, Scalar(255.0), -1)
                        val mean = org.opencv.core.Core.mean(gray, mask).`val`[0]
                        if (mean < 100) { // It's dark
                            squareCenters.add(Point(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0))
                        }
                    }
                }
            }
        }
        
        // If we found exactly 4 (or more), we sort them into quadrants
        if (squareCenters.size >= 4) {
            // Sort to find the best 4. Just take corners.
            val w = bitmap.width.toDouble()
            val h = bitmap.height.toDouble()
            
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
                return listOf(
                    Pair(tl.x.toFloat(), tl.y.toFloat()),
                    Pair(tr.x.toFloat(), tr.y.toFloat()),
                    Pair(bl.x.toFloat(), bl.y.toFloat()),
                    Pair(br.x.toFloat(), br.y.toFloat())
                )
            }
        }
        
        // Fallback: Use OpenCV to find the largest document contour
        maxArea = 0.0
        bestApprox = MatOfPoint2f()
        
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
             return listOf(
                Pair(top[0].x.toFloat(), top[0].y.toFloat()),
                Pair(top[1].x.toFloat(), top[1].y.toFloat()),
                Pair(bottom[0].x.toFloat(), bottom[0].y.toFloat()),
                Pair(bottom[1].x.toFloat(), bottom[1].y.toFloat())
             )
        }
        
        // Absolute fallback: corners of the image
        return listOf(
            Pair(0f, 0f),
            Pair(bitmap.width.toFloat(), 0f),
            Pair(0f, bitmap.height.toFloat()),
            Pair(bitmap.width.toFloat(), bitmap.height.toFloat())
        )
    }
"""

# Now replace the old findCornersOpenCV with the better one
start_str = "    private fun findCornersOpenCV(bitmap: Bitmap): List<Pair<Float, Float>> {"
end_str = "    private fun findCorner(bitmap: Bitmap, isLeft: Boolean, isTop: Boolean): Pair<Float, Float> {"

start_idx = text.find(start_str)
end_idx = text.find(end_str)

if start_idx != -1 and end_idx != -1:
    # Need to add maxArea/bestApprox to the new snippet
    better = better_find_corners.strip()
    better = better.replace("maxArea = 0.0", "var maxArea = 0.0")
    better = better.replace("bestApprox = MatOfPoint2f()", "var bestApprox = MatOfPoint2f()")
    
    text = text[:start_idx] + better + "\n\n" + text[end_idx:]
    with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
        f.write(text)
else:
    print("Could not find start or end strings")

