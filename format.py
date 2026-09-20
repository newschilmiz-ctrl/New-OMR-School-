import re
with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    content = f.read()

count = 0
for i, line in enumerate(content.split("\n")):
    if "fun ResultView" in line:
        count = 1
        continue
    if count > 0:
        count += line.count("{") - line.count("}")
        if count == 0:
            print(f"ResultView closed at line {i+1}:\n{line}")
            break
