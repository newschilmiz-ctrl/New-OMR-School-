import re

with open("app/src/main/java/com/example/util/OmrGenerator.kt", "r") as f:
    content = f.read()

target = """        // Tear Line
        val dashPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }
        val tearY = 380f
        canvas.drawLine(60f, tearY, SHEET_WIDTH - 60f, tearY, dashPaint)
        canvas.drawText("यहाँ से फाड़ें", SHEET_WIDTH / 2f, tearY + 25f, smallTextPaint)"""

replacement = """        // Set Code Instructions
        val instrPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val textY = 410f + 40f
        val textX = (1000 - 80f - 150f - 20f) # marksBoxLeft - 20f
        canvas.drawText("निर्देश: परीक्षार्थी अपना प्रश्न पत्र सेट कोड नीचे (क्र. 10)", textX, textY, instrPaint)
        canvas.drawText("में अवश्य भरें, अन्यथा परिणाम अमान्य हो सकता है।", textX, textY + 30f, instrPaint)"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/util/OmrGenerator.kt", "w") as f:
        f.write(content)
    print("Replaced successfully.")
else:
    print("Target not found.")

