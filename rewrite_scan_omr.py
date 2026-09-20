with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if i < 83:
        new_lines.append(line)

new_code = """
                var startLiveScanner by remember { mutableStateOf(false) }
                if (startLiveScanner) {
                    LiveScanner(
                        numQuestions = numQuestions,
                        numOptions = numOptions,
                        onScanSuccess = { res ->
                            scanResult = res
                            startLiveScanner = false
                        },
                        onCancel = {
                            startLiveScanner = false
                        }
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ready to scan", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = {
                            startLiveScanner = true
                        }) {
                            Text("Open Live Scanner")
                        }
                    }
                }
            }
        }
    }
}
"""

new_lines.append(new_code)

for i, line in enumerate(lines):
    if i >= 154:
        new_lines.append(line)

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.writelines(new_lines)
