import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

old_block = """                    val q = shuffledQuestions[i]
                    val qNum = "${i + 1}."
                    canvas.drawText(qNum, colX, qY, qTextPaint)
                    
                    val qStr = if (q.text.isBlank()) "......................................................................... ?" else q.text
                    canvas.drawText(qStr, colX + 25f, qY, qDotsPaint)
                    
                    val optY1 = qY + 20f
                    val optY2 = qY + 40f
                    val colSpacing = 160f
                    val labels = listOf("(a)", "(b)", "(c)", "(d)")"""

new_block = """                    val q = shuffledQuestions[i]
                    val qNum = "${i + 1}."
                    canvas.drawText(qNum, colX, qY, qTextPaint)
                    
                    var text = q.text
                    if (text.isBlank()) text = "......................................................................... ?"
                    
                    val maxW = 320f
                    var line1 = text
                    var line2 = ""
                    
                    val breakIndex = qDotsPaint.breakText(text, true, maxW, null)
                    if (breakIndex < text.length) {
                        val spaceIdx = text.lastIndexOf(' ', breakIndex)
                        val splitIdx = if (spaceIdx > 0) spaceIdx else breakIndex
                        line1 = text.substring(0, splitIdx)
                        line2 = text.substring(splitIdx).trim()
                        
                        val b2 = qDotsPaint.breakText(line2, true, maxW, null)
                        if (b2 < line2.length) {
                            line2 = line2.substring(0, b2) + "..."
                        }
                    }
                    
                    val isTwoLines = line2.isNotEmpty()
                    val optOffset = if (isTwoLines) 14f else 0f
                    
                    canvas.drawText(line1, colX + 25f, qY, qDotsPaint)
                    if (isTwoLines) {
                        canvas.drawText(line2, colX + 25f, qY + 14f, qDotsPaint)
                    }
                    
                    val optY1 = qY + 17f + optOffset
                    val optY2 = qY + 34f + optOffset
                    val colSpacing = 160f
                    val labels = listOf("(a)", "(b)", "(c)", "(d)")"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Old block not found. Checking if file has variations...")
