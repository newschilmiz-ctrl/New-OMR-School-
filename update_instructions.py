import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

target = """                    canvas.drawText("INSTRUCTIONS : ", 60f, 185f, instrBoldPaint)
                    canvas.drawText("Each question carries 1 mark. Choose the correct option (A / B / C / D) and", 185f, 185f, instrRegularPaint)
                    canvas.drawText("mark your answer on the OMR sheet.", 185f, 205f, instrRegularPaint)"""

replacement = """                    canvas.drawText("निर्देश : ", 60f, 185f, instrBoldPaint)
                    canvas.drawText("प्रत्येक प्रश्न 1 अंक का है। सही विकल्प (A / B / C / D) चुनें और", 185f, 185f, instrRegularPaint)
                    canvas.drawText("अपना उत्तर OMR शीट पर अंकित करें।", 185f, 205f, instrRegularPaint)"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
        f.write(content)
    print("Replaced successfully.")
else:
    print("Target not found.")

