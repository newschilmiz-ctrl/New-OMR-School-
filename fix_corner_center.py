import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

pattern = re.compile(r"private fun findCorner\(bitmap: Bitmap, isLeft: Boolean, isTop: Boolean\): Pair<Float, Float> \{.*?\n    \}", re.DOTALL)
match = pattern.search(content)

new_func = """private fun findCorner(bitmap: Bitmap, isLeft: Boolean, isTop: Boolean): Pair<Float, Float> {
        val width = bitmap.width
        val height = bitmap.height
        
        val searchWidth = width / 4
        val searchHeight = height / 4
        
        val startX = if (isLeft) 0 else width - searchWidth
        val startY = if (isTop) 0 else height - searchHeight
        val endX = if (isLeft) searchWidth else width
        val endY = if (isTop) searchHeight else height
        
        var bgSum = 0
        var bgCount = 0
        val step = Math.max(1, width / 100)
        for (x in startX until endX step step) {
            for (y in startY until endY step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                bgSum += (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                bgCount++
            }
        }
        val bgAvg = if (bgCount > 0) bgSum / bgCount else 200
        val threshold = Math.min(130, bgAvg - 40)
        
        val windowSize = width / 25
        
        var bestScore = Float.MAX_VALUE
        var bestX = if (isLeft) width * 0.05f else width * 0.95f
        var bestY = if (isTop) height * 0.05f else height * 0.95f
        var found = false
        
        for (x in startX until endX - windowSize step step) {
            for (y in startY until endY - windowSize step step) {
                var darkCount = 0
                for (wx in x until x + windowSize step step) {
                    for (wy in y until y + windowSize step step) {
                        val pixel = bitmap.getPixel(wx, wy)
                        val r = android.graphics.Color.red(pixel)
                        val g = android.graphics.Color.green(pixel)
                        val b = android.graphics.Color.blue(pixel)
                        val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                        if (gray < threshold) darkCount++
                    }
                }
                
                val total = (windowSize/step) * (windowSize/step)
                if (total > 0 && darkCount.toFloat() / total > 0.4f) {
                    val cx = x + windowSize / 2f
                    val cy = y + windowSize / 2f
                    
                    val distCornerX = if (isLeft) cx else (width - cx)
                    val distCornerY = if (isTop) cy else (height - cy)
                    
                    val penaltyX = if (distCornerX < width * 0.015f) 500f else 0f
                    val penaltyY = if (distCornerY < height * 0.015f) 500f else 0f
                    
                    val score = distCornerX + distCornerY + penaltyX + penaltyY
                    
                    if (score < bestScore) {
                        bestScore = score
                        bestX = cx
                        bestY = cy
                        found = true
                    }
                }
            }
        }
        
        if (found) {
            var sumX = 0L
            var sumY = 0L
            var count = 0
            for (wx in (bestX - windowSize).toInt().coerceAtLeast(0) until (bestX + windowSize).toInt().coerceAtMost(width - 1)) {
                for (wy in (bestY - windowSize).toInt().coerceAtLeast(0) until (bestY + windowSize).toInt().coerceAtMost(height - 1)) {
                    val pixel = bitmap.getPixel(wx, wy)
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                    if (gray < threshold + 15) {
                        sumX += wx
                        sumY += wy
                        count++
                    }
                }
            }
            if (count > 0) {
                return Pair(sumX.toFloat() / count, sumY.toFloat() / count)
            }
        }
        
        return Pair(bestX, bestY)
    }"""

if match:
    content = content[:match.start()] + new_func + content[match.end():]
    with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
        f.write(content)
