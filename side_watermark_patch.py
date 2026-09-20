import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

old_paints = """        val watermarkPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 700f
            textAlign = Paint.Align.CENTER
        }"""

new_paints = """        val watermarkPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 700f
            textAlign = Paint.Align.CENTER
        }
        val verticalWatermarkPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#B0B0B0")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }"""

old_draw = """                // Watermark
                canvas.drawText(setName, pageW / 2f, pageH / 2f + 250f, watermarkPaint)"""

new_draw = """                // Watermark
                canvas.drawText(setName, pageW / 2f, pageH / 2f + 250f, watermarkPaint)

                // Vertical Watermarks
                canvas.save()
                canvas.translate(35f, pageH / 2f)
                canvas.rotate(-90f)
                canvas.drawText("MADE BY SUMIT SHARMA", 0f, 0f, verticalWatermarkPaint)
                canvas.restore()
                
                canvas.save()
                canvas.translate(pageW - 35f, pageH / 2f)
                canvas.rotate(90f)
                canvas.drawText("MADE BY SUMIT SHARMA", 0f, 0f, verticalWatermarkPaint)
                canvas.restore()"""

if old_paints in content and old_draw in content:
    content = content.replace(old_paints, new_paints)
    content = content.replace(old_draw, new_draw)
    with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Not found")
