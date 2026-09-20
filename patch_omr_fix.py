import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

# 1. Update findCornersOpenCV
new_find_corners = """
    private fun findCornersOpenCV(gray: Mat): List<Point>? {
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(7.0, 7.0), 0.0)
        
        val thresh = Mat()
        Imgproc.adaptiveThreshold(blurred, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 51, 10.0)
        
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        
        val squareCenters = mutableListOf<Point>()
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > 150 && area < 10000) {
                val rect = Imgproc.boundingRect(contour)
                val aspectRatio = rect.width.toDouble() / rect.height
                val extent = area / (rect.width * rect.height)
                
                if (aspectRatio > 0.6 && aspectRatio < 1.4 && extent > 0.5) {
                    val center = Point(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0)
                    
                    val mask = Mat.zeros(gray.size(), CvType.CV_8U)
                    Imgproc.drawContours(mask, listOf(contour), -1, Scalar(255.0), -1)
                    val mean = Core.mean(gray, mask).`val`[0]
                    if (mean < 130) {
                        squareCenters.add(center)
                    }
                }
            }
        }
        
        if (squareCenters.size >= 4) {
            val w = gray.width().toDouble()
            val h = gray.height().toDouble()
            
            var tl = squareCenters[0]
            var tr = squareCenters[0]
            var bl = squareCenters[0]
            var br = squareCenters[0]
            
            var minTl = Double.MAX_VALUE
            var minTr = Double.MAX_VALUE
            var minBl = Double.MAX_VALUE
            var minBr = Double.MAX_VALUE
            
            for (pt in squareCenters) {
                val valTl = pt.x + pt.y
                val valTr = (w - pt.x) + pt.y
                val valBl = pt.x + (h - pt.y)
                val valBr = (w - pt.x) + (h - pt.y)
                
                if (valTl < minTl) { minTl = valTl; tl = pt }
                if (valTr < minTr) { minTr = valTr; tr = pt }
                if (valBl < minBl) { minBl = valBl; bl = pt }
                if (valBr < minBr) { minBr = valBr; br = pt }
            }
            
            val areaQuad = 0.5 * Math.abs(
                tl.x*tr.y - tl.y*tr.x + 
                tr.x*br.y - tr.y*br.x + 
                br.x*bl.y - br.y*bl.x + 
                bl.x*tl.y - bl.y*tl.x
            )
            
            if (areaQuad > w * h * 0.1) {
                return listOf(tl, tr, bl, br)
            }
        }
        
        var maxAreaContour = 0.0
        var bestApprox = MatOfPoint2f()
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > gray.width() * gray.height() * 0.1) {
                val contour2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(contour2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)
                
                if (approx.total() == 4L && area > maxAreaContour) {
                    maxAreaContour = area
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
start_idx = text.find("private fun findMarkerInRegion")
if start_idx != -1:
    text = text[:start_idx] + new_find_corners.strip() + "\n"

# 2. Update getFillPercentage
old_fill = "val r = (radius * 0.8).toInt() // Slightly smaller ROI to avoid bubble border"
new_fill = "val r = (radius * 0.5).toInt() // Much smaller ROI to strictly check the center core only"
text = text.replace(old_fill, new_fill)

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)

print("Patch applied")

