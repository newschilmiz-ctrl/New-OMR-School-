import os
import glob

files = glob.glob("app/src/main/java/com/example/ui/**/*.kt", recursive=True)

for filepath in files:
    with open(filepath, "r") as f:
        text = f.read()

    text = text.replace("PremiumOutlinedPremiumButton", "PremiumOutlinedButton")

    with open(filepath, "w") as f:
        f.write(text)
