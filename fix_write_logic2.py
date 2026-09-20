import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

anchor = """        try {
            context.contentResolver.openOutputStream(uri!!)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Saved 10 Sets successfully", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
            }
        } finally {
            pdfDocument.close()
            (context as? android.app.Activity)?.runOnUiThread {
                onDone()
            }
        }"""

new = """        if (!isPreview && uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Saved 10 Sets successfully", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
                }
            } finally {
                pdfDocument.close()
                (context as? android.app.Activity)?.runOnUiThread {
                    onDone()
                }
            }
        } else {
            (context as? android.app.Activity)?.runOnUiThread {
                onDone()
            }
        }"""

if anchor in content:
    content = content.replace(anchor, new)
    with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
        f.write(content)
    print("Fixed write logic")
else:
    print("Anchor not found!")

