import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

# 1. Update sampleDarkness
target_sampleDarkness = """    private fun sampleDarkness(bitmap: Bitmap, matrix: Matrix, virtualX: Float, virtualY: Float, actualX: Float, actualY: Float, virtualRadius: Float): Float {
        // Map radius to image coordinates approximately
        val ptsRadius = floatArrayOf(virtualX + virtualRadius, virtualY)
        matrix.mapPoints(ptsRadius)
        val ptsCenter = floatArrayOf(virtualX, virtualY)
        matrix.mapPoints(ptsCenter)
        val rx = ptsRadius[0]
        val ry = ptsRadius[1]
        val radius = Math.hypot((rx - ptsCenter[0]).toDouble(), (ry - ptsCenter[1]).toDouble()).toInt()
        
        val actualRadius = (radius * 0.75f).toInt().coerceAtLeast(2) // Sample inner core avoiding borders

        val cx = actualX.toInt()
        val cy = actualY.toInt()

        if (cx - actualRadius < 0 || cy - actualRadius < 0 || cx + actualRadius >= bitmap.width || cy + actualRadius >= bitmap.height) {
            return 0f
        }

        var darkCount = 0
        var totalCount = 0

        for (dx in -actualRadius..actualRadius) {
            for (dy in -actualRadius..actualRadius) {
                if (dx * dx + dy * dy <= actualRadius * actualRadius) {
                    val pixel = bitmap.getPixel(cx + dx, cy + dy)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                    
                    // Threshold for dark pixels (0=black, 255=white)
                    // 140 ignores most light noise and printed text artifacts but catches pen/pencil better.
                    if (gray < 140) {
                        darkCount++
                    }
                    totalCount++
                }
            }
        }

        return if (totalCount == 0) 0f else darkCount.toFloat() / totalCount
    }"""

replacement_sampleDarkness = """    private fun sampleDarkness(bitmap: Bitmap, matrix: Matrix, virtualX: Float, virtualY: Float, actualX: Float, actualY: Float, virtualRadius: Float): Float {
        // Map radius to image coordinates approximately
        val ptsRadius = floatArrayOf(virtualX + virtualRadius, virtualY)
        matrix.mapPoints(ptsRadius)
        val ptsCenter = floatArrayOf(virtualX, virtualY)
        matrix.mapPoints(ptsCenter)
        val rx = ptsRadius[0]
        val ry = ptsRadius[1]
        val radius = Math.hypot((rx - ptsCenter[0]).toDouble(), (ry - ptsCenter[1]).toDouble()).toInt()
        
        val actualRadius = (radius * 0.75f).toInt().coerceAtLeast(2) // Sample inner core avoiding borders

        val cx = actualX.toInt().coerceIn(0, bitmap.width - 1)
        val cy = actualY.toInt().coerceIn(0, bitmap.height - 1)

        if (cx - actualRadius < 0 || cy - actualRadius < 0 || cx + actualRadius >= bitmap.width || cy + actualRadius >= bitmap.height) {
            return 0f
        }

        var darkCount = 0
        var totalCount = 0

        // Calculate local background brightness
        var bgSum = 0
        var bgCount = 0
        val bgRadius = (radius * 1.5f).toInt().coerceAtLeast(actualRadius + 2)
        val bgMinRadius = (radius * 1.1f).toInt().coerceAtLeast(actualRadius + 1)
        
        val startX = (cx - bgRadius).coerceAtLeast(0)
        val startY = (cy - bgRadius).coerceAtLeast(0)
        val endX = (cx + bgRadius).coerceAtMost(bitmap.width - 1)
        val endY = (cy + bgRadius).coerceAtMost(bitmap.height - 1)

        for (dx in startX - cx..endX - cx) {
            for (dy in startY - cy..endY - cy) {
                val distSq = dx * dx + dy * dy
                if (distSq in (bgMinRadius * bgMinRadius)..bgRadius * bgRadius) {
                    val px = cx + dx
                    val py = cy + dy
                    if (px in 0 until bitmap.width && py in 0 until bitmap.height) {
                        val pixel = bitmap.getPixel(px, py)
                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val b = Color.blue(pixel)
                        bgSum += (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                        bgCount++
                    }
                }
            }
        }
        
        // If we found a background average, use it to make an adaptive threshold
        val bgAvg = if (bgCount > 0) bgSum / bgCount else 200
        val threshold = Math.min(160, bgAvg - 40) // Must be significantly darker than local background

        for (dx in -actualRadius..actualRadius) {
            for (dy in -actualRadius..actualRadius) {
                if (dx * dx + dy * dy <= actualRadius * actualRadius) {
                    val px = (cx + dx).coerceIn(0, bitmap.width - 1)
                    val py = (cy + dy).coerceIn(0, bitmap.height - 1)
                    val pixel = bitmap.getPixel(px, py)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                    
                    if (gray < threshold) {
                        darkCount++
                    }
                    totalCount++
                }
            }
        }

        return if (totalCount == 0) 0f else darkCount.toFloat() / totalCount
    }"""

