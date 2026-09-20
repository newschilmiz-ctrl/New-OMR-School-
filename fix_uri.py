import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

content = content.replace("context.contentResolver.openOutputStream(uri)?.use { outputStream ->", "context.contentResolver.openOutputStream(uri!!)?.use { outputStream ->")

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("Uri fixed")
