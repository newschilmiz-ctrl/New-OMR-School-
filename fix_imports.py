with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    content = f.read()

# Make sure imports are absolutely correct
if "import androidx.compose.ui.graphics.Color" not in content:
    content = content.replace("import androidx.compose.ui.graphics.asImageBitmap", "import androidx.compose.ui.graphics.asImageBitmap\nimport androidx.compose.ui.graphics.Color")

if "import androidx.compose.foundation.shape.CircleShape" not in content:
    content = content.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.shape.CircleShape")

# Also fix the weird ones I introduced
content = content.replace("import Color\n", "")
content = content.replace("import Color.White\n", "")
content = content.replace("import CircleShape\n", "")

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(content)
