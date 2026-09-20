import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

anchor = """                            previewBitmap = bitmap
                            showPreviewDialog = true"""

new = """                            previewBitmap = androidx.compose.ui.graphics.asImageBitmap(bitmap)
                            showPreviewDialog = true"""

content = content.replace(anchor, new)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("Preview bitmap fixed")
