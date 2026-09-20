import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

# Let's just use regex to replace the loop
pattern = re.compile(r"for \(i in 0 until numTimingMarks\) \{.*?\n\s+fun mapVirtualToActual", re.DOTALL)
match = pattern.search(content)

if match:
    new_loop = """for (i in 0 until numTimingMarks) {
            val ty = startY + i * timingStepY
            
            // Left mark
            val ptsL = floatArrayOf(leftX, ty)
            matrix.mapPoints(ptsL)
            var roughLx = ptsL[0].toInt()
            var roughLy = ptsL[1].toInt()
            
            // Use local prediction if available for better accuracy
            if (i >= 2) {
                roughLx = (2 * leftMarks[i-1].first - leftMarks[i-2].first).toInt()
                roughLy = (2 * leftMarks[i-1].second - leftMarks[i-2].second).toInt()
            } else if (i == 1) {
                val pt0 = floatArrayOf(leftX, startY)
                val pt1 = floatArrayOf(leftX, ty)
                matrix.mapPoints(pt0)
                matrix.mapPoints(pt1)
                roughLx = (leftMarks[0].first + (pt1[0] - pt0[0])).toInt()
                roughLy = (leftMarks[0].second + (pt1[1] - pt0[1])).toInt()
            }
            
            val searchRadiusY = Math.min(searchRadius, (bitmap.height * 0.012f).toInt())
            
            val actualL = findCenterOfMass(
                bitmap, 
                Math.max(0, roughLx - searchRadius), Math.max(0, roughLy - searchRadiusY), 
                Math.min(bitmap.width, roughLx + searchRadius), Math.min(bitmap.height, roughLy + searchRadiusY)
            ) ?: Pair(roughLx.toFloat(), roughLy.toFloat())
            leftMarks[i] = actualL
            
            // Right mark
            val ptsR = floatArrayOf(rightX, ty)
            matrix.mapPoints(ptsR)
            var roughRx = ptsR[0].toInt()
            var roughRy = ptsR[1].toInt()
            
            if (i >= 2) {
                roughRx = (2 * rightMarks[i-1].first - rightMarks[i-2].first).toInt()
                roughRy = (2 * rightMarks[i-1].second - rightMarks[i-2].second).toInt()
            } else if (i == 1) {
                val pt0 = floatArrayOf(rightX, startY)
                val pt1 = floatArrayOf(rightX, ty)
                matrix.mapPoints(pt0)
                matrix.mapPoints(pt1)
                roughRx = (rightMarks[0].first + (pt1[0] - pt0[0])).toInt()
                roughRy = (rightMarks[0].second + (pt1[1] - pt0[1])).toInt()
            }
            
            val actualR = findCenterOfMass(
                bitmap, 
                Math.max(0, roughRx - searchRadius), Math.max(0, roughRy - searchRadiusY), 
                Math.min(bitmap.width, roughRx + searchRadius), Math.min(bitmap.height, roughRy + searchRadiusY)
            ) ?: Pair(roughRx.toFloat(), roughRy.toFloat())
            rightMarks[i] = actualR
        }

        fun mapVirtualToActual"""
        
    content = content[:match.start()] + new_loop + content[match.end():]
    with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
        f.write(content)
    print("Replaced!")
else:
    print("Pattern not found!")
