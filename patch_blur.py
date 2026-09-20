import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

old_thresh = """
        // Apply Thresholding on warped image for bubble detection
        val warpedThresh = Mat()
        // Adaptive thresholding to handle uneven lighting
        Imgproc.adaptiveThreshold(warpedGray, warpedThresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 31, 15.0)
"""

new_thresh = """
        // Apply Thresholding on warped image for bubble detection
        val warpedBlurred = Mat()
        Imgproc.GaussianBlur(warpedGray, warpedBlurred, Size(5.0, 5.0), 0.0)
        
        val warpedThresh = Mat()
        // Adaptive thresholding to handle uneven lighting
        Imgproc.adaptiveThreshold(warpedBlurred, warpedThresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 31, 15.0)
"""

text = text.replace(old_thresh.strip(), new_thresh.strip())

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)

