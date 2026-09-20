import re

with open("app/src/main/java/com/example/util/OmrGenerator.kt", "r") as f:
    text = f.read()

old_logic = """
        } else {
            // Draw Roll No Bubbles (7 columns, 10 rows 0-9)
            val rStartX = 200f
            val rStartY = 140f
            val rSpacingX = 40f
            val rSpacingY = 25f
            val rBubbleRadius = 10f
            
            canvas.drawText("ROLL NUMBER", rStartX + 3 * rSpacingX, rStartY - 30f, titlePaint.apply { textSize = 20f; textAlign = Paint.Align.CENTER })
            
            for (col in 0 until 7) {
                // Draw Box for digit
                canvas.drawRect(rStartX + col * rSpacingX - 15f, rStartY - 20f, rStartX + col * rSpacingX + 15f, rStartY, thinStroke)
                
                for (row in 0..9) {
                    val cx = rStartX + col * rSpacingX
                    val cy = rStartY + 20f + row * rSpacingY
                    canvas.drawCircle(cx, cy, rBubbleRadius, thinStroke)
                    canvas.drawText(row.toString(), cx, cy + 4f, smallTextPaint)
                }
            }
            
            // Signature box on the right
            val sigX = 600f
            val sigY = 220f
            val sigW = 300f
            val sigH = 80f
            canvas.drawRect(sigX, sigY, sigX + sigW, sigY + sigH, thinStroke)
            canvas.drawText("परीक्षार्थी का पूरा हस्ताक्षर", sigX + sigW / 2f, sigY + sigH + 20f, titlePaint.apply { textSize = 16f; textAlign = Paint.Align.CENTER })
            canvas.drawText("(Full Signature of Candidate)", sigX + sigW / 2f, sigY + sigH + 40f, smallTextPaint.apply { textAlign = Paint.Align.CENTER })
        }
"""

new_logic = """
        } else {
            // Draw Roll No Bubbles (7 columns, 10 rows 0-9)
            val rStartX = 150f
            val rStartY = 120f
            val rSpacingX = 42f
            val rSpacingY = 28f
            val rBubbleRadius = 11f
            
            canvas.drawText("ROLL NUMBER", rStartX + 3 * rSpacingX, rStartY - 25f, titlePaint.apply { textSize = 20f; textAlign = Paint.Align.CENTER })
            
            for (col in 0 until 7) {
                // Draw Box for digit
                val cx = rStartX + col * rSpacingX
                canvas.drawRect(cx - 18f, rStartY - 18f, cx + 18f, rStartY + 2f, thinStroke)
                
                for (row in 0..9) {
                    val cy = rStartY + 28f + row * rSpacingY
                    canvas.drawCircle(cx, cy, rBubbleRadius, thinStroke)
                    canvas.drawText(row.toString(), cx, cy + 4f, smallTextPaint)
                }
            }
            
            // Signature box on the right
            val sigX = 650f
            val sigY = 220f
            val sigW = 250f
            val sigH = 80f
            canvas.drawRect(sigX, sigY, sigX + sigW, sigY + sigH, thinStroke)
            canvas.drawText("परीक्षार्थी का पूरा हस्ताक्षर", sigX + sigW / 2f, sigY + sigH + 25f, titlePaint.apply { textSize = 16f; textAlign = Paint.Align.CENTER })
            canvas.drawText("(Full Signature of Candidate)", sigX + sigW / 2f, sigY + sigH + 45f, smallTextPaint.apply { textAlign = Paint.Align.CENTER })
        }
"""

text = text.replace(old_logic.strip(), new_logic.strip())

with open("app/src/main/java/com/example/util/OmrGenerator.kt", "w") as f:
    f.write(text)