if target_sampleDarkness in content:
    content = content.replace(target_sampleDarkness, replacement_sampleDarkness)
else:
    print("Could not find sampleDarkness")

# 2. Update findCenterOfMass
target_findCenterOfMass = """    private fun findCenterOfMass(bitmap: Bitmap, startX: Int, startY: Int, endX: Int, endY: Int): Pair<Float, Float>? {
        var sumX = 0L
        var sumY = 0L
        var count = 0

        for (x in startX until endX) {
            for (y in startY until endY) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Convert to grayscale
                val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                
                // Assuming dark marker on white background. Threshold < 130 is dark.
                if (gray < 130) {
                    sumX += x
                    sumY += y
                    count++
                }
            }
        }

        return if (count > 50) { // arbitrary threshold to avoid noise"""

replacement_findCenterOfMass = """    private fun findCenterOfMass(bitmap: Bitmap, startX: Int, startY: Int, endX: Int, endY: Int): Pair<Float, Float>? {
        var sumX = 0L
        var sumY = 0L
        var count = 0
        
        // Find local background
        var bgSum = 0
        var bgCount = 0
        for (x in startX until endX step 2) {
            for (y in startY until endY step 2) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                bgSum += (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                bgCount++
            }
        }
        val bgAvg = if (bgCount > 0) bgSum / bgCount else 200
        val threshold = Math.min(140, bgAvg - 40)

        for (x in startX until endX) {
            for (y in startY until endY) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Convert to grayscale
                val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                
                if (gray < threshold) {
                    sumX += x
                    sumY += y
                    count++
                }
            }
        }

        return if (count > 20) { // lower threshold since area might be small"""

if target_findCenterOfMass in content:
    content = content.replace(target_findCenterOfMass, replacement_findCenterOfMass)
else:
    print("Could not find findCenterOfMass")


# 3. Update studentAns condition to be slightly more lenient since background subtraction is better
content = content.replace(
    "val studentAns = if (maxDarkness > 0.3f && (maxDarkness - secondMaxDarkness) > 0.12f && bestOpt != -1) bestOpt else -1",
    "val studentAns = if (maxDarkness > 0.25f && (maxDarkness - secondMaxDarkness) > 0.10f && bestOpt != -1) bestOpt else -1"
)

# 4. Same for paperSet
content = content.replace(
    "val paperSet = if (maxSetDarkness > 0.25f && (maxSetDarkness - secondMaxSetDarkness) > 0.12f && bestSetRow != -1) {",
    "val paperSet = if (maxSetDarkness > 0.25f && (maxSetDarkness - secondMaxSetDarkness) > 0.10f && bestSetRow != -1) {"
)

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(content)

print("Patch applied.")
