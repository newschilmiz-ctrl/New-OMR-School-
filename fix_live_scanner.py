import re

with open("app/src/main/java/com/example/ui/LiveScanner.kt", "r") as f:
    text = f.read()

# Add onGloballyPositioned import
if "import androidx.compose.ui.layout.onGloballyPositioned" not in text:
    text = text.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.layout.onGloballyPositioned")

# Add preview variables
preview_vars = """
    var isProcessing by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("Align sheet and press shutter") }
    
    var previewWidth by remember { mutableIntStateOf(1080) }
    var previewHeight by remember { mutableIntStateOf(1920) }
    val density = LocalContext.current.resources.displayMetrics.density
"""
text = text.replace("    var isProcessing by remember { mutableStateOf(false) }\n    var scanStatus by remember { mutableStateOf(\"Align sheet and press shutter\") }", preview_vars.strip())

# Add onGloballyPositioned to AndroidView
view_old = """
                modifier = Modifier.fillMaxSize()
            )

            // Overlays
"""
view_new = """
                modifier = Modifier.fillMaxSize().onGloballyPositioned {
                    previewWidth = it.size.width
                    previewHeight = it.size.height
                }
            )

            // Overlays
"""
text = text.replace(view_old.strip(), view_new.strip())

# Update capture logic
capture_old = """
                                    val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                                    scope.launch(Dispatchers.Default) {
                                        try {
                                            scanStatus = "Processing Image..."
                                            // Process Image with new Perspective Transform
                                            val result = OmrScanner.scanAdvanced(rotatedBitmap, numQuestions, numOptions)
"""

capture_new = """
                                    val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                                    scope.launch(Dispatchers.Default) {
                                        try {
                                            scanStatus = "Processing Image..."
                                            
                                            // 1. Calculate the exact crop area based on the blue boxes on screen
                                            val screenWidth = previewWidth.toFloat()
                                            val screenHeight = previewHeight.toFloat()
                                            
                                            val imgWidth = rotatedBitmap.width.toFloat()
                                            val imgHeight = rotatedBitmap.height.toFloat()
                                            
                                            // PreviewView ScaleType.FILL_CENTER logic
                                            val scale = Math.max(screenWidth / imgWidth, screenHeight / imgHeight)
                                            val dispWidth = imgWidth * scale
                                            val dispHeight = imgHeight * scale
                                            val leftOffset = (screenWidth - dispWidth) / 2f
                                            val topOffset = (screenHeight - dispHeight) / 2f
                                            
                                            // Margins used for blue boxes (in pixels)
                                            val marginX = 24f * density
                                            val marginY = 80f * density
                                            val bottomMargin = 160f * density
                                            
                                            val screenX1 = marginX
                                            val screenY1 = marginY
                                            val screenX2 = screenWidth - marginX
                                            val screenY2 = screenHeight - bottomMargin
                                            
                                            // Map screen coordinates back to image coordinates
                                            val imgX1 = ((screenX1 - leftOffset) / scale).toInt().coerceIn(0, imgWidth.toInt())
                                            val imgY1 = ((screenY1 - topOffset) / scale).toInt().coerceIn(0, imgHeight.toInt())
                                            val imgX2 = ((screenX2 - leftOffset) / scale).toInt().coerceIn(0, imgWidth.toInt())
                                            val imgY2 = ((screenY2 - topOffset) / scale).toInt().coerceIn(0, imgHeight.toInt())
                                            
                                            val cropW = (imgX2 - imgX1).coerceAtLeast(1)
                                            val cropH = (imgY2 - imgY1).coerceAtLeast(1)
                                            
                                            // Crop the image to the exact bounding box
                                            val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, imgX1, imgY1, cropW, cropH)
                                            
                                            // Scale to standard A4 size expected by the scanner
                                            val a4Bitmap = Bitmap.createScaledBitmap(croppedBitmap, 800, 1131, true)

                                            // Process Image directly
                                            val result = OmrScanner.scan(a4Bitmap, numQuestions, numOptions, "Standard")
"""
text = text.replace(capture_old.strip(), capture_new.strip())

with open("app/src/main/java/com/example/ui/LiveScanner.kt", "w") as f:
    f.write(text)

