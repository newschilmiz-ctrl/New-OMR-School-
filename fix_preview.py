import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

target = """                    val pdfDoc = android.graphics.pdf.PdfDocument()
                    generateQuestionPapersInternal(context, exam, questions, null, viewModel, headerText, runByText, directorText, addressText, true, pdfDoc) {
                        try {
                            val tempFile = java.io.File(context.cacheDir, "preview.pdf")
                            pdfDoc.writeTo(java.io.FileOutputStream(tempFile))
                            pdfDoc.close()
                            
                            val fd = android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                            val renderer = android.graphics.pdf.PdfRenderer(fd)
                            val page = renderer.openPage(0)
                            val bitmap = android.graphics.Bitmap.createBitmap(page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            renderer.close()
                            fd.close()
                            
                            previewBitmap = bitmap.asImageBitmap()
                            showPreviewDialog = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }"""

replacement = """                    val pdfDoc = android.graphics.pdf.PdfDocument()
                    generateQuestionPapersInternal(context, exam, questions, null, viewModel, headerText, runByText, directorText, addressText, true, pdfDoc) {
                        Thread {
                            try {
                                val tempFile = java.io.File(context.cacheDir, "preview.pdf")
                                pdfDoc.writeTo(java.io.FileOutputStream(tempFile))
                                pdfDoc.close()
                                
                                val fd = android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                                val renderer = android.graphics.pdf.PdfRenderer(fd)
                                val page = renderer.openPage(0)
                                val bitmap = android.graphics.Bitmap.createBitmap(page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                page.close()
                                renderer.close()
                                fd.close()
                                
                                val imageBmp = bitmap.asImageBitmap()
                                (context as? android.app.Activity)?.runOnUiThread {
                                    previewBitmap = imageBmp
                                    showPreviewDialog = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }.start()
                    }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
        f.write(content)
    print("Replaced preview logic successfully.")
else:
    print("Preview logic target not found.")

