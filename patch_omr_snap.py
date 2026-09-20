import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

# Find bubble snapping logic
snap_logic = """
        // --- BUBBLE SNAPPING ---
        val bubbleCenters = mutableListOf<Point>()
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(warpedThresh, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            val rect = Imgproc.boundingRect(contour)
            val aspectRatio = rect.width.toDouble() / rect.height
            if (area > 50 && area < 1500 && aspectRatio > 0.6 && aspectRatio < 1.6) {
                 val cx = rect.x + rect.width / 2.0
                 val cy = rect.y + rect.height / 2.0
                 bubbleCenters.add(Point(cx, cy))
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
        // ------------------------
"""

# We need to insert this before reading Set
insert_idx = text.find("// Read Set")
text = text[:insert_idx] + snap_logic + "\n        " + text[insert_idx:]

# Replace cx, cy for SET
text = text.replace(
    """val cx = setStartX
            val cy = setStartY + i * setSpacingY""",
    """var cx = setStartX
            var cy = setStartY + i * setSpacingY
            
            val snapped = snapToNearest(cx, cy, bubbleCenters, 30.0)
            cx = snapped.x
            cy = snapped.y
"""
)

# Replace cx, cy for answers
text = text.replace(
    """val cx = qStartX + opt * ansSpacingX
                val cy = qStartY""",
    """var cx = qStartX + opt * ansSpacingX
                var cy = qStartY
                
                val snapped = snapToNearest(cx, cy, bubbleCenters, 25.0)
                cx = snapped.x
                cy = snapped.y"""
)

# Fix cx, cy in paperSet (the final circle draw)
text = text.replace(
    """val cx = setStartX
                val cy = setStartY + bestSetRow * setSpacingY
                Imgproc.circle""",
    """val cx = setStartX
                val cy = setStartY + bestSetRow * setSpacingY
                val snapped = snapToNearest(cx, cy, bubbleCenters, 30.0)
                Imgproc.circle(warpedAnnotated, snapped, bubbleRadius.toInt(), colorGreen, -1)
                // Imgproc.circle"""
)

text = text.replace(
    """val cx = qStartX + studentAns * ansSpacingX
                val cy = qStartY
                Imgproc.circle""",
    """val cx = qStartX + studentAns * ansSpacingX
                val cy = qStartY
                val snapped = snapToNearest(cx, cy, bubbleCenters, 25.0)
                Imgproc.circle(warpedAnnotated, snapped, ansBubbleRadius.toInt(), colorRed, -1)
                // Imgproc.circle"""
)

text = text.replace(
    """val cx = qStartX + bestOpt * ansSpacingX
                val cy = qStartY
                Imgproc.circle""",
    """val cx = qStartX + bestOpt * ansSpacingX
                val cy = qStartY
                val snapped = snapToNearest(cx, cy, bubbleCenters, 25.0)
                Imgproc.circle(warpedAnnotated, snapped, ansBubbleRadius.toInt(), Scalar(255.0, 255.0, 0.0, 255.0), 2)
                // Imgproc.circle"""
)

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)
print("Snap logic applied")
