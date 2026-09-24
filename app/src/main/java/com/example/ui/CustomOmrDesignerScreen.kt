package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.designer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomOmrDesignerScreen(navController: NavController, viewModel: OmrViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Canvas elements state
    var elements by remember { mutableStateOf(OmrTemplatePresets.createStandard50()) }
    var selectedElementId by remember { mutableStateOf<String?>(null) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showInspectorSheet by remember { mutableStateOf(false) }
    var showPresetsDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    // Canvas coordinate scale factors
    var canvasScreenWidthPx by remember { mutableFloatStateOf(1000f) }
    var canvasScreenHeightPx by remember { mutableFloatStateOf(1414f) }

    val selectedElement = elements.find { it.id == selectedElementId }

    // Rendered preview bitmap for export & clean preview
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Function to update element
    fun updateElement(updated: OmrElement) {
        elements = elements.map { if (it.id == updated.id) updated else it }
    }

    // Function to delete element
    fun deleteSelectedElement() {
        selectedElementId?.let { id ->
            elements = elements.filter { it.id != id }
            selectedElementId = null
            Toast.makeText(context, "Element removed", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to duplicate element
    fun duplicateSelectedElement() {
        selectedElement?.let { el ->
            val duplicated = el.copy(
                id = java.util.UUID.randomUUID().toString(),
                x = (el.x + 20f).coerceAtMost(900f),
                y = (el.y + 20f).coerceAtMost(1300f),
                zIndex = elements.maxOfOrNull { it.zIndex }?.plus(1) ?: 1
            )
            elements = elements + duplicated
            selectedElementId = duplicated.id
            Toast.makeText(context, "Element duplicated", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to center element horizontally
    fun centerSelectedElement() {
        selectedElement?.let { el ->
            val centeredX = ((1000f - el.width) / 2f).coerceAtLeast(20f)
            updateElement(el.copy(x = centeredX))
            Toast.makeText(context, "Aligned to Center", Toast.LENGTH_SHORT).show()
        }
    }

    // Trigger re-render whenever elements change
    LaunchedEffect(elements) {
        scope.launch(Dispatchers.Default) {
            val bmp = OmrSheetRenderer.renderToBitmap(elements)
            withContext(Dispatchers.Main) {
                previewBitmap = bmp
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "OMR Sheet Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            if (isPreviewMode) "Preview Mode (Clean A4)" else "Drag & Drop Canvas Designer",
                            fontSize = 10.sp,
                            color = if (isPreviewMode) Color(0xFF10B981) else Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(36.dp).testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                    }
                },
                actions = {
                    // Toggle Grid
                    IconButton(onClick = { showGrid = !showGrid }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                            contentDescription = "Grid",
                            tint = if (showGrid) Color(0xFF2563EB) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Presets / Templates
                    IconButton(onClick = { showPresetsDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DashboardCustomize, contentDescription = "Templates", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                    }

                    // Preview Toggle
                    IconButton(onClick = {
                        isPreviewMode = !isPreviewMode
                        if (isPreviewMode) selectedElementId = null
                    }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = "Preview",
                            tint = if (isPreviewMode) Color(0xFF10B981) else Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Print & Export
                    IconButton(onClick = {
                        OmrSheetRenderer.printOmrSheet(context, elements, "Custom_OMR_Sheet")
                    }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Print, contentDescription = "Print PDF", tint = Color(0xFFE11D48), modifier = Modifier.size(18.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // Contextual Inspector / Add Item Controls
            Surface(
                color = Color.White,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    if (selectedElement != null && !isPreviewMode) {
                        // Selected Element Quick Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFEEF2FF),
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(
                                        text = selectedElement.type.displayName,
                                        color = Color(0xFF4F46E5),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "X:${selectedElement.x.toInt()} Y:${selectedElement.y.toInt()} • ${selectedElement.width.toInt()}x${selectedElement.height.toInt()}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            // Quick Action Chips
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { centerSelectedElement() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.FormatAlignCenter, contentDescription = "Center", tint = Color(0xFF475569), modifier = Modifier.size(15.dp))
                                }
                                IconButton(
                                    onClick = { duplicateSelectedElement() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color(0xFF475569), modifier = Modifier.size(15.dp))
                                }
                                IconButton(
                                    onClick = { deleteSelectedElement() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                                }
                            }
                        }

                        // Customize button & Nudge arrows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { showInspectorSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Edit Properties", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Step fine-nudge buttons
                            Row(
                                modifier = Modifier
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                IconButton(
                                    onClick = { updateElement(selectedElement.copy(x = (selectedElement.x - 5f).coerceAtLeast(0f))) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Left", modifier = Modifier.size(15.dp))
                                }
                                IconButton(
                                    onClick = { updateElement(selectedElement.copy(x = (selectedElement.x + 5f).coerceAtMost(1000f - selectedElement.width))) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Right", modifier = Modifier.size(15.dp))
                                }
                                IconButton(
                                    onClick = { updateElement(selectedElement.copy(y = (selectedElement.y - 5f).coerceAtLeast(0f))) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", modifier = Modifier.size(15.dp))
                                }
                                IconButton(
                                    onClick = { updateElement(selectedElement.copy(y = (selectedElement.y + 5f).coerceAtMost(1414f - selectedElement.height))) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    } else {
                        // General Bottom Bar (When nothing is selected)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { showAddSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Add Items to Sheet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { OmrSheetRenderer.sharePdf(context, elements) },
                                modifier = Modifier.height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share PDF", fontSize = 11.5.sp, color = Color(0xFF0F172A))
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            // Scrollable Workspace Box holding the A4 Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // A4 Paper Aspect Ratio Sheet Container (1000 x 1414)
                BoxWithConstraints(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .aspectRatio(1000f / 1414f)
                        .shadow(12.dp, RoundedCornerShape(4.dp))
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                ) {
                    val canvasWidth = constraints.maxWidth.toFloat()
                    val canvasHeight = constraints.maxHeight.toFloat()
                    canvasScreenWidthPx = canvasWidth
                    canvasScreenHeightPx = canvasHeight

                    val scaleX = canvasWidth / 1000f
                    val scaleY = canvasHeight / 1414f

                    // 1. Draw High-Res Sheet Content Bitmap
                    if (previewBitmap != null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawImage(previewBitmap!!.asImageBitmap())
                        }
                    }

                    // 2. Alignment Grid Lines Overlay (Optional)
                    if (showGrid && !isPreviewMode) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val gridSpacingX = 50f * scaleX
                            val gridSpacingY = 50f * scaleY
                            val gridColor = Color.LightGray.copy(alpha = 0.35f)

                            var x = gridSpacingX
                            while (x < size.width) {
                                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                                x += gridSpacingX
                            }

                            var y = gridSpacingY
                            while (y < size.height) {
                                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                                y += gridSpacingY
                            }

                            // Center vertical guide
                            val midX = 500f * scaleX
                            drawLine(
                                Color(0xFFEF4444).copy(alpha = 0.4f),
                                Offset(midX, 0f),
                                Offset(midX, size.height),
                                1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }
                    }

                    // 3. Interactive Touch & Drag-and-Drop Overlay for every element
                    if (!isPreviewMode) {
                        // Canvas Tap Gesture to select or deselect
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val omrX = offset.x / scaleX
                                        val omrY = offset.y / scaleY

                                        // Find element touched (from top z-index downwards)
                                        val clicked = elements
                                            .sortedByDescending { it.zIndex }
                                            .find { el ->
                                                omrX >= el.x && omrX <= (el.x + el.width) &&
                                                        omrY >= el.y && omrY <= (el.y + el.height)
                                            }

                                        selectedElementId = clicked?.id
                                    }
                                }
                        )

                        // Render selection boundary box & drag handle on the selected element
                        selectedElement?.let { el ->
                            val density = LocalDensity.current
                            val leftPx = el.x * scaleX
                            val topPx = el.y * scaleY
                            val widthPx = el.width * scaleX
                            val heightPx = el.height * scaleY

                            val leftDp = with(density) { leftPx.toDp() }
                            val topDp = with(density) { topPx.toDp() }
                            val widthDp = with(density) { widthPx.toDp() }
                            val heightDp = with(density) { heightPx.toDp() }

                            Box(
                                modifier = Modifier
                                    .offset(x = leftDp, y = topDp)
                                    .size(widthDp, heightDp)
                                    .border(2.dp, Color(0xFF10B981), RoundedCornerShape(2.dp))
                                    .pointerInput(el.id) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            var newX = el.x + dragAmount.x / scaleX
                                            var newY = el.y + dragAmount.y / scaleY

                                            // Snap to center when near X=500
                                            val elMidX = newX + el.width / 2f
                                            if (Math.abs(elMidX - 500f) < 12f) {
                                                newX = 500f - el.width / 2f
                                            }

                                            // Boundary constraint inside 1000 x 1414
                                            newX = newX.coerceIn(0f, 1000f - el.width)
                                            newY = newY.coerceIn(0f, 1414f - el.height)

                                            updateElement(el.copy(x = newX, y = newY))
                                        }
                                    }
                            ) {
                                // Corner Drag Handle (Top-Left)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.TopStart)
                                        .offset((-6).dp, (-6).dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                        .border(1.5.dp, Color.White, CircleShape)
                                )

                                // Corner Drag Handle (Bottom-Right)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.BottomEnd)
                                        .offset(6.dp, 6.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                        .border(1.5.dp, Color.White, CircleShape)
                                )

                                // Drag indicator badge
                                Surface(
                                    color = Color(0xFF10B981),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = (-20).dp)
                                ) {
                                    Text(
                                        text = "DRAG TO MOVE",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEET: ADD NEW ITEMS ---
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Add Item to OMR Sheet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Choose a component to insert and customize on your sheet",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(10.dp))

                val addItems = listOf(
                    Triple(ElementType.CUSTOM_TEXT, "Custom Text / Label", "Header, note, board name, or custom instruction"),
                    Triple(ElementType.QUESTION_BLOCK, "Question Bubbles", "Custom questions (10 to 100), 3-5 options, 1-4 columns"),
                    Triple(ElementType.ROLL_NO_GRID, "Roll No / Student ID Grid", "Interactive bubble matrix for roll numbers"),
                    Triple(ElementType.STUDENT_INFO_BOX, "Student Details Box", "Table with Name, Roll, Subject, and Date fields"),
                    Triple(ElementType.INSTRUCTIONS_BOX, "Instructions Box", "Standard exam rules and bubble filling example"),
                    Triple(ElementType.BARCODE_QR, "QR Code / Barcode", "Unique exam ID or paper set identifier"),
                    Triple(ElementType.SIGNATURE_BOX, "Signatures Box", "Candidate and Invigilator signature spaces"),
                    Triple(ElementType.DIVIDER_LINE, "Horizontal Divider Line", "Separator line to divide sections"),
                    Triple(ElementType.REGISTRATION_MARKERS, "4 Corner Alignment Markers", "Essential markers for OpenCV scanner calibration")
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    addItems.forEach { (type, title, desc) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val newEl = when (type) {
                                        ElementType.CUSTOM_TEXT -> OmrElement(
                                            type = type,
                                            x = 100f,
                                            y = 100f,
                                            width = 800f,
                                            height = 40f,
                                            text = "EXAMINATION HEADER TEXT",
                                            fontSize = 20f,
                                            isBold = true
                                        )
                                        ElementType.QUESTION_BLOCK -> OmrElement(
                                            type = type,
                                            x = 80f,
                                            y = 500f,
                                            width = 840f,
                                            height = 650f,
                                            numQuestions = 30,
                                            numOptions = 4,
                                            numColumns = 2,
                                            hasBorder = true
                                        )
                                        ElementType.ROLL_NO_GRID -> OmrElement(
                                            type = type,
                                            x = 640f,
                                            y = 160f,
                                            width = 280f,
                                            height = 320f,
                                            digitsCount = 6,
                                            gridTitle = "ROLL NO."
                                        )
                                        ElementType.STUDENT_INFO_BOX -> OmrElement(
                                            type = type,
                                            x = 80f,
                                            y = 160f,
                                            width = 540f,
                                            height = 180f,
                                            hasBorder = true
                                        )
                                        ElementType.INSTRUCTIONS_BOX -> OmrElement(
                                            type = type,
                                            x = 80f,
                                            y = 360f,
                                            width = 540f,
                                            height = 130f,
                                            hasBorder = true
                                        )
                                        ElementType.BARCODE_QR -> OmrElement(
                                            type = type,
                                            x = 80f,
                                            y = 350f,
                                            width = 180f,
                                            height = 120f,
                                            barcodeFormat = "QR",
                                            barcodeContent = "EXAM-OMR-2026"
                                        )
                                        ElementType.SIGNATURE_BOX -> OmrElement(
                                            type = type,
                                            x = 80f,
                                            y = 1240f,
                                            width = 840f,
                                            height = 90f,
                                            hasBorder = true
                                        )
                                        ElementType.DIVIDER_LINE -> OmrElement(
                                            type = type,
                                            x = 80f,
                                            y = 150f,
                                            width = 840f,
                                            height = 2f
                                        )
                                        ElementType.REGISTRATION_MARKERS -> OmrElement(
                                            type = type,
                                            x = 0f,
                                            y = 0f,
                                            width = 1000f,
                                            height = 1414f,
                                            isLocked = true
                                        )
                                        else -> OmrElement(
                                            type = type,
                                            x = 100f,
                                            y = 100f,
                                            width = 500f,
                                            height = 50f,
                                            text = "New Element"
                                        )
                                    }

                                    elements = elements + newEl
                                    selectedElementId = newEl.id
                                    showAddSheet = false
                                    Toast.makeText(context, "$title Added! Drag to position.", Toast.LENGTH_SHORT).show()
                                },
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFE0E7FF), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFF4338CA), modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))
                                    Text(desc, fontSize = 10.5.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEET: FULL ELEMENT INSPECTOR & PROPERTY CUSTOMIZER ---
    if (showInspectorSheet && selectedElement != null) {
        ModalBottomSheet(
            onDismissRequest = { showInspectorSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Customize Element", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(selectedElement.type.displayName, fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Button(
                        onClick = { showInspectorSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

                // 1. Text & Content Settings (If text-applicable)
                if (selectedElement.type == ElementType.CUSTOM_TEXT ||
                    selectedElement.type == ElementType.HEADER_TITLE ||
                    selectedElement.type == ElementType.SUBTITLE ||
                    selectedElement.type == ElementType.INSTRUCTIONS_BOX
                ) {
                    Text("Text Content", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
                    OutlinedTextField(
                        value = selectedElement.text,
                        onValueChange = { updateElement(selectedElement.copy(text = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Font Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Font Size: ${selectedElement.fontSize.toInt()}sp", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { updateElement(selectedElement.copy(fontSize = (selectedElement.fontSize - 1f).coerceAtLeast(8f))) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { updateElement(selectedElement.copy(fontSize = (selectedElement.fontSize + 1f).coerceAtMost(60f))) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Slider(
                        value = selectedElement.fontSize,
                        onValueChange = { updateElement(selectedElement.copy(fontSize = it)) },
                        valueRange = 8f..60f
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Text Style: Bold & Alignment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedElement.isBold,
                            onClick = { updateElement(selectedElement.copy(isBold = !selectedElement.isBold)) },
                            label = { Text("Bold", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedElement.alignment == TextAlignment.LEFT,
                            onClick = { updateElement(selectedElement.copy(alignment = TextAlignment.LEFT)) },
                            label = { Text("Left", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedElement.alignment == TextAlignment.CENTER,
                            onClick = { updateElement(selectedElement.copy(alignment = TextAlignment.CENTER)) },
                            label = { Text("Center", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedElement.alignment == TextAlignment.RIGHT,
                            onClick = { updateElement(selectedElement.copy(alignment = TextAlignment.RIGHT)) },
                            label = { Text("Right", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Border toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Outer Border", fontSize = 11.5.sp)
                        Switch(
                            checked = selectedElement.hasBorder,
                            onCheckedChange = { updateElement(selectedElement.copy(hasBorder = it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 2. Question Block Settings
                if (selectedElement.type == ElementType.QUESTION_BLOCK) {
                    Text("Number of Questions: ${selectedElement.numQuestions}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = selectedElement.numQuestions.toFloat(),
                        onValueChange = { updateElement(selectedElement.copy(numQuestions = it.toInt())) },
                        valueRange = 5f..100f,
                        steps = 18
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Columns: ${selectedElement.numColumns}", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(1, 2, 3, 4).forEach { col ->
                                FilterChip(
                                    selected = selectedElement.numColumns == col,
                                    onClick = { updateElement(selectedElement.copy(numColumns = col)) },
                                    label = { Text("$col Col", fontSize = 10.5.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Options Per Question:", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(3, 4, 5).forEach { opt ->
                                FilterChip(
                                    selected = selectedElement.numOptions == opt,
                                    onClick = { updateElement(selectedElement.copy(numOptions = opt)) },
                                    label = { Text("$opt (A-${('A' + opt - 1)})", fontSize = 10.5.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bubble Size Slider
                    Text("Bubble Radius / Size: ${selectedElement.bubbleRadius.toInt()}px", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                    Slider(
                        value = selectedElement.bubbleRadius,
                        onValueChange = { updateElement(selectedElement.copy(bubbleRadius = it)) },
                        valueRange = 7f..16f
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Options label format
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Option Labels:", fontSize = 11.5.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedElement.optionsFormat == "ABCD",
                                onClick = { updateElement(selectedElement.copy(optionsFormat = "ABCD")) },
                                label = { Text("A, B, C, D", fontSize = 10.5.sp) }
                            )
                            FilterChip(
                                selected = selectedElement.optionsFormat == "1234",
                                onClick = { updateElement(selectedElement.copy(optionsFormat = "1234")) },
                                label = { Text("1, 2, 3, 4", fontSize = 10.5.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 3. Roll Number Grid Settings
                if (selectedElement.type == ElementType.ROLL_NO_GRID) {
                    Text("Roll Number Title", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = selectedElement.gridTitle,
                        onValueChange = { updateElement(selectedElement.copy(gridTitle = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Number of Digits: ${selectedElement.digitsCount}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = selectedElement.digitsCount.toFloat(),
                        onValueChange = { updateElement(selectedElement.copy(digitsCount = it.toInt())) },
                        valueRange = 4f..10f,
                        steps = 5
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 4. Barcode / QR Settings
                if (selectedElement.type == ElementType.BARCODE_QR) {
                    Text("Barcode / QR Content", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = selectedElement.barcodeContent,
                        onValueChange = { updateElement(selectedElement.copy(barcodeContent = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedElement.barcodeFormat == "QR",
                            onClick = { updateElement(selectedElement.copy(barcodeFormat = "QR")) },
                            label = { Text("QR Code", fontSize = 10.5.sp) }
                        )
                        FilterChip(
                            selected = selectedElement.barcodeFormat == "BARCODE",
                            onClick = { updateElement(selectedElement.copy(barcodeFormat = "BARCODE")) },
                            label = { Text("Code 128 Barcode", fontSize = 10.5.sp) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 5. Position & Dimension Fine-Tuning
                Text("Position & Dimensions (Canvas 1000 x 1414)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                Spacer(modifier = Modifier.height(6.dp))

                // Width
                Text("Width: ${selectedElement.width.toInt()}px", fontSize = 11.sp)
                Slider(
                    value = selectedElement.width,
                    onValueChange = { updateElement(selectedElement.copy(width = it)) },
                    valueRange = 50f..950f
                )

                // Height
                Text("Height: ${selectedElement.height.toInt()}px", fontSize = 11.sp)
                Slider(
                    value = selectedElement.height,
                    onValueChange = { updateElement(selectedElement.copy(height = it)) },
                    valueRange = 20f..1000f
                )

                // X Position
                Text("X Position: ${selectedElement.x.toInt()}px", fontSize = 11.sp)
                Slider(
                    value = selectedElement.x,
                    onValueChange = { updateElement(selectedElement.copy(x = it)) },
                    valueRange = 0f..900f
                )

                // Y Position
                Text("Y Position: ${selectedElement.y.toInt()}px", fontSize = 11.sp)
                Slider(
                    value = selectedElement.y,
                    onValueChange = { updateElement(selectedElement.copy(y = it)) },
                    valueRange = 0f..1350f
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Delete Button
                Button(
                    onClick = {
                        deleteSelectedElement()
                        showInspectorSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Delete Element", color = Color(0xFFDC2626), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // --- TEMPLATES / PRESETS DIALOG ---
    if (showPresetsDialog) {
        AlertDialog(
            onDismissRequest = { showPresetsDialog = false },
            title = {
                Text("Choose OMR Template Preset", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                elements = OmrTemplatePresets.createStandard50()
                                selectedElementId = null
                                showPresetsDialog = false
                                Toast.makeText(context, "Standard 50 Questions Loaded", Toast.LENGTH_SHORT).show()
                            },
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Standard 50 Questions", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Text("2 Columns of 25 • Roll Grid • Student Details • 4 Options (A-D)", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                elements = OmrTemplatePresets.createNeet100()
                                selectedElementId = null
                                showPresetsDialog = false
                                Toast.makeText(context, "NEET / Standard 100 Questions Loaded", Toast.LENGTH_SHORT).show()
                            },
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Standard / NEET 100 Questions", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Text("4 Columns of 25 • 7-Digit Roll Matrix • QR Code • Instructions", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                elements = OmrTemplatePresets.createQuick20()
                                selectedElementId = null
                                showPresetsDialog = false
                                Toast.makeText(context, "Quick 20 Questions Loaded", Toast.LENGTH_SHORT).show()
                            },
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Quick Quiz 20 Questions", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Text("Large extra-clear bubbles • Junior classes & single section tests", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                elements = listOf(
                                    OmrElement(
                                        type = ElementType.REGISTRATION_MARKERS,
                                        x = 0f,
                                        y = 0f,
                                        width = 1000f,
                                        height = 1414f,
                                        isLocked = true
                                    )
                                )
                                selectedElementId = null
                                showPresetsDialog = false
                                Toast.makeText(context, "Blank Canvas ready. Click '+ Add Items'!", Toast.LENGTH_SHORT).show()
                            },
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Start Blank Sheet", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFFBE123C))
                            Text("Empty canvas with 4 corner alignment markers", fontSize = 10.5.sp, color = Color(0xFF9F1239))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
