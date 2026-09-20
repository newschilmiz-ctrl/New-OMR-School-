import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

# Fix 1: asImageBitmap extension
# Use the full extension name: androidx.compose.ui.graphics.asImageBitmap
content = content.replace("androidx.compose.ui.graphics.asImageBitmap(bitmap)", "bitmap.asImageBitmap()")
if "import androidx.compose.ui.graphics.asImageBitmap" not in content:
    content = content.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.asImageBitmap")

# Fix 2: Icons
if "import androidx.compose.material.icons.filled.Settings" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Delete", "import androidx.compose.material.icons.filled.Delete\nimport androidx.compose.material.icons.filled.Settings\nimport androidx.compose.material.icons.filled.Visibility")

# Fix 3: `if` expression
content = content.replace('if (isGenerating && pendingAction == "downloadAnswerKey") "Key"', 'if (isGenerating && pendingAction == "downloadAnswerKey") "Key" else "Answer Key"')

# Fix 4: generateAnswerKeyPdf expecting Uri
content = content.replace('generateAnswerKeyPdf(context, exam, uri, viewModel)', 'if (uri != null) { generateAnswerKeyPdf(context, exam, uri, viewModel) }')
content = content.replace('if (uri != null) { if (uri != null) {', 'if (uri != null) {')

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("Compiler errors fixed")
