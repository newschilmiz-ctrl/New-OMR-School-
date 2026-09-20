import re

with open("app/src/main/java/com/example/ui/LiveScanner.kt", "r") as f:
    text = f.read()

# Add captureRequested state
state_old = """
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isAnalyzing by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("Align OMR sheet within the frame") }
"""
state_new = """
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isAnalyzing by remember { mutableStateOf(false) }
    var captureRequested by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("Align OMR sheet and tap the button") }
"""
text = text.replace(state_old.strip(), state_new.strip())

# Update Analyzer logic
analyzer_old = """
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (isAnalyzing) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                
                                val bitmap = imageProxy.toBitmap()
"""
analyzer_new = """
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (!captureRequested || isAnalyzing) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                captureRequested = false
                                
                                val bitmap = imageProxy.toBitmap()
"""
text = text.replace(analyzer_old.strip(), analyzer_new.strip())

# Update Shutter button
button_old = """
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .size(72.dp)
                .background(Color.White, shape = CircleShape)
                .clickable { /* Shutter button action to be implemented later */ },
            contentAlignment = Alignment.Center
        ) {
            // White circle button
        }
"""
button_new = """
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .size(72.dp)
                .background(if (isAnalyzing) Color.Gray else Color.White, shape = CircleShape)
                .clickable(enabled = !isAnalyzing) { captureRequested = true },
            contentAlignment = Alignment.Center
        ) {
            // White circle button
        }
"""
text = text.replace(button_old.strip(), button_new.strip())

with open("app/src/main/java/com/example/ui/LiveScanner.kt", "w") as f:
    f.write(text)

