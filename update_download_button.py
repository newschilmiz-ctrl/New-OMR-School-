import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

bottom_anchor = """        Button(
            onClick = {
                if (questions.isEmpty()) {
                    Toast.makeText(context, "Add at least 1 question", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                pendingAction = "generatePapers"
                createDocumentLauncher.launch("${exam.subject}_QuestionPapers.pdf")
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(56.dp),
            enabled = !isGenerating && questions.isNotEmpty(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(if (isGenerating) "Generating..." else "Download Question Papers")
        }"""

bottom_new = """        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (questions.isEmpty()) {
                        Toast.makeText(context, "Add at least 1 question", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val pdfDoc = android.graphics.pdf.PdfDocument()
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
                            
                            previewBitmap = bitmap
                            showPreviewDialog = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = !isGenerating && questions.isNotEmpty(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Preview")
            }
            
            Button(
                onClick = {
                    if (questions.isEmpty()) {
                        Toast.makeText(context, "Add at least 1 question", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    pendingAction = "generatePapers"
                    createDocumentLauncher.launch("${exam.subject}_QuestionPapers.pdf")
                },
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = !isGenerating && questions.isNotEmpty(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Download")
            }
        }"""

content = content.replace(bottom_anchor, bottom_new)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("Replaced bottom buttons")
