import re

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    content = f.read()

target = """        // Show Annotated Image
        val imageBitmap = remember(result) { result.annotatedBitmap.asImageBitmap() }
        if (imageBitmap != null) {
            Text("Scanned Sheet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.Image(
                bitmap = imageBitmap,
                contentDescription = "Annotated OMR",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(imageBitmap.width.toFloat() / imageBitmap.height.toFloat())
            )
            Spacer(modifier = Modifier.height(16.dp))
        }"""

content = content.replace(target, "")

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(content)
