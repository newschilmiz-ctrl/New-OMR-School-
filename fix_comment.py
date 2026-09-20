import re

with open("app/src/main/java/com/example/util/OmrGenerator.kt", "r") as f:
    content = f.read()

content = content.replace("val textX = (1000 - 80f - 150f - 20f) # marksBoxLeft - 20f", "val textX = (1000 - 80f - 150f - 20f) // marksBoxLeft - 20f")

with open("app/src/main/java/com/example/util/OmrGenerator.kt", "w") as f:
    f.write(content)

