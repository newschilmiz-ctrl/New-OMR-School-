import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

target = """    private fun findCorner(bitmap: Bitmap, isLeft: Boolean, isTop: Boolean): Pair<Float, Float> {
        val width = bitmap.width
        val height = bitmap.height
        
        val searchWidth = width / 3
        val searchHeight = height / 3
        
        val startX = if (isLeft) 0 else width - searchWidth
        val startY = if (isTop) 0 else height - searchHeight
        
        val endX = if (isLeft) searchWidth else width
        val endY = if (isTop) searchHeight else height
        
        val step = Math.max(1, width / 200)
        val windowSize = width / 30
        
        var bestScore = Float.MAX_VALUE
        var bestX = if (isLeft) 0f else width.toFloat()
        var bestY = if (isTop) 0f else height.toFloat()
        var found = false
        
        for (x in startX until endX - windowSize step step) {
            for (y in startY until endY - windowSize step step) {
                var darkCount = 0
                var total = 0
                
                for (wx in x until x + windowSize step step) {
                    for (wy in y until y + windowSize step step) {
                        val pixel = bitmap.getPixel(wx, wy)
                        val r = android.graphics.Color.red(pixel)
                        val g = android.graphics.Color.green(pixel)
                        val b = android.graphics.Color.blue(pixel)
                        val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                        if (gray < 120) darkCount++
                        total++
                    }
                }
                
                if (total > 0 && darkCount.toFloat() / total > 0.5f) {
                    val cx = x + windowSize / 2f
                    val cy = y + windowSize / 2f
                    
                    val distCornerX = if (isLeft) cx else (width - cx)
                    val distCornerY = if (isTop) cy else (height - cy)
                    val score = distCornerX + distCornerY
                    
                    if (score < bestScore) {
                        bestScore = score
                        bestX = cx
                        bestY = cy
                        found = true
                    }
                }
            }
        }
        
        if (!found) {
            bestX = if (isLeft) width * 0.05f else width * 0.95f
            bestY = if (isTop) height * 0.05f else height * 0.95f
        } else {
            // Find the extreme corner pixel of this block to avoid skew from different block sizes
            var extX = bestX
            var extY = bestY
            var extScore = if (isLeft && isTop) Float.MAX_VALUE
                            else if (!isLeft && isTop) Float.MAX_VALUE
                            else if (isLeft && !isTop) Float.MAX_VALUE
                            else Float.MAX_VALUE
                           
            // We want to minimize distance to the actual image corner
            val targetX = if (isLeft) 0f else width.toFloat()
            val targetY = if (isTop) 0f else height.toFloat()
            for (wx in (bestX - windowSize).toInt().coerceAtLeast(0) until (bestX + windowSize).toInt().coerceAtMost(width)) {
                for (wy in (bestY - windowSize).toInt().coerceAtLeast(0) until (bestY + windowSize).toInt().coerceAtMost(height)) {
                    val pixel = bitmap.getPixel(wx, wy)
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                    if (gray < 130) {
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
        
        return Pair(bestX, bestY)
    }"""

replacement = """    private fun findCorner(bitmap: Bitmap, isLeft: Boolean, isTop: Boolean): Pair<Float, Float> {
        val width = bitmap.width
        val height = bitmap.height
        
        val searchWidth = width / 3
        val searchHeight = height / 3
        
        val startX = if (isLeft) 0 else width - searchWidth
        val startY = if (isTop) 0 else height - searchHeight
        
        val endX = if (isLeft) searchWidth else width
        val endY = if (isTop) searchHeight else height
        
        val step = Math.max(1, width / 200)
        val windowSize = width / 30
        
        var bestScore = Float.MAX_VALUE
        var bestX = if (isLeft) 0f else width.toFloat()
        var bestY = if (isTop) 0f else height.toFloat()
        var found = false
        
        // Sample image brightness
        var bgSum = 0
        var bgCount = 0
        for (x in startX until endX step step * 2) {
            for (y in startY until endY step step * 2) {
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
        
        for (x in startX until endX - windowSize step step) {
            for (y in startY until endY - windowSize step step) {
                var darkCount = 0
                var total = 0
                
                for (wx in x until x + windowSize step step) {
                    for (wy in y until y + windowSize step step) {
                        val pixel = bitmap.getPixel(wx, wy)
                        val r = android.graphics.Color.red(pixel)
                        val g = android.graphics.Color.green(pixel)
                        val b = android.graphics.Color.blue(pixel)
                        val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                        if (gray < threshold) darkCount++
                        total++
                    }
                }
                
                if (total > 0 && darkCount.toFloat() / total > 0.4f) {
                    val cx = x + windowSize / 2f
                    val cy = y + windowSize / 2f
                    
                    val distCornerX = if (isLeft) cx else (width - cx)
                    val distCornerY = if (isTop) cy else (height - cy)
                    val score = distCornerX + distCornerY
                    
                    if (score < bestScore) {
                        bestScore = score
                        bestX = cx
                        bestY = cy
                        found = true
                    }
                }
            }
        }
        
        if (!found) {
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
        
        return Pair(bestX, bestY)
    }"""

if target in content:
    content = content.replace(target, replacement)
    print("findCorner patched")
else:
    print("findCorner NOT FOUND")

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(content)
