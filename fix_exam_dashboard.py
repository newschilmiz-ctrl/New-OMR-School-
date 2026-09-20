import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

old_func = """private fun generateOmrPdf(context: Context, exam: Exam, students: List<com.example.data.Student>, gender: String, uri: android.net.Uri, onDone: () -> Unit) {
    Thread {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            for (student in students) {
                val pageInfo = PdfDocument.PageInfo.Builder(793, 1122, 1).create() // A4 at 96 dpi
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val bitmap = OmrGenerator.generateOmrBitmap(context, 100, 4, student, exam.title, exam.logoUrl, exam.logoOpacity, exam.logoSize, exam.logoPosition)
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                val destRect = Rect(0, 0, 793, 1122)
                canvas.drawBitmap(bitmap, srcRect, destRect, null)
                pdfDocument.finishPage(page)
            }
            context.contentResolver.openOutputStream(uri!!)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Saved $gender OMRs successfully", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
            }
        } finally {
            pdfDocument?.close()
            (context as? android.app.Activity)?.runOnUiThread {
                onDone()
            }
        }
    }.start()
}"""

new_func = """private fun generateOmrPdf(context: Context, exam: Exam, students: List<com.example.data.Student>, gender: String, uri: android.net.Uri, onDone: () -> Unit) {
    Thread {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            for (student in students) {
                // High quality OMR sheet by creating the page with exact OMR dimensions (1000x1414)
                val pageInfo = PdfDocument.PageInfo.Builder(com.example.util.OmrGenerator.SHEET_WIDTH, com.example.util.OmrGenerator.SHEET_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                
                // Draw OMR vector directly onto the PDF canvas for infinite scalability
                com.example.util.OmrGenerator.drawOmrToCanvas(
                    context = context, 
                    canvas = canvas, 
                    numQuestions = 100, 
                    numOptions = 4, 
                    student = student, 
                    title = exam.title, 
                    logoPath = exam.logoUrl, 
                    logoOpacity = exam.logoOpacity, 
                    logoSize = exam.logoSize, 
                    logoPosition = exam.logoPosition
                )
                
                pdfDocument.finishPage(page)
            }
            context.contentResolver.openOutputStream(uri!!)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Saved $gender OMRs successfully", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
            }
        } finally {
            pdfDocument?.close()
            (context as? android.app.Activity)?.runOnUiThread {
                onDone()
            }
        }
    }.start()
}"""

if old_func in content:
    content = content.replace(old_func, new_func)
    with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
        f.write(content)
    print("Replaced!")
else:
    print("Not found! Let's use regex.")
    pattern = re.compile(r"private fun generateOmrPdf\(context: Context, exam: Exam, students: List<com\.example\.data\.Student>, gender: String, uri: android\.net\.Uri, onDone: \(\) -> Unit\) \{.*?\n        \}\n    \}\.start\(\)\n\}", re.DOTALL)
    match = pattern.search(content)
    if match:
        content = content[:match.start()] + new_func + content[match.end():]
        with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
            f.write(content)
        print("Replaced with regex!")
    else:
        print("Still not found!")
