import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

# 1. Remove the old Run By and Director from the top
top_anchor = """                    if (runByText.isNotBlank()) {
                        canvas.drawText("Run By: $runByText", 40f, 25f, boldSmallPaint.apply { textAlign = android.graphics.Paint.Align.LEFT })
                    }
                    if (directorText.isNotBlank()) {
                        canvas.drawText("Director: $directorText", pageW - 40f, 25f, boldSmallPaint.apply { textAlign = android.graphics.Paint.Align.RIGHT })
                    }
                    boldSmallPaint.textAlign = android.graphics.Paint.Align.LEFT"""

content = content.replace(top_anchor, "")

# 2. Insert them before Instructions Box, and shift Instructions Box down
instr_anchor = """                    // Instructions Box
                    val instrRect = android.graphics.RectF(40f, 150f, pageW - 40f, 210f)
                    canvas.drawRoundRect(instrRect, 5f, 5f, thinStroke)
                    canvas.drawText("INSTRUCTIONS : ", 60f, 175f, instrBoldPaint)
                    canvas.drawText("Each question carries 1 mark. Choose the correct option (A / B / C / D) and", 185f, 175f, instrRegularPaint)
                    canvas.drawText("mark your answer on the OMR sheet.", 185f, 195f, instrRegularPaint)

                    // Center Divider
                    canvas.drawLine(pageW / 2f, 220f, pageW / 2f, pageH - 80f, thinStroke)"""

instr_new = """                    if (runByText.isNotBlank()) {
                        canvas.drawText("Run By: $runByText", 40f, 150f, boldSmallPaint.apply { textAlign = android.graphics.Paint.Align.LEFT })
                    }
                    if (directorText.isNotBlank()) {
                        canvas.drawText("Director: $directorText", pageW - 40f, 150f, boldSmallPaint.apply { textAlign = android.graphics.Paint.Align.RIGHT })
                    }
                    boldSmallPaint.textAlign = android.graphics.Paint.Align.LEFT

                    // Instructions Box
                    val instrRect = android.graphics.RectF(40f, 160f, pageW - 40f, 220f)
                    canvas.drawRoundRect(instrRect, 5f, 5f, thinStroke)
                    canvas.drawText("INSTRUCTIONS : ", 60f, 185f, instrBoldPaint)
                    canvas.drawText("Each question carries 1 mark. Choose the correct option (A / B / C / D) and", 185f, 185f, instrRegularPaint)
                    canvas.drawText("mark your answer on the OMR sheet.", 185f, 205f, instrRegularPaint)

                    // Center Divider
                    canvas.drawLine(pageW / 2f, 230f, pageW / 2f, pageH - 80f, thinStroke)"""

content = content.replace(instr_anchor, instr_new)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("Fixed positions")
