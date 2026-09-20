import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

old_paints = """        val footerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 16f
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.BLACK
        }"""

new_paints = """        val footerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 16f
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.BLACK
        }
        val watermarkPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 700f
            textAlign = Paint.Align.CENTER
        }"""

old_loop = """                // Border
                canvas.drawRect(20f, 20f, pageW - 20f, pageH - 20f, thinStroke)

                // Footer"""

new_loop = """                // Border
                canvas.drawRect(20f, 20f, pageW - 20f, pageH - 20f, thinStroke)

                // Watermark
                canvas.drawText(setName, pageW / 2f, pageH / 2f + 250f, watermarkPaint)

                // Footer"""

if old_paints in content and old_loop in content:
    content = content.replace(old_paints, new_paints)
    content = content.replace(old_loop, new_loop)
    with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Not found")
    print("old_paints in content:", old_paints in content)
    print("old_loop in content:", old_loop in content)
