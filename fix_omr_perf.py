import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

# Replace getPixel with array access
content = content.replace("bitmap.getPixel(px, py)", "pixels[py * width + px]")
content = content.replace("bitmap.getPixel(x, y)", "pixels[y * width + x]")
content = content.replace("bitmap.getPixel(wx, wy)", "pixels[wy * width + wx]")

# We need to extract `pixels` at the very beginning of `scan`
scan_start = r"fun scan\(bitmap: Bitmap, numQuestions: Int, numOptions: Int\): ScanResult \{"
scan_start_repl = """fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int): ScanResult {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)"""
content = re.sub(scan_start, scan_start_repl, content)

# Remove the other getPixels calls and their width/height inside scan
content = re.sub(r"val width = bitmap\.width\s*val height = bitmap\.height\s*val pixels = IntArray\(width \* height\)\s*bitmap\.getPixels\(pixels, 0, width, 0, 0, width, height\)", "", content)

# Remove other redundant val width/height
content = re.sub(r"val width = bitmap\.width\s*val height = bitmap\.height", "", content, count=1) # the one right after paintText

# Now fix sampleDarkness signature and usages
content = content.replace("private fun sampleDarkness(bitmap: Bitmap", "private fun sampleDarkness(pixels: IntArray, width: Int, height: Int")
content = content.replace("sampleDarkness(bitmap", "sampleDarkness(pixels, width, height")

# Now fix findCorner signature and usages
content = content.replace("private fun findCorner(bitmap: Bitmap", "private fun findCorner(pixels: IntArray, width: Int, height: Int")
content = content.replace("findCorner(bitmap", "findCorner(pixels, width, height")

# Remove val width = bitmap.width inside findCorner
content = re.sub(r"val width = bitmap\.width\s*val height = bitmap\.height", "", content)

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(content)
