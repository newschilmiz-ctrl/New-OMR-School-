import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

# 1. Insert states
state_new = """    var isGenerating by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    
    // Paper Settings
    var showSettingsDialog by remember { mutableStateOf(false) }
    var headerText by remember { mutableStateOf("QUESTION PAPER") }
    var runByText by remember { mutableStateOf("") }
    var directorText by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf("★  ALL THE BEST  ★") }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
"""

content = re.sub(r'    var isGenerating by remember \{ mutableStateOf\(false\) \}\s*var pendingAction by remember \{ mutableStateOf<String\?>\(null\) \}', state_new, content)

# 2. Also fix import for asImageBitmap and bitmap usages. Wait, previewBitmap is of type ImageBitmap directly.
# Let's see:
# previewBitmap = bitmap -> previewBitmap = bitmap.asImageBitmap()

# First, fix `previewBitmap` type in the existing file if it was added (it wasn't added because it failed)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("State inserted")
