import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

anchor = """        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Saved 10 Sets successfully", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        pdfDocument.close()
        
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onDone()
        }
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
            }
            pdfDocument.close()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onDone()
            }
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onDone()
            }
        }
    }
}"""

if anchor in content:
    content = content.replace(anchor, new)
    with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
        f.write(content)
    print("Fixed write logic")
else:
    print("Not found anchor! Using regex...")
    import re
    # More robust replacement
    pattern = re.compile(r'        try \{\s*context\.contentResolver\.openOutputStream\(uri\)\?\.use \{ outputStream ->\s*pdfDocument\.writeTo\(outputStream\)\s*\}\s*\(context as\? android\.app\.Activity\)\?\.runOnUiThread \{\s*Toast\.makeText\(context, "Saved 10 Sets successfully", Toast\.LENGTH_LONG\)\.show\(\)\s*\}\s*\} catch \(e: Exception\) \{\s*e\.printStackTrace\(\)\s*\}\s*pdfDocument\.close\(\)\s*android\.os\.Handler\(android\.os\.Looper\.getMainLooper\(\)\)\.post \{\s*onDone\(\)\s*\}\s*\}\s*\}')
    content = pattern.sub(new, content)
    with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
        f.write(content)
    print("Replaced with regex")

