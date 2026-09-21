package com.example.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ui.components.PremiumButton
import com.example.ui.components.PremiumOutlinedButton
import com.example.util.OmrGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExamScreen(navController: NavController, viewModel: OmrViewModel) {
    val context = LocalContext.current
    var examName by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Mathematics") }
    var examDate by remember { mutableStateOf("") }
    var examTitle by remember { mutableStateOf("ACADEMIC EVALUATION TEST") }
    
    var logoPath by remember { mutableStateOf("") }
    var logoOpacity by remember { mutableStateOf(0.15f) }
    var logoSize by remember { mutableStateOf(100f) }
    var logoPosition by remember { mutableStateOf("Center") }
    
    var marksPerQuestion by remember { mutableStateOf("1") }
    var negativeMarks by remember { mutableStateOf("0") }
    var passMarks by remember { mutableStateOf("30") }
    var bonusMarks by remember { mutableStateOf("0") }
    var templateType by remember { mutableStateOf("Standard") }

    val availableSubjects = listOf("Mathematics", "Science", "Physics", "Chemistry", "Biology", "English", "Social Studies")

    val students by viewModel.students.collectAsStateWithLifecycle()
    val mappedStudents = remember(students, selectedSubject) {
        students.filter { it.subjects.contains(selectedSubject, ignoreCase = true) }
    }

    var datePickerVisible by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (datePickerVisible) {
        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        examDate = sdf.format(Date(millis))
                    }
                    datePickerVisible = false
                }) { Text("Confirm", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { datePickerVisible = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.filesDir, "logo_${System.currentTimeMillis()}.png")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                logoPath = file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var previewVisible by remember { mutableStateOf(false) }
    if (previewVisible) {
        Dialog(onDismissRequest = { previewVisible = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OMR Sheet Preview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        IconButton(onClick = { previewVisible = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }

                    var previewBmp by remember { mutableStateOf<Bitmap?>(null) }
                    
                    LaunchedEffect(examTitle, logoPath, logoOpacity, logoSize, logoPosition, templateType) {
                        withContext(Dispatchers.IO) {
                            val bmp = OmrGenerator.generateOmrBitmap(context, 100, 4, null, examTitle, logoPath, logoOpacity, logoSize, logoPosition, templateType)
                            previewBmp = bmp
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBmp != null) {
                            Image(
                                bitmap = previewBmp!!.asImageBitmap(),
                                contentDescription = "OMR Preview",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator(color = Color(0xFFE11D48))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    PremiumButton(
                        onClick = { previewVisible = false },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color(0xFF0F172A),
                        borderColor = Color(0xFF0F172A)
                    ) {
                        Text("Close Preview")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create New Exam",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFAFBFD)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. BASIC EXAM INFO CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("1. Basic Information", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = examName,
                        onValueChange = { examName = it },
                        label = { Text("Exam Name (e.g. Mid-Term Physics 2026)", fontSize = 11.5.sp) },
                        textStyle = TextStyle(fontSize = 12.5.sp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date Picker Input
                    Box(modifier = Modifier.fillMaxWidth().clickable { datePickerVisible = true }) {
                        OutlinedTextField(
                            value = examDate,
                            onValueChange = {},
                            label = { Text("Exam Date (DD/MM/YYYY) *", fontSize = 11.5.sp) },
                            textStyle = TextStyle(fontSize = 12.5.sp),
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            trailingIcon = { Icon(Icons.Outlined.CalendarToday, contentDescription = "Select Date", tint = Color(0xFF64748B), modifier = Modifier.size(17.dp)) },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color(0xFF0F172A),
                                disabledContainerColor = Color(0xFFF8FAFC),
                                disabledBorderColor = Color(0xFFE2E8F0),
                                disabledLabelColor = Color(0xFF64748B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Subject Selector Chips
                    Text("Subject", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(5.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableSubjects.chunked(3).forEach { rowSubjects ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowSubjects.forEach { subj ->
                                    val isSelected = selectedSubject == subj
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(7.dp))
                                            .clickable { selectedSubject = subj },
                                        shape = RoundedCornerShape(7.dp),
                                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = subj,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. OMR SHEET BRANDING & HEADER
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("2. OMR Sheet Branding", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = examTitle,
                        onValueChange = { examTitle = it },
                        label = { Text("Header Title (Printed on OMR)", fontSize = 11.5.sp) },
                        textStyle = TextStyle(fontSize = 12.5.sp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Logo selector row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (logoPath.isEmpty()) "Institution Watermark" else "Logo Selected",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (logoPath.isEmpty()) "Optional logo on sheet" else "Watermark active",
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        PremiumOutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(if (logoPath.isEmpty()) "Upload" else "Change", fontSize = 10.5.sp)
                        }
                    }

                    if (logoPath.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Watermark Opacity: ${(logoOpacity * 100).toInt()}%", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                        Slider(
                            value = logoOpacity,
                            onValueChange = { logoOpacity = it },
                            valueRange = 0.05f..0.8f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFE11D48),
                                activeTrackColor = Color(0xFFE11D48)
                            )
                        )

                        Text("Watermark Position", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Left", "Center", "Right").forEach { pos ->
                                val isSelected = logoPosition == pos
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { logoPosition = pos },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = pos,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. SCORING SCHEME CARD (2x2 GRID)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("3. Evaluation & Marking Rules", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = marksPerQuestion,
                            onValueChange = { marksPerQuestion = it },
                            label = { Text("Marks/Q", fontSize = 11.sp) },
                            textStyle = TextStyle(fontSize = 12.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = Color(0xFF0F172A)
                            )
                        )
                        OutlinedTextField(
                            value = negativeMarks,
                            onValueChange = { negativeMarks = it },
                            label = { Text("Negative", fontSize = 11.sp) },
                            textStyle = TextStyle(fontSize = 12.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = Color(0xFF0F172A)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = passMarks,
                            onValueChange = { passMarks = it },
                            label = { Text("Pass Marks", fontSize = 11.sp) },
                            textStyle = TextStyle(fontSize = 12.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = Color(0xFF0F172A)
                            )
                        )
                        OutlinedTextField(
                            value = bonusMarks,
                            onValueChange = { bonusMarks = it },
                            label = { Text("Bonus Marks", fontSize = 11.sp) },
                            textStyle = TextStyle(fontSize = 12.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = Color(0xFF0F172A)
                            )
                        )
                    }
                }
            }

            // 4. TEMPLATE SELECTION (Standard vs Simple)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("4. OMR Sheet Style", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Standard Template Option
                        val isStandard = templateType == "Standard"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { templateType = "Standard" },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isStandard) Color(0xFFFFF1F2) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isStandard) Color(0xFFE11D48) else Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isStandard,
                                        onClick = { templateType = "Standard" },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Standard", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = if (isStandard) Color(0xFFE11D48) else Color(0xFF0F172A))
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Student Details + QR code verification", fontSize = 9.sp, color = Color(0xFF64748B))
                            }
                        }

                        // RollNoOnly Template Option
                        val isSimple = templateType == "RollNoOnly"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { templateType = "RollNoOnly" },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSimple) Color(0xFFFFF1F2) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isSimple) Color(0xFFE11D48) else Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSimple,
                                        onClick = { templateType = "RollNoOnly" },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simple", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = if (isSimple) Color(0xFFE11D48) else Color(0xFF0F172A))
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Minimal layout with Roll bubbles only", fontSize = 9.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }

            // 5. AUTOMATIC MAPPING CALLOUT
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "${mappedStudents.size} Students Enrolled in $selectedSubject",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF15803D)
                        )
                        Text(
                            "They will be automatically assigned and personalized OMR answer sheets will be generated.",
                            fontSize = 10.sp,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }

            // 6. ACTION BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PremiumOutlinedButton(
                    onClick = { previewVisible = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview", fontSize = 12.sp)
                }

                PremiumButton(
                    onClick = {
                        val mk = marksPerQuestion.toFloatOrNull() ?: 1f
                        val neg = negativeMarks.toFloatOrNull() ?: 0f
                        val ps = passMarks.toFloatOrNull() ?: 30f
                        val bns = bonusMarks.toFloatOrNull() ?: 0f

                        viewModel.createExam(
                            examName, selectedSubject, examDate, examTitle,
                            logoPath, logoOpacity, logoSize, logoPosition,
                            mk, neg, ps, bns, templateType
                        ) { examId ->
                            navController.popBackStack()
                            navController.navigate(Screen.ExamDashboard.createRoute(examId))
                        }
                    },
                    modifier = Modifier.weight(1.5f),
                    enabled = examName.isNotBlank() && examDate.isNotBlank(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    containerColor = Color(0xFFE11D48),
                    borderColor = Color(0xFFBE123C)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Create Exam", fontSize = 12.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
