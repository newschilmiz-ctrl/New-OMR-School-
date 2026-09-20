import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

# Update dst array
dst_old = """
        // Destination points (virtual sheet 1000x1414)
        // The corner squares are drawn at margin 30, with size 40x40.
        // So their centers are at (50, 50), (950, 50), (950, 1364), (50, 1364)
        val dst = floatArrayOf(
            50f, 50f,
            950f, 50f,
            950f, 1364f,
            50f, 1364f
        )
"""
dst_new = """
        // Destination points based on the 4 markers bounding the answer section
        val dst = floatArrayOf(
            0f, 0f,
            1000f, 0f,
            1000f, 1000f,
            0f, 1000f
        )
"""
text = text.replace(dst_old.strip(), dst_new.strip())

# Update Set coords
set_old = """
        // Read Set
        val setStartX = 215f
        val setStartY = 590f
        val setSpacingY = 60f
"""
set_new = """
        // Read Set
        val setStartX = 120f
        val setStartY = 50f
        val setSpacingY = 94.736f
"""
text = text.replace(set_old.strip(), set_new.strip())

# Update ans coords
ans_old = """
        // Read Answers
        val ansBaseX = 316f
        val ansBaseY = 599.5f
        val ansColStrideX = 130f
        val ansSpacingX = 21f
        val ansSpacingY = 39f
"""
ans_new = """
        // Read Answers
        val ansBaseX = 230f
        val ansBaseY = 50f
        val ansColStrideX = 160f
        val ansSpacingX = 30f
        val ansSpacingY = 47.368f
"""
text = text.replace(ans_old.strip(), ans_new.strip())

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)

