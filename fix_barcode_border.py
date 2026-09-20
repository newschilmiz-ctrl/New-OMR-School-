import re

with open("app/src/main/java/com/example/util/OmrGenerator.kt", "r") as f:
    content = f.read()

pattern = re.compile(r"        if \(barcodeBmp != null\) \{\n            canvas\.drawBitmap\(barcodeBmp, col2X, barcodeY, null\)\n        \} else \{")

new_code = """        // Draw a scan-type border around the barcode area
        val scanPadding = 4f
        canvas.drawRect(col2X - scanPadding, barcodeY - scanPadding, col2X + barcodeW + scanPadding, barcodeY + barcodeH + scanPadding, thinStroke)
        
        if (barcodeBmp != null) {
            canvas.drawBitmap(barcodeBmp, col2X, barcodeY, null)
        } else {"""

if pattern.search(content):
    content = pattern.sub(new_code, content)
    with open("app/src/main/java/com/example/util/OmrGenerator.kt", "w") as f:
        f.write(content)
    print("Replaced!")
else:
    print("Not found!")
