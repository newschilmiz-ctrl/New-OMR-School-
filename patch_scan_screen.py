import re

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    text = f.read()

text = text.replace("Button(", "PremiumButton(")
text = text.replace("TextPremiumButton(", "TextButton(")
text = text.replace("IconPremiumButton(", "IconButton(")
text = text.replace("FloatingActionPremiumButton(", "FloatingActionButton(")
text = text.replace("OutlinedPremiumButton(", "OutlinedButton(")

# Fix colors = ButtonDefaults.buttonColors(containerColor = ...)
# In PremiumButton we use backgroundColor = ...
text = text.replace("colors = ButtonDefaults.buttonColors(\n                    containerColor =", "backgroundColor =")
text = text.replace("colors = ButtonDefaults.buttonColors(\n                        containerColor =", "backgroundColor =")

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(text)

