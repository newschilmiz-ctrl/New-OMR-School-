import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

old_fallback = """
        val srcPoints = if (corners != null) {
            corners
        } else {
            // Absolute fallback
            listOf(
                Point(0.0, 0.0),
                Point(bitmap.width.toDouble(), 0.0),
                Point(0.0, bitmap.height.toDouble()),
                Point(bitmap.width.toDouble(), bitmap.height.toDouble())
            )
        }
"""

new_fallback = """
        val srcPoints = if (corners != null) {
            corners
        } else {
            // Absolute fallback: Assume image is perfectly cropped A4 page, 
            // guess the marker positions based on expected layout percentages.
            listOf(
                Point(bitmap.width * 0.05, bitmap.height * 0.35),
                Point(bitmap.width * 0.95, bitmap.height * 0.35),
                Point(bitmap.width * 0.05, bitmap.height * 0.96),
                Point(bitmap.width * 0.95, bitmap.height * 0.96)
            )
        }
"""

text = text.replace(old_fallback.strip(), new_fallback.strip())
with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)

