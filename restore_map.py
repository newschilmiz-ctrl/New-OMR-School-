import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

pattern = re.compile(r"fun mapVirtualToActual\(vx: Float, vy: Float\): Pair<Float, Float> \{.*?\n        \}", re.DOTALL)
match = pattern.search(content)

new_func = """fun mapVirtualToActual(vx: Float, vy: Float): Pair<Float, Float> {
            if (vy <= startY) {
                val u = (vx - leftX) / (rightX - leftX)
                val ax = leftMarks[0].first + u * (rightMarks[0].first - leftMarks[0].first)
                val ay = leftMarks[0].second + u * (rightMarks[0].second - leftMarks[0].second)
                
                // Add the delta from matrix for vy < startY
                val pts0 = floatArrayOf(vx, vy)
                matrix.mapPoints(pts0)
                val ptsRef = floatArrayOf(vx, startY)
                matrix.mapPoints(ptsRef)
                
                return Pair(ax + (pts0[0] - ptsRef[0]), ay + (pts0[1] - ptsRef[1]))
            } else if (vy >= endY) {
                val u = (vx - leftX) / (rightX - leftX)
                val ax = leftMarks[numTimingMarks - 1].first + u * (rightMarks[numTimingMarks - 1].first - leftMarks[numTimingMarks - 1].first)
                val ay = leftMarks[numTimingMarks - 1].second + u * (rightMarks[numTimingMarks - 1].second - leftMarks[numTimingMarks - 1].second)
                
                val pts0 = floatArrayOf(vx, vy)
                matrix.mapPoints(pts0)
                val ptsRef = floatArrayOf(vx, endY)
                matrix.mapPoints(ptsRef)
                
                return Pair(ax + (pts0[0] - ptsRef[0]), ay + (pts0[1] - ptsRef[1]))
            } else {
                val indexF = (vy - startY) / timingStepY
                var i = indexF.toInt()
                if (i >= numTimingMarks - 1) i = numTimingMarks - 2
                val t = indexF - i
                
                val lx = leftMarks[i].first + (leftMarks[i+1].first - leftMarks[i].first) * t
                val ly = leftMarks[i].second + (leftMarks[i+1].second - leftMarks[i].second) * t
                
                val rx = rightMarks[i].first + (rightMarks[i+1].first - rightMarks[i].first) * t
                val ry = rightMarks[i].second + (rightMarks[i+1].second - rightMarks[i].second) * t
                
                val u = (vx - leftX) / (rightX - leftX)
                val ax = lx + u * (rx - lx)
                val ay = ly + u * (ry - ly)
                
                return Pair(ax, ay)
            }
        }"""

if match:
    content = content[:match.start()] + new_func + content[match.end():]
    with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
        f.write(content)
    print("Replaced!")
