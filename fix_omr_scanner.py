import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

text = text.replace(
    'fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int): ScanResult {',
    'fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int, templateType: String = "Standard"): ScanResult {'
)

# Replace the student ID reading logic:
# Find:
#         // Read Student ID using ZXing
#         var studentId = "?"
#         try {
# ...
#             } catch (e2: Exception) {
#                 e2.printStackTrace()
#             }
#         }

block_regex = r'(// Read Student ID using ZXing[\s\S]*?\} catch \(e2: Exception\) \{\s*e2\.printStackTrace\(\)\s*\}\s*\})'

def replacement(match):
    zxing_logic = match.group(1)
    
    roll_no_logic = """
        var studentId = "?"
        if (templateType == "Standard") {
            """ + zxing_logic.replace('var studentId = "?"', '') + """
        } else {
            // Read Roll No Bubbles
            val rStartX = 300f
            val rStartY = 200f
            val rSpacingX = 45f
            val rSpacingY = 30f
            
            var rollNoStr = ""
            for (col in 0 until 7) {
                var bestDigit = -1
                var maxDarkness = 0f
                var secondMaxDarkness = 0f
                
                for (row in 0..9) {
                    val cx = rStartX + col * rSpacingX
                    val cy = rStartY + 20f + row * rSpacingY
                    val actual = mapVirtualToActual(cx, cy)
                    val darkness = sampleDarkness(bitmap, matrix, cx, cy, actual.first, actual.second, 8f)
                    
                    canvas.drawCircle(actual.first, actual.second, 10f, paintBlue)
                    
                    if (darkness > maxDarkness) {
                        secondMaxDarkness = maxDarkness
                        maxDarkness = darkness
                        bestDigit = row
                    } else if (darkness > secondMaxDarkness) {
                        secondMaxDarkness = darkness
                    }
                }
                
                if (maxDarkness > 0.25f && (maxDarkness - secondMaxDarkness) > 0.10f && bestDigit != -1) {
                    rollNoStr += bestDigit.toString()
                    val cx = rStartX + col * rSpacingX
                    val cy = rStartY + 20f + bestDigit * rSpacingY
                    val actual = mapVirtualToActual(cx, cy)
                    canvas.drawCircle(actual.first, actual.second, 12f, paintRed)
                } else {
                    rollNoStr += "?"
                }
            }
            studentId = rollNoStr
        }
    """
    return roll_no_logic

text = re.sub(block_regex, replacement, text)

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)

