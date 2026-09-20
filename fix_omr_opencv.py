import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

opencv_imports = """
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
"""

if "import org.opencv.android.Utils" not in text:
    text = text.replace("import android.util.Log", "import android.util.Log\n" + opencv_imports.strip())

find_corners_old = """
    private fun findCorner(bitmap: Bitmap, isLeft: Boolean, isTop: Boolean): Pair<Float, Float> {
"""

find_corners_new = """
    private fun findCornersOpenCV(bitmap: Bitmap): List<Pair<Float, Float>> {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
        val thresh = Mat()
        Imgproc.adaptiveThreshold(blurred, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2.0)
        
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        
        var maxArea = 0.0
        var bestApprox = MatOfPoint2f()
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > 1000) { // arbitrary area threshold
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
        
        if (bestApprox.total() != 4L) {
            // Fallback: corners of the image
            return listOf(
                Pair(0f, 0f),
                Pair(bitmap.width.toFloat(), 0f),
                Pair(bitmap.width.toFloat(), bitmap.height.toFloat()),
                Pair(0f, bitmap.height.toFloat())
            )
        }
        
        val points = bestApprox.toList()
        
        // Sort points: TL, TR, BR, BL
        val sortedByY = points.sortedBy { it.y }
        val top = sortedByY.take(2).sortedBy { it.x }
        val bottom = sortedByY.drop(2).sortedBy { it.x }
        
        val tl = top[0]
        val tr = top[1]
        val bl = bottom[0]
        val br = bottom[1]
        
        return listOf(
            Pair(tl.x.toFloat(), tl.y.toFloat()),
            Pair(tr.x.toFloat(), tr.y.toFloat()),
            Pair(bl.x.toFloat(), bl.y.toFloat()),
            Pair(br.x.toFloat(), br.y.toFloat())
        )
    }

    private fun findCorner(bitmap: Bitmap, isLeft: Boolean, isTop: Boolean): Pair<Float, Float> {
"""

text = text.replace(find_corners_old.strip(), find_corners_new.strip())


scan_old = """
        // Find the 4 corner squares of the OMR sheet
        val tl = findCorner(bitmap, isLeft = true, isTop = true)
        val tr = findCorner(bitmap, isLeft = false, isTop = true)
        val bl = findCorner(bitmap, isLeft = true, isTop = false)
        val br = findCorner(bitmap, isLeft = false, isTop = false)
"""

scan_new = """
        // Find the 4 corner squares of the OMR sheet using OpenCV
        val corners = findCornersOpenCV(bitmap)
        val tl = corners[0]
        val tr = corners[1]
        val bl = corners[2]
        val br = corners[3]
"""

text = text.replace(scan_old.strip(), scan_new.strip())

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)

