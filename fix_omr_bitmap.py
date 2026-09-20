import re

with open("app/src/main/java/com/example/util/OmrGenerator.kt", "r") as f:
    text = f.read()

text = text.replace(
    'logoPosition: String = "Left"): Bitmap {',
    'logoPosition: String = "Left", templateType: String = "Standard"): Bitmap {'
)

text = text.replace(
    'drawOmrToCanvas(context, canvas, numQuestions, numOptions, student, title, logoPath, logoOpacity, logoSize, logoPosition)',
    'drawOmrToCanvas(context, canvas, numQuestions, numOptions, student, title, logoPath, logoOpacity, logoSize, logoPosition, templateType)'
)

with open("app/src/main/java/com/example/util/OmrGenerator.kt", "w") as f:
    f.write(text)

