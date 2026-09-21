package com.example.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.Exam
import com.example.ui.components.PremiumButton
import com.example.ui.components.PremiumOutlinedButton
import com.example.util.OmrScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanOmrScreen(navController: NavController, viewModel: OmrViewModel, examId: Int) {
    var exam by remember { mutableStateOf<Exam?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<OmrScanner.ScanResult?>(null) }
    var rawCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var manualRotationDegrees by remember { mutableIntStateOf(0) }
    var currentSensitivityMultiplier by remember { mutableFloatStateOf(1.0f) }
    var showCameraXScanner by remember { mutableStateOf(false) }
    val students by viewModel.students.collectAsStateWithLifecycle()

    LaunchedEffect(examId) {
        exam = viewModel.getExamById(examId)
    }

    val numQuestions = 100
    val numOptions = 4

    fun evaluateOmrSheet(bitmap: Bitmap, rotation: Int, sensitivity: Float) {
        isProcessing = true
        coroutineScope.launch(Dispatchers.Default) {
            try {
                val res = OmrScanner.scan(
                    bitmap = bitmap,
                    numQuestions = numQuestions,
                    numOptions = numOptions,
                    templateType = exam?.templateType ?: "Standard",
                    manualRotation = rotation,
                    sensitivityMultiplier = sensitivity
                )
                withContext(Dispatchers.Main) {
                    scanResult = res
                    isProcessing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Scanning error: ${e.message}", Toast.LENGTH_SHORT).show()
                    isProcessing = false
                }
            }
        }
    }

    var startLiveScanner by remember { mutableStateOf(false) }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()
    }

    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val resultData = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
            resultData?.pages?.firstOrNull()?.imageUri?.let { uri ->
                isProcessing = true
                coroutineScope.launch(Dispatchers.Default) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()

                        if (bitmap != null) {
                            withContext(Dispatchers.Main) {
                                rawCapturedBitmap = bitmap
                                manualRotationDegrees = 0
                            }
                            evaluateOmrSheet(bitmap, 0, currentSensitivityMultiplier)
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Failed to load OMR image", Toast.LENGTH_SHORT).show()
                                isProcessing = false
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Scanning error: ${e.message}", Toast.LENGTH_SHORT).show()
                            isProcessing = false
                        }
                    }
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            coroutineScope.launch(Dispatchers.Default) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap != null) {
                        withContext(Dispatchers.Main) {
                            rawCapturedBitmap = bitmap
                            manualRotationDegrees = 0
                        }
                        evaluateOmrSheet(bitmap, 0, currentSensitivityMultiplier)
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Could not decode selected image", Toast.LENGTH_SHORT).show()
                            isProcessing = false
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        isProcessing = false
                    }
                }
            }
        }
    }

    if (startLiveScanner) {
        LaunchedEffect(Unit) {
            scanner.getStartScanIntent(context as Activity)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    startLiveScanner = false
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Scanner start error: ${e.message}", Toast.LENGTH_SHORT).show()
                    startLiveScanner = false
                }
        }
    }

    if (showCameraXScanner) {
        LiveScanner(
            numQuestions = numQuestions,
            numOptions = numOptions,
            onScanSuccess = { res ->
                scanResult = res
                showCameraXScanner = false
            },
            onCancel = { showCameraXScanner = false },
            onImageCaptured = { bmp, res ->
                rawCapturedBitmap = bmp
                manualRotationDegrees = 0
                scanResult = res
                showCameraXScanner = false
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Live OMR Scanner",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        exam?.let {
                            Text(
                                it.name,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFAFBFD)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isProcessing) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.padding(24.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFFE11D48), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                "Evaluating OMR Sheet...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Detecting corner markers, bubbles & Roll ID",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else if (scanResult != null && exam != null) {
                ResultView(
                    result = scanResult!!,
                    exam = exam!!,
                    viewModel = viewModel,
                    rawBitmap = rawCapturedBitmap,
                    currentRotation = manualRotationDegrees,
                    currentSensitivity = currentSensitivityMultiplier,
                    onRotate = { newRot ->
                        manualRotationDegrees = newRot
                        rawCapturedBitmap?.let { bmp ->
                            evaluateOmrSheet(bmp, newRot, currentSensitivityMultiplier)
                        }
                    },
                    onSensitivityChange = { newSens ->
                        currentSensitivityMultiplier = newSens
                        rawCapturedBitmap?.let { bmp ->
                            evaluateOmrSheet(bmp, manualRotationDegrees, newSens)
                        }
                    },
                    onDismiss = { navController.popBackStack() },
                    onRescan = {
                        scanResult = null
                        rawCapturedBitmap = null
                        manualRotationDegrees = 0
                    }
                )
            } else {
                // Pre-scan State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Scanner Illustration / Viewport Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF1F2))
                                    .border(2.dp, Color(0xFFFFE4E6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = Color(0xFFE11D48)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Instant OMR Auto-Scoring",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Evaluates 100 questions in under 1 second with automatic perspective correction & roll number extraction.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Best Practice Checklist Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Tips for Highest Accuracy",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Hold phone parallel to the paper sheet", fontSize = 12.sp, color = Color(0xFF334155))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Ensure all 4 black corner boxes are in frame", fontSize = 12.sp, color = Color(0xFF334155))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Auto de-skew works even if image is tilted or rotated", fontSize = 12.sp, color = Color(0xFF334155))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Scanner Action Buttons (Full Advanced Suite)
                    PremiumButton(
                        onClick = { showCameraXScanner = true },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color(0xFFE11D48),
                        borderColor = Color(0xFFBE123C)
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Live A4 Camera Scanner (Fast & Auto)")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    PremiumOutlinedButton(
                        onClick = { startLiveScanner = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Document Edge-Detect Scanner")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    PremiumOutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick Sheet Image from Gallery")
                    }
                }
            }
        }
    }
}

