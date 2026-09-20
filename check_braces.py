with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    text = f.read()

count = 0
for char in text:
    if char == '{': count += 1
    elif char == '}': count -= 1
print("Brace count:", count)
