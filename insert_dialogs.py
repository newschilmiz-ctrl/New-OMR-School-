import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

anchor = """    if (showJsonDialog) {"""

dialogs = """    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Question Paper Settings") },
            text = {
                Column {
                    OutlinedTextField(
                        value = headerText,
                        onValueChange = { headerText = it },
                        label = { Text("Heading (e.g. QUESTION PAPER)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = runByText,
                        onValueChange = { runByText = it },
                        label = { Text("Run By") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = directorText,
                        onValueChange = { directorText = it },
                        label = { Text("Director") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addressText,
                        onValueChange = { addressText = it },
                        label = { Text("Bottom Address / Footer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("OK") }
            }
        )
    }

    if (showPreviewDialog && previewBitmap != null) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Paper Preview") },
            text = {
                androidx.compose.foundation.Image(
                    bitmap = previewBitmap!!,
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxWidth().aspectRatio(previewBitmap!!.width.toFloat() / previewBitmap!!.height.toFloat())
                )
            },
            confirmButton = {
                TextButton(onClick = { showPreviewDialog = false }) { Text("Close") }
            }
        )
    }

    if (showJsonDialog) {"""

content = content.replace(anchor, dialogs)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("Dialogs inserted")
