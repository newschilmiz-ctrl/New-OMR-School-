import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

# Add drawing logic for answers
old_logic = """            // A valid mark should be significantly darker than the empty bubbles and mostly filled
            val studentAns = if (maxDarkness > 0.25f && (maxDarkness - secondMaxDarkness) > 0.10f && bestOpt != -1) bestOpt else -1
            answers.add(studentAns)"""

new_logic = """            // A valid mark should be significantly darker than the empty bubbles and mostly filled
            val studentAns = if (maxDarkness > 0.25f && (maxDarkness - secondMaxDarkness) > 0.10f && bestOpt != -1) bestOpt else -1
            
            if (studentAns != -1) {
                val vx = qStartX + studentAns * ansSpacingX
                val vy = qStartY
                val actual = mapVirtualToActual(vx, vy)
                canvas.drawCircle(actual.first, actual.second, ansBubbleRadius * 1.5f, paintRed)
            }
            
            answers.add(studentAns)"""

content = content.replace(old_logic, new_logic)

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(content)
