import re

with open("app/src/main/java/com/example/util/OmrGenerator.kt", "r") as f:
    text = f.read()

# Make templateType an argument
text = text.replace(
    'fun drawOmrToCanvas(context: android.content.Context, canvas: Canvas, numQuestions: Int, numOptions: Int, student: Student? = null, title: String = "बिहार विद्यालय परीक्षा , समिति", logoPath: String = "", logoOpacity: Float = 0.2f, logoSize: Float = 100f, logoPosition: String = "Left") {',
    'fun drawOmrToCanvas(context: android.content.Context, canvas: Canvas, numQuestions: Int, numOptions: Int, student: Student? = null, title: String = "बिहार विद्यालय परीक्षा , समिति", logoPath: String = "", logoOpacity: Float = 0.2f, logoSize: Float = 100f, logoPosition: String = "Left", templateType: String = "Standard") {'
)

# We want to conditionally skip details and barcodes if templateType == "RollNoOnly"
# and instead draw roll number bubbles.

# Find from `// OMR Serial - Removed as per user request` up to `// Set Code Instructions`
block_regex = r'(// Fields \(Left Column\).*?)(// Set Code Instructions)'

def replacement(match):
    original = match.group(1)
    
    roll_no_logic = """
        if (templateType == "Standard") {
            """ + original.replace('\n', '\n            ') + """
        } else {
            // Draw Roll No Bubbles (7 columns, 10 rows 0-9)
            val rStartX = 300f
            val rStartY = 200f
            val rSpacingX = 45f
            val rSpacingY = 30f
            val rBubbleRadius = 10f
            
            canvas.drawText("ROLL NUMBER", rStartX + 3 * rSpacingX, rStartY - 30f, titlePaint.apply { textSize = 20f })
            
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
        }
    """
    return roll_no_logic + match.group(2)

text = re.sub(block_regex, replacement, text, flags=re.DOTALL)

with open("app/src/main/java/com/example/util/OmrGenerator.kt", "w") as f:
    f.write(text)

