import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

# 1. Add asImageBitmap import
if "import androidx.compose.ui.graphics.asImageBitmap" not in content:
    content = content.replace("import androidx.compose.ui.text.font.FontWeight", "import androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.ui.graphics.asImageBitmap")

# 2. Fix Uri? issue by adding !!
content = content.replace('generateAnswerKeyPdf(context, exam, uri, viewModel)', 'generateAnswerKeyPdf(context, exam, uri!!, viewModel)')

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("Fixed!")
