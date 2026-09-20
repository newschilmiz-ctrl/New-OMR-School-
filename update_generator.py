import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

setup_anchor = """    Thread {
        val sets = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        val pdfDocument = PdfDocument()"""

setup_new = """    Thread {
        val sets = if (isPreview) listOf("A") else listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        val pdfDocument = providedPdfDoc ?: PdfDocument()"""

content = content.replace(setup_anchor, setup_new)

# footer replacement
footer_anchor = """                // Footer
                canvas.drawLine(40f, pageH - 80f, pageW - 40f, pageH - 80f, thinStroke)
                canvas.drawText("★  ALL THE BEST  ★", pageW / 2f, pageH - 45f, footerPaint)"""

footer_new = """                // Footer
                canvas.drawLine(40f, pageH - 80f, pageW - 40f, pageH - 80f, thinStroke)
                canvas.drawText(addressText.ifBlank { "★  ALL THE BEST  ★" }, pageW / 2f, pageH - 45f, footerPaint)"""

content = content.replace(footer_anchor, footer_new)

# top center replacement
top_anchor = """                    // Top Center
                    canvas.drawText("QUESTION PAPER", pageW / 2f, 65f, titlePaint)
                    canvas.drawLine(pageW / 2f - 180f, 85f, pageW / 2f - 20f, 85f, thickStroke)
                    canvas.drawLine(pageW / 2f + 20f, 85f, pageW / 2f + 180f, 85f, thickStroke)"""

top_new = """                    // Top Center
                    canvas.drawText(headerText.ifBlank { "QUESTION PAPER" }, pageW / 2f, 65f, titlePaint)
                    canvas.drawLine(pageW / 2f - 180f, 85f, pageW / 2f - 20f, 85f, thickStroke)
                    canvas.drawLine(pageW / 2f + 20f, 85f, pageW / 2f + 180f, 85f, thickStroke)
                    
                    if (runByText.isNotBlank()) {
                        canvas.drawText("Run By: $runByText", 40f, 25f, boldSmallPaint.apply { textAlign = android.graphics.Paint.Align.LEFT })
                    }
                    if (directorText.isNotBlank()) {
                        canvas.drawText("Director: $directorText", pageW - 40f, 25f, boldSmallPaint.apply { textAlign = android.graphics.Paint.Align.RIGHT })
                    }
                    boldSmallPaint.textAlign = android.graphics.Paint.Align.LEFT"""

content = content.replace(top_anchor, top_new)

# write logic replacement
write_anchor = """        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        pdfDocument.close()
        
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onDone()
            Toast.makeText(context, "Question Papers Exported", Toast.LENGTH_LONG).show()
        }"""

write_new = """        if (!isPreview && uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            pdfDocument.close()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onDone()
                Toast.makeText(context, "Question Papers Exported", Toast.LENGTH_LONG).show()
            }
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onDone()
            }
        }"""

content = content.replace(write_anchor, write_new)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("Updated generator drawing logic")
