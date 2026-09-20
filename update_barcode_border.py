import re

with open("app/src/main/java/com/example/util/OmrGenerator.kt", "r") as f:
    content = f.read()

pattern = re.compile(r"        // Draw a scan-type border around the barcode area\n        val scanPadding = 4f\n        canvas\.drawRect\(col2X - scanPadding, barcodeY - scanPadding, col2X \+ barcodeW \+ scanPadding, barcodeY \+ barcodeH \+ scanPadding, thinStroke\)")

new_code = """        // Draw scan-type corner borders around the 2D barcode area
        val scanPadding = 5f
        val bX = col2X - scanPadding
        val bY = barcodeY - scanPadding
        val bW = barcodeW + 2 * scanPadding
        val bH = barcodeH + 2 * scanPadding
        val cLen = 15f
        
        // top left
        canvas.drawLine(bX, bY, bX + cLen, bY, thinStroke)
        canvas.drawLine(bX, bY, bX, bY + cLen, thinStroke)
        // top right
        canvas.drawLine(bX + bW - cLen, bY, bX + bW, bY, thinStroke)
        canvas.drawLine(bX + bW, bY, bX + bW, bY + cLen, thinStroke)
        // bottom left
        canvas.drawLine(bX, bY + bH, bX + cLen, bY + bH, thinStroke)
        canvas.drawLine(bX, bY + bH - cLen, bX, bY + bH, thinStroke)
        // bottom right
        canvas.drawLine(bX + bW - cLen, bY + bH, bX + bW, bY + bH, thinStroke)
        canvas.drawLine(bX + bW, bY + bH - cLen, bX + bW, bY + bH, thinStroke)"""

if pattern.search(content):
    content = pattern.sub(new_code, content)
    with open("app/src/main/java/com/example/util/OmrGenerator.kt", "w") as f:
        f.write(content)
    print("Replaced!")
else:
    print("Not found!")
