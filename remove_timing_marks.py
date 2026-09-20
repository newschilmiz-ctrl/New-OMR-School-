import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

# Match from val numTimingMarks = 48 down to the end of mapVirtualToActual
pattern = re.compile(r"        // Enhance mapping using the 48 timing marks on the left and right\n        val numTimingMarks = 48.*?fun mapVirtualToActual\(vx: Float, vy: Float\): Pair<Float, Float> \{.*?\n        \}", re.DOTALL)

new_code = """        fun mapVirtualToActual(vx: Float, vy: Float): Pair<Float, Float> {
            val pts = floatArrayOf(vx, vy)
            matrix.mapPoints(pts)
            return Pair(pts[0], pts[1])
        }"""

match = pattern.search(content)
if match:
    content = content[:match.start()] + new_code + content[match.end():]
    with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
        f.write(content)
    print("Removed timing marks logic!")
else:
    print("Pattern not found!")
