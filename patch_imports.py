with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    content = f.read()

# Add clickable import
if "import androidx.compose.foundation.clickable" not in content:
    content = content.replace("import androidx.compose.foundation.background", "import androidx.compose.foundation.background\nimport androidx.compose.foundation.clickable")

if "import androidx.compose.foundation.shape.CircleShape" not in content:
    content = content.replace("import androidx.compose.foundation.background", "import androidx.compose.foundation.background\nimport androidx.compose.foundation.shape.CircleShape")

content = content.replace("androidx.compose.material3.AlertDialog", "AlertDialog")
content = content.replace("androidx.compose.material3.RadioButton", "RadioButton")
content = content.replace("androidx.compose.material3.TextButton", "TextButton")
content = content.replace("androidx.compose.foundation.shape.CircleShape", "CircleShape")
content = content.replace("androidx.compose.ui.graphics.Color.White", "Color.White")
content = content.replace("androidx.compose.ui.graphics.Color", "Color")

content = content.replace(".androidx.compose.foundation.clickable", ".clickable")

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(content)
