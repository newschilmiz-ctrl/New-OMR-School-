import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

buttons_anchor = """                Button(onClick = { pendingAction = "downloadAnswerKey"; createDocumentLauncher.launch("${exam.subject}_AnswerKey.pdf") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp)) {
                    Text(if (isGenerating && pendingAction == "downloadAnswerKey") "Generating..." else "Answer Key")
                }"""

buttons_new = """                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                Button(onClick = { pendingAction = "downloadAnswerKey"; createDocumentLauncher.launch("${exam.subject}_AnswerKey.pdf") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp)) {
                    Text(if (isGenerating && pendingAction == "downloadAnswerKey") "Key")
                }"""

content = content.replace(buttons_anchor, buttons_new)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("Replaced settings button")
