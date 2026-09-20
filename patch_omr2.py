import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

new_code = """
    private fun findMarkerInRegion(gray: Mat, xStart: Int, xEnd: Int, yStart: Int, yEnd: Int): Point? {
        val windowSize = (gray.width() * 0.022).toInt() // ~2.2% of width
        val step = Math.max(1, windowSize / 4)
        
        var minMean = 255.0
        var bestCx = -1
        var bestCy = -1
        
        for (y in yStart until (yEnd - windowSize) step step) {
            for (x in xStart until (xEnd - windowSize) step step) {
                val roi = gray.submat(Rect(x, y, windowSize, windowSize))
                val mean = Core.mean(roi).`val`[0]
                if (mean < minMean) {
                    minMean = mean
                    bestCx = x + windowSize / 2
                    bestCy = y + windowSize / 2
                }
            }
        }
        
        if (minMean < 120) {
            return Point(bestCx.toDouble(), bestCy.toDouble())
        }
        return null
    }

    private fun findCornersOpenCV(gray: Mat): List<Point>? {
        val w = gray.width()
        val h = gray.height()
        
        // Expected regions for the 4 markers
        // TL: Left 1-12%, Top 28-42%
        val tl = findMarkerInRegion(gray, (w * 0.01).toInt(), (w * 0.12).toInt(), (h * 0.28).toInt(), (h * 0.42).toInt())
        // TR: Right 88-99%, Top 28-42%
        val tr = findMarkerInRegion(gray, (w * 0.88).toInt(), (w * 0.99).toInt(), (h * 0.28).toInt(), (h * 0.42).toInt())
        // BL: Left 1-12%, Bottom 85-98%
        val bl = findMarkerInRegion(gray, (w * 0.01).toInt(), (w * 0.12).toInt(), (h * 0.85).toInt(), (h * 0.98).toInt())
        // BR: Right 88-99%, Bottom 85-98%
        val br = findMarkerInRegion(gray, (w * 0.88).toInt(), (w * 0.99).toInt(), (h * 0.85).toInt(), (h * 0.98).toInt())
        
        if (tl != null && tr != null && bl != null && br != null) {
            return listOf(tl, tr, bl, br)
        }
        return null
    }
}
"""

start_str = "    private fun findCornersOpenCV(gray: Mat): List<Point>? {"

start_idx = text.find(start_str)

if start_idx != -1:
    text = text[:start_idx] + new_code.strip() + "\n"
    with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
        f.write(text)
    print("Patched successfully")
else:
    print("Could not find start string")

