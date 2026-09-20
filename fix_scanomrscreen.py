import re

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    content = f.read()

content = content.replace("val imageBitmap = remember(scanResult) { scanResult?.annotatedBitmap?.asImageBitmap() }",
                          "val imageBitmap = remember(result) { result.annotatedBitmap.asImageBitmap() }")

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(content)
