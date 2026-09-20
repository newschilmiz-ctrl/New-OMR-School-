import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

better_find_corners = """
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
            if (area > 200 && area < gray.width() * gray.height() / 20) { 
                val rect = Imgproc.boundingRect(contour)
                val aspectRatio = rect.width.toDouble() / rect.height
                
                // Allow slightly more lenient aspect ratio for tilted squares
                if (aspectRatio > 0.6 && aspectRatio < 1.4) {
                    // Check if it's mostly black inside
                    val mask = Mat.zeros(gray.size(), CvType.CV_8U)
                    Imgproc.drawContours(mask, listOf(contour), -1, Scalar(255.0), -1)
                    val mean = Core.mean(gray, mask).`val`[0]
                    if (mean < 120) { 
                        // To avoid adding both inner and outer contours of the same marker,
                        // check distance to existing centers
                        val center = Point(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0)
                        var isDuplicate = false
                        for (existing in squareCenters) {
                            val dist = Math.hypot(existing.x - center.x, existing.y - center.y)
                            if (dist < 20.0) {
                                isDuplicate = true
                                break
                            }
                        }
                        if (!isDuplicate) {
                            squareCenters.add(center)
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
            
            // Check if they form a reasonable quadrilateral
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
"""

start_str = "    private fun findCornersOpenCV(gray: Mat): List<Point>? {"
end_str = "}"

start_idx = text.find(start_str)
end_idx = text.rfind(end_str)

if start_idx != -1 and end_idx != -1:
    text = text[:start_idx] + better_find_corners.strip() + "\n}\n"
    with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
        f.write(text)
else:
    print("Could not find start or end strings")

