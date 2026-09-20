import re

with open("app/src/main/java/com/example/ui/LiveScanner.kt", "r") as f:
    text = f.read()

old_logic = """
        // Overlay Guide
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val frameWidth = canvasWidth * 0.8f
            val frameHeight = frameWidth * 1.414f // A4 ratio
            
            val left = (canvasWidth - frameWidth) / 2f
            val top = (canvasHeight - frameHeight) / 2f
            
            // Draw dark background outside frame
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, 0f),
                size = Size(canvasWidth, top)
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, top + frameHeight),
                size = Size(canvasWidth, canvasHeight - (top + frameHeight))
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, top),
                size = Size(left, frameHeight)
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(left + frameWidth, top),
                size = Size(canvasWidth - (left + frameWidth), frameHeight)
            )

            // Draw frame border
            drawRect(
                color = Color.Green,
                topLeft = Offset(left, top),
                size = Size(frameWidth, frameHeight),
                style = Stroke(width = 4.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = scanStatus,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCancel) {
                Text("Cancel")
            }
        }
"""

new_logic = """
        // Overlay Guide
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val frameWidth = canvasWidth * 0.8f
            val frameHeight = frameWidth * 1.414f // A4 ratio
            
            val left = (canvasWidth - frameWidth) / 2f
            val top = (canvasHeight - frameHeight) / 2f
            
            val boxSize = 80.dp.toPx()
            val strokeWidth = 4.dp.toPx()
            val blueColor = Color.Blue
            
            // Top Left Box
            drawRect(
                color = blueColor,
                topLeft = Offset(left, top),
                size = Size(boxSize, boxSize),
                style = Stroke(width = strokeWidth)
            )
            
            // Top Right Box
            drawRect(
                color = blueColor,
                topLeft = Offset(left + frameWidth - boxSize, top),
                size = Size(boxSize, boxSize),
                style = Stroke(width = strokeWidth)
            )
            
            // Bottom Left Box
            drawRect(
                color = blueColor,
                topLeft = Offset(left, top + frameHeight - boxSize),
                size = Size(boxSize, boxSize),
                style = Stroke(width = strokeWidth)
            )
            
            // Bottom Right Box
            drawRect(
                color = blueColor,
                topLeft = Offset(left + frameWidth - boxSize, top + frameHeight - boxSize),
                size = Size(boxSize, boxSize),
                style = Stroke(width = strokeWidth)
            )
        }

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
        
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = scanStatus,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCancel) {
                Text("Cancel")
            }
        }
"""

text = text.replace(old_logic.strip(), new_logic.strip())

# Need to add CircleShape and background import if not present
if "import androidx.compose.foundation.shape.CircleShape" not in text:
    text = text.replace("import androidx.compose.foundation.shape.RoundedCornerShape", "import androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.foundation.shape.CircleShape\nimport androidx.compose.foundation.background\nimport androidx.compose.foundation.clickable")

with open("app/src/main/java/com/example/ui/LiveScanner.kt", "w") as f:
    f.write(text)

