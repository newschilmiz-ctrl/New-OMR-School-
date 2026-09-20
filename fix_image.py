import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

content = content.replace("bitmap = previewBitmap!!.asImageBitmap(),", "bitmap = previewBitmap!!,")

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("Fixed Image composable")
