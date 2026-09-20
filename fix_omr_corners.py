import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

# I will replace the findCorner logic to return the center of mass
old_corner = """        if (!found) {
            bestX = if (isLeft) width * 0.05f else width * 0.95f
            bestY = if (isTop) height * 0.05f else height * 0.95f
        } else {
            var extX = bestX
            var extY = bestY
            var extScore = Float.MAX_VALUE
                           
            val targetX = if (isLeft) 0f else width.toFloat()
            val targetY = if (isTop) 0f else height.toFloat()
            for (wx in (bestX - windowSize).toInt().coerceAtLeast(0) until (bestX + windowSize).toInt().coerceAtMost(width)) {
                for (wy in (bestY - windowSize).toInt().coerceAtLeast(0) until (bestY + windowSize).toInt().coerceAtMost(height)) {
                    val pixel = bitmap.getPixel(wx, wy)
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                    if (gray < threshold + 10) {
                        val dist = Math.abs(wx - targetX) + Math.abs(wy - targetY)
                        if (dist < extScore) {
                            extScore = dist
                            extX = wx.toFloat()
                            extY = wy.toFloat()
                        }
                    }
                }
            }
            bestX = extX
            bestY = extY
        }
        return Pair(bestX, bestY)"""

new_corner = """        if (!found) {
            bestX = if (isLeft) width * 0.05f else width * 0.95f
            bestY = if (isTop) height * 0.05f else height * 0.95f
            return Pair(bestX, bestY)
        } else {
            // bestX and bestY point to the center of the dark window found.
            // Let's use findCenterOfMass in that area to get the exact geometric center.
            val radius = windowSize
            val startX_mass = (bestX - radius).toInt().coerceAtLeast(0)
            val startY_mass = (bestY - radius).toInt().coerceAtLeast(0)
            val endX_mass = (bestX + radius).toInt().coerceAtMost(width)
            val endY_mass = (bestY + radius).toInt().coerceAtMost(height)
            
            val center = findCenterOfMass(bitmap, startX_mass, startY_mass, endX_mass, endY_mass)
            return center ?: Pair(bestX, bestY)
        }"""

content = content.replace(old_corner, new_corner)

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(content)
