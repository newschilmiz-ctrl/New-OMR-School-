import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

# Replace maxDist
text = text.replace("snapToNearest(cx, cy, bubbleCenters, 30.0)", "snapToNearest(cx, cy, bubbleCenters, 14.0)")
text = text.replace("snapToNearest(cx, cy, bubbleCenters, 25.0)", "snapToNearest(cx, cy, bubbleCenters, 14.0)")

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)
print("maxDist updated")
