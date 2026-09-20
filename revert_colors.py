import os
import glob

files = glob.glob("app/src/main/java/com/example/ui/**/*.kt", recursive=True)

for filepath in files:
    with open(filepath, "r") as f:
        text = f.read()

    text = text.replace("backgroundColor =", "containerColor =")

    # Fix PremiumButton colors syntax
    # Before we replaced `colors = ButtonDefaults.buttonColors(` with ``
    # This means `containerColor = ...)` might be hanging.
    # Actually, `Button` didn't have `colors = ButtonDefaults.buttonColors(containerColor = ...)`
    # Oh wait, `PremiumButton` just takes `containerColor = ...` natively. So it's fine!
    
    with open(filepath, "w") as f:
        f.write(text)
