import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

old_block = """                    val optY = qY + 22f
                    val optSpacing = 82f
                    val labels = listOf("A", "B", "C", "D")
                    
                    for (optIndex in 0..3) {
                        val optX = colX + 25f + (optIndex * optSpacing)
                        canvas.drawCircle(optX + 8f, optY - 4f, 8f, thinStroke)
                        canvas.drawText(labels[optIndex], optX + 8f, optY - 1f, optLetterPaint)
                        
                        val optStrText = when (optIndex) {
                            0 -> q.optionA.ifBlank { "Option" }
                            1 -> q.optionB.ifBlank { "Option" }
                            2 -> q.optionC.ifBlank { "Option" }
                            3 -> q.optionD.ifBlank { "Option" }
                            else -> "Option"
                        }
                        var displayStr = optStrText
                        if (displayStr.length > 12) displayStr = displayStr.substring(0, 10) + ".."
                        
                        canvas.drawText(displayStr, optX + 20f, optY, optPaint)
                    }"""

new_block = """                    val optY1 = qY + 20f
                    val optY2 = qY + 40f
                    val colSpacing = 160f
                    val labels = listOf("(a)", "(b)", "(c)", "(d)")
                    
                    for (optIndex in 0..3) {
                        val isOptCol2 = optIndex % 2 != 0
                        val isOptRow2 = optIndex >= 2
                        
                        val optX = colX + 25f + if (isOptCol2) colSpacing else 0f
                        val currentOptY = if (isOptRow2) optY2 else optY1
                        
                        canvas.drawText(labels[optIndex], optX, currentOptY, optPaint)
                        
                        val optStrText = when (optIndex) {
                            0 -> q.optionA.ifBlank { "Option" }
                            1 -> q.optionB.ifBlank { "Option" }
                            2 -> q.optionC.ifBlank { "Option" }
                            3 -> q.optionD.ifBlank { "Option" }
                            else -> "Option"
                        }
                        var displayStr = optStrText
                        if (displayStr.length > 18) displayStr = displayStr.substring(0, 16) + ".."
                        
                        canvas.drawText(displayStr, optX + 20f, currentOptY, optPaint)
                    }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Old block not found.")
