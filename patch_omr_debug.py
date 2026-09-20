import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

# Make ansBubbleRadius bigger
text = text.replace("val ansBubbleRadius = 11.0", "val ansBubbleRadius = 14.0")

# Draw the 4 corners we used for perspective transform
old_transform = """
        val warped = Mat()
        Imgproc.warpPerspective(mat, warped, perspectiveTransform, Size(w, h))
"""

new_transform = """
        val warped = Mat()
        Imgproc.warpPerspective(mat, warped, perspectiveTransform, Size(w, h))
        
        // Draw the srcPoints on the original mat for debugging if they were found
        val colorCorner = Scalar(255.0, 0.0, 255.0, 255.0) // Magenta
        for (pt in srcPoints) {
            Imgproc.circle(mat, pt, 20, colorCorner, -1)
        }
"""

text = text.replace(old_transform, new_transform)

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)

print("Debug patch applied")
