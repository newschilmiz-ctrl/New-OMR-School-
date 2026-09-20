import glob
import re

files = glob.glob("app/src/main/java/com/example/ui/**/*.kt", recursive=True)

for filepath in files:
    with open(filepath, "r") as f:
        text = f.read()

    # The issue is `containerColor = ... )`
    # We replaced `colors = ButtonDefaults.buttonColors(\n                    containerColor =`
    # Let's just fix `containerColor = if (autoSaved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary\n                )\n            ) {`
    text = text.replace("containerColor = if (autoSaved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary\n                )\n            ) {", "containerColor = if (autoSaved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary\n            ) {")

    with open(filepath, "w") as f:
        f.write(text)

