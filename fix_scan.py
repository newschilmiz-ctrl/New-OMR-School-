import re
with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    content = f.read()

content = content.replace("@Composable\n}\nfun ResultView", "}\n@Composable\nfun ResultView")

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(content)