@Composable
fun ResultView(
    result: OmrScanner.ScanResult,
    exam: Exam,
    viewModel: OmrViewModel,
    rawBitmap: Bitmap?,
    currentRotation: Int,
    currentSensitivity: Float,
    onRotate: (Int) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onRescan: () -> Unit
) {
    var editedStudentId by remember(result) { mutableStateOf(result.studentId) }
    var editedPaperSet by remember(result) { mutableStateOf(result.paperSet) }
    var showFullPreviewDialog by remember { mutableStateOf(false) }

    var key by remember { mutableStateOf<com.example.data.AnswerKey?>(null) }
    var keyLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(editedPaperSet) {
        keyLoaded = false
        key = viewModel.getAnswerKeyForExamAndSet(exam.id, editedPaperSet)
        keyLoaded = true
    }

    if (!keyLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFE11D48))
        }
        return
    }

    val students by viewModel.students.collectAsStateWithLifecycle()
    val studentName = students.find { it.rollNo == editedStudentId }?.name ?: "Unregistered Candidate"
    val isRollMatch = students.any { it.rollNo == editedStudentId }

    var showEditRollNo by remember { mutableStateOf(false) }
    var showEditSet by remember { mutableStateOf(false) }

    if (showEditRollNo) {
        var tempRoll by remember { mutableStateOf(editedStudentId) }
        AlertDialog(
            onDismissRequest = { showEditRollNo = false },
            title = { Text("Edit Student Roll No", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempRoll,
                    onValueChange = { tempRoll = it },
                    label = { Text("Roll No") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editedStudentId = tempRoll
                    showEditRollNo = false
                }) { Text("Save", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditRollNo = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditSet) {
        var tempSet by remember { mutableStateOf(editedPaperSet) }
        AlertDialog(
            onDismissRequest = { showEditSet = false },
            title = { Text("Edit Paper Set Code", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempSet,
                    onValueChange = { tempSet = it.uppercase() },
                    label = { Text("Set (A, B, C, D)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editedPaperSet = tempSet
                    showEditSet = false
                }) { Text("Save", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditSet = false }) { Text("Cancel") }
            }
        )
    }

    if (key == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        if (editedPaperSet == "?") "Paper Set Not Detected" else "Set $editedPaperSet Answer Key Missing",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Please verify the paper set code or create the answer key for this set first.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumOutlinedButton(
                        onClick = { showEditSet = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enter Paper Set Manually")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumButton(
                        onClick = onRescan,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color(0xFF0F172A),
                        borderColor = Color(0xFF0F172A)
                    ) {
                        Text("Rescan Paper")
                    }
                }
            }
        }
        return
    }

    val correctAnswersList = viewModel.converters.toList(key!!.correctAnswers)
    var editedAnswers by remember(result) { mutableStateOf(result.answers.toList()) }
    var showEditDialogForQ by remember { mutableStateOf<Int?>(null) }

    var correct = 0
    var wrong = 0
    var empty = 0
    val statuses = mutableListOf<Int>() // 1 = correct, 0 = wrong, -1 = empty

    for (i in 0 until key!!.numQuestions) {
        val studentAns = if (i < editedAnswers.size) editedAnswers[i] else -1
        val correctAns = if (i < correctAnswersList.size) correctAnswersList[i] else -1

        if (studentAns == -1) {
            empty++
            statuses.add(-1)
        } else if (studentAns == correctAns) {
            correct++
            statuses.add(1)
        } else {
            wrong++
            statuses.add(0)
        }
    }

    val attempted = key!!.numQuestions - empty
    val rawScore = correct * (exam.marksPerQuestion) - (attempted - correct) * (exam.negativeMarks) + (exam.bonusMarks)
    val score = Math.max(0f, rawScore)
    val isPassed = score >= exam.passMarks

    var autoSaved by remember { mutableStateOf(false) }

    LaunchedEffect(result, key, editedStudentId, editedPaperSet, editedAnswers) {
        if (!autoSaved && key != null && isRollMatch && editedPaperSet != "?") {
            viewModel.saveScanResult(exam.id, editedStudentId, editedPaperSet, score, key!!.numQuestions, editedAnswers, statuses) {
                autoSaved = true
            }
        }
    }

    if (showEditDialogForQ != null) {
        val qIndex = showEditDialogForQ!!
        AlertDialog(
            onDismissRequest = { showEditDialogForQ = null },
            title = { Text("Manual Override: Question #${qIndex + 1}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text("Select actual student bubble mark:", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(12.dp))
                    val options = listOf("A", "B", "C", "D", "Blank / Unattempted")
                    val currentAns = if (qIndex < editedAnswers.size) editedAnswers[qIndex] else -1
                    options.forEachIndexed { optIndex, optText ->
                        val value = if (optText.startsWith("Blank")) -1 else optIndex
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newAnswers = editedAnswers.toMutableList()
                                    while (newAnswers.size <= qIndex) newAnswers.add(-1)
                                    newAnswers[qIndex] = value
                                    editedAnswers = newAnswers
                                    autoSaved = false
                                    showEditDialogForQ = null
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = currentAns == value,
                                onClick = {
                                    val newAnswers = editedAnswers.toMutableList()
                                    while (newAnswers.size <= qIndex) newAnswers.add(-1)
                                    newAnswers[qIndex] = value
                                    editedAnswers = newAnswers
                                    autoSaved = false
                                    showEditDialogForQ = null
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(optText, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditDialogForQ = null }) { Text("Cancel") }
            }
        )
    }

    val context = LocalContext.current
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null) {
                Thread {
                    try {
                        val pdfDocument = PdfDocument()
                        val bitmap = result.annotatedBitmap
                        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                        val page = pdfDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(page)

                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            pdfDocument.writeTo(out)
                        }
                        pdfDocument.close()
                        (context as? Activity)?.runOnUiThread {
                            Toast.makeText(context, "Scored OMR PDF saved!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        (context as? Activity)?.runOnUiThread {
                            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. SCORE HERO CARD
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("FINAL EVALUATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "%.1f".format(score),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPassed) Color(0xFF15803D) else Color(0xFFE11D48)
                            )
                            Text(
                                text = " / ${key!!.numQuestions * exam.marksPerQuestion}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                            )
                        }
                    }

                    // Pass/Fail badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isPassed) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = if (isPassed) "PASSED" else "NEEDS IMPROVEMENT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPassed) Color(0xFF15803D) else Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Student Identification Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(studentName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showEditRollNo = true }
                        ) {
                            Text(
                                "Roll: $editedStudentId",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isRollMatch) Color(0xFF0284C7) else Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                        }
                    }

                    // Paper Set pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.clickable { showEditSet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SET $editedPaperSet", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats breakdown strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = Color(0xFFF0FDF4)) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CORRECT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            Text("$correct", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                        }
                    }
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = Color(0xFFFEF2F2)) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WRONG", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            Text("$wrong", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626))
                        }
                    }
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = Color(0xFFF8FAFC)) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BLANK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text("$empty", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF475569))
                        }
                    }
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = Color(0xFFF0F9FF)) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOTAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            Text("${key!!.numQuestions}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0369A1))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. QUESTION MATRIX & MANUAL OVERRIDE
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Question Analysis", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("Tap to edit bubble", fontSize = 11.sp, color = Color(0xFF64748B))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color legend
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF16A34A)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Correct", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFDC2626)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Incorrect", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFCBD5E1)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unattempted", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Question bubble chips grid
                val chunkedQuestions = (0 until key!!.numQuestions).chunked(8)
                for (rowQuestions in chunkedQuestions) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in rowQuestions) {
                            val status = statuses.getOrNull(i) ?: -1
                            val chipBg = when (status) {
                                1 -> Color(0xFF16A34A)
                                0 -> Color(0xFFDC2626)
                                else -> Color(0xFFE2E8F0)
                            }
                            val chipText = if (status == -1) Color(0xFF475569) else Color.White

                            Box(
                                modifier = Modifier
                                    .padding(vertical = 3.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(chipBg)
                                    .clickable { showEditDialogForQ = i },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${i + 1}",
                                    color = chipText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. SCANNED OMR IMAGE PREVIEW & ADVANCED ALIGNMENT TOOLBAR
        if (showFullPreviewDialog) {
            Dialog(
                onDismissRequest = { showFullPreviewDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = result.annotatedBitmap.asImageBitmap(),
                            contentDescription = "Full High-Res Scanned Sheet",
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { showFullPreviewDialog = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(20.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Scanned Sheet & Calibrated Bubbles", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text(
                            "Rotation: ${currentRotation}° ${if (currentRotation == 180) "(Flipped 180°)" else ""}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    OutlinedButton(
                        onClick = { showFullPreviewDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Inspect", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .clickable { showFullPreviewDialog = true }
                ) {
                    Image(
                        bitmap = result.annotatedBitmap.asImageBitmap(),
                        contentDescription = "Annotated OMR Scan",
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tap to Zoom", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ADVANCED ORIENTATION & CORRECTION TOOLBAR
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Sheet Orientation Fix (उल्टा / सीधा)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    "Agar paper ulta scan hua hai, turant 1-tap me seedha karein:",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Primary 180° Invert button
                            Button(
                                onClick = {
                                    if (rawBitmap != null) {
                                        val newRot = (currentRotation + 180) % 360
                                        onRotate(newRot)
                                    }
                                },
                                modifier = Modifier.weight(1.3f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD97706),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Flip 180° (उल्टा/सीधा)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Rotate +90°
                            OutlinedButton(
                                onClick = {
                                    if (rawBitmap != null) {
                                        val newRot = (currentRotation + 90) % 360
                                        onRotate(newRot)
                                    }
                                },
                                modifier = Modifier.weight(0.85f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("+90° ↻", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Rotate -90°
                            OutlinedButton(
                                onClick = {
                                    if (rawBitmap != null) {
                                        val newRot = ((currentRotation - 90) % 360 + 360) % 360
                                        onRotate(newRot)
                                    }
                                },
                                modifier = Modifier.weight(0.85f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("-90° ↺", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sensitivity Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bubble Fill Sensitivity:", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF78350F))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = currentSensitivity < 0.9f,
                                    onClick = { onSensitivityChange(0.7f) },
                                    label = { Text("Light Pencil", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = currentSensitivity in 0.9f..1.1f,
                                    onClick = { onSensitivityChange(1.0f) },
                                    label = { Text("Pen (Normal)", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = currentSensitivity > 1.1f,
                                    onClick = { onSensitivityChange(1.35f) },
                                    label = { Text("Strict", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. ACTION BUTTONS
        PremiumButton(
            onClick = {
                if (!autoSaved) {
                    viewModel.saveScanResult(exam.id, editedStudentId, editedPaperSet, score, key!!.numQuestions, editedAnswers, statuses) {
                        onDismiss()
                    }
                } else {
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color(0xFFE11D48),
            borderColor = Color(0xFFBE123C)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (autoSaved) "Done (Saved)" else "Save & Finish")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PremiumOutlinedButton(
                onClick = { savePdfLauncher.launch("Scanned_OMR_${editedStudentId}.pdf") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export PDF")
            }

            PremiumOutlinedButton(
                onClick = onRescan,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Next")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
