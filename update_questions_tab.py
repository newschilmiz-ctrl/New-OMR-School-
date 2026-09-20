import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

# 1. Add states to CreateQuestionPaperTabInternal
state_anchor = """    var isGenerating by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var jsonInput by remember { mutableStateOf("") }"""

state_new = """    var isGenerating by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var jsonInput by remember { mutableStateOf("") }
    
    // Paper Settings
    var showSettingsDialog by remember { mutableStateOf(false) }
    var headerText by remember { mutableStateOf("QUESTION PAPER") }
    var runByText by remember { mutableStateOf("") }
    var directorText by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf("★  ALL THE BEST  ★") }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }"""

content = content.replace(state_anchor, state_new)

# 2. Add dialog for settings and preview before the Column
dialog_anchor = """    if (showJsonDialog) {"""

dialog_new = """    if (showSettingsDialog) {
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
                    bitmap = previewBitmap!!.asImageBitmap(),
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

content = content.replace(dialog_anchor, dialog_new)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("Replaced states and dialogs")
