import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

old_logic = """
            // Read Roll No Bubbles
            val rStartX = 200f
            val rStartY = 140f
            val rSpacingX = 40f
            val rSpacingY = 25f
            
            var rollNoStr = ""
            for (col in 0 until 7) {
                var bestDigit = -1
                var maxDarkness = 0f
                var secondMaxDarkness = 0f
                
                for (row in 0..9) {
                    val cx = rStartX + col * rSpacingX
                    val cy = rStartY + 20f + row * rSpacingY
"""

new_logic = """
            // Read Roll No Bubbles
            val rStartX = 150f
            val rStartY = 120f
            val rSpacingX = 42f
            val rSpacingY = 28f
            
            var rollNoStr = ""
            for (col in 0 until 7) {
                var bestDigit = -1
                var maxDarkness = 0f
                var secondMaxDarkness = 0f
                
                for (row in 0..9) {
                    val cx = rStartX + col * rSpacingX
                    val cy = rStartY + 28f + row * rSpacingY
"""

text = text.replace(old_logic.strip(), new_logic.strip())

# The scanner also has a second place `val cy = rStartY + 20f + bestDigit * rSpacingY` to replace
text = text.replace('val cy = rStartY + 20f + bestDigit * rSpacingY', 'val cy = rStartY + 28f + bestDigit * rSpacingY')

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)

