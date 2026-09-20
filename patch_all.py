import os
import glob

def patch_file(filepath):
    with open(filepath, "r") as f:
        text = f.read()

    if "import com.example.ui.components.PremiumButton" not in text:
        text = text.replace("import androidx.compose.material3.*", "import androidx.compose.material3.*\nimport com.example.ui.components.PremiumButton\nimport com.example.ui.components.PremiumOutlinedButton")
    
    text = text.replace("OutlinedButton(", "PremiumOutlinedButton(")
    text = text.replace("Button(", "PremiumButton(")
    
    # Restore false positives
    text = text.replace("TextPremiumButton(", "TextButton(")
    text = text.replace("IconPremiumButton(", "IconButton(")
    text = text.replace("FloatingActionPremiumButton(", "FloatingActionButton(")
    text = text.replace("RadioPremiumButton(", "RadioButton(")
    
    # Fix parameter names
    text = text.replace("colors = ButtonDefaults.buttonColors(", "")
    text = text.replace("containerColor =", "backgroundColor =")
    
    with open(filepath, "w") as f:
        f.write(text)

files = [
    "app/src/main/java/com/example/ui/ExamDashboardScreen.kt",
    "app/src/main/java/com/example/ui/CreateExamScreen.kt",
    "app/src/main/java/com/example/ui/HomeScreen.kt",
    "app/src/main/java/com/example/ui/StudentAdmissionScreen.kt",
    "app/src/main/java/com/example/ui/StudentsScreen.kt",
]

for f in files:
    patch_file(f)
