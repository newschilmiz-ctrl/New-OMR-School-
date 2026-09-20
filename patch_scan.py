with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    text = f.read()

if "import com.example.ui.components.PremiumButton" not in text:
    text = text.replace("import androidx.compose.material3.*", "import androidx.compose.material3.*\nimport com.example.ui.components.PremiumButton\nimport com.example.ui.components.PremiumOutlinedButton")

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(text)
