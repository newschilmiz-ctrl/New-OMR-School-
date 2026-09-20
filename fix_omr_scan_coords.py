import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

old_logic = """
            // Read Roll No Bubbles
            val rStartX = 300f
            val rStartY = 200f
            val rSpacingX = 45f
            val rSpacingY = 30f
"""

new_logic = """
            // Read Roll No Bubbles
            val rStartX = 200f
            val rStartY = 140f
            val rSpacingX = 40f
            val rSpacingY = 25f
"""

text = text.replace(old_logic.strip(), new_logic.strip())

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)

