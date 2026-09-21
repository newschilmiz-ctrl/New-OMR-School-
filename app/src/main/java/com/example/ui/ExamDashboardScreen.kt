package com.example.ui
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import com.example.ui.components.PremiumButton
import com.example.ui.components.PremiumOutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.navigation.NavController
import com.example.data.Exam
import com.example.data.QuestionEntity
import com.example.util.OmrGenerator
import java.io.File
import java.io.FileOutputStream



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDashboardScreen(navController: NavController, viewModel: OmrViewModel, examId: Int, initialTab: Int = 0) {
    var exam by remember { mutableStateOf<Exam?>(null) }
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    
    LaunchedEffect(examId) {
        exam = viewModel.getExamById(examId)
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                exam?.name ?: "Exam Dashboard",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            if (exam != null) {
                                Text(
                                    "${exam!!.subject} • Pass: ${exam!!.passMarks.toInt()} Marks",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                        }
                    },
                    actions = {
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        var showEditDialog by remember { mutableStateOf(false) }
                        
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("Delete Exam", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                                text = { Text("Are you sure you want to delete this exam? All questions, answer keys, and scan results will be permanently removed.", fontSize = 12.sp) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.deleteExam(examId) {
                                            navController.popBackStack()
                                        }
                                    }) { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", fontSize = 12.sp) }
                                }
                            )
                        }

                        if (showEditDialog && exam != null) {
                            var editName by remember { mutableStateOf(exam!!.name) }
                            var editSubject by remember { mutableStateOf(exam!!.subject) }
                            AlertDialog(
                                onDismissRequest = { showEditDialog = false },
                                title = { Text("Edit Exam Details", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = editName,
                                            onValueChange = { editName = it },
                                            label = { Text("Exam Name", fontSize = 11.sp) },
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = editSubject,
                                            onValueChange = { editSubject = it },
                                            label = { Text("Subject", fontSize = 11.sp) },
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.updateExam(exam!!.copy(name = editName, subject = editSubject)) {
                                            showEditDialog = false
                                            exam = exam!!.copy(name = editName, subject = editSubject)
                                        }
                                    }) { Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showEditDialog = false }) { Text("Cancel", fontSize = 12.sp) }
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .clickable { showEditDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Exam", tint = Color(0xFF0F172A), modifier = Modifier.size(15.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2))
                                .clickable { showDeleteDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Exam", tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
            }
        },
        containerColor = Color(0xFFFAFBFD)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFFE11D48),
                edgePadding = 6.dp,
                divider = { HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp) },
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFE11D48),
                            height = 2.5.dp
                        )
                    }
                }
            ) {
                val tabs = listOf(
                    Triple("Generate", Icons.Default.Download, 0),
                    Triple("Questions", Icons.Default.Assignment, 1),
                    Triple("Scanner", Icons.Default.CameraAlt, 2),
                    Triple("Exam Day", Icons.Default.Event, 3),
                    Triple("Reports", Icons.Default.Assessment, 4)
                )
                tabs.forEach { (label, icon, index) ->
                    val isSelected = selectedTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                label,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFFE11D48) else Color(0xFF64748B)
                            )
                        },
                        icon = {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (isSelected) Color(0xFFE11D48) else Color(0xFF94A3B8)
                            )
                        }
                    )
                }
            }
            when (selectedTab) {
                0 -> GenerateTab(navController, viewModel, examId, exam)
                1 -> if (exam != null) CreateQuestionPaperTabInternal(viewModel, exam!!)
                2 -> ScannerTab(navController, viewModel, examId)
                3 -> if (exam != null) ExamDayTab(navController, viewModel, examId, exam!!)
                4 -> if (exam != null) ReportsTab(viewModel, examId, exam!!)
            }
        }
    }
}

@Composable
fun GenerateTab(navController: NavController, viewModel: OmrViewModel, examId: Int, exam: Exam?) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsStateWithLifecycle()
    var isGeneratingMale by remember { mutableStateOf(false) }
    var isGeneratingFemale by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) }

    val enrolledStudents = students.filter { exam?.subject != null && it.subjects.contains(exam.subject, ignoreCase = true) }
    val males = enrolledStudents.filter { it.gender.equals("Male", ignoreCase = true) }
    val females = enrolledStudents.filter { it.gender.equals("Female", ignoreCase = true) }

    val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && exam != null) {
            when (pendingAction) {
                "Male" -> {
                    isGeneratingMale = true
                    generateOmrPdf(context, exam, males, "Male", uri) {
                        isGeneratingMale = false
                    }
                }
                "Female" -> {
                    isGeneratingFemale = true
                    generateOmrPdf(context, exam, females, "Female", uri) {
                        isGeneratingFemale = false
                    }
                }
                "DeskSlips" -> {
                    generateDeskSlipsPdf(context, exam, enrolledStudents, uri)
                }
                "SeatingPlan" -> {
                    generateSeatingPlanPdf(context, exam, enrolledStudents, uri)
                }
            }
        }
        pendingAction = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Enrolled Students Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Candidate Roster",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "Subject: ${exam?.subject ?: "N/A"}",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "${enrolledStudents.size} Enrolled",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2563EB))
                            )
                            Column {
                                Text("Male Candidates", fontSize = 10.sp, color = Color(0xFF1E40AF))
                                Text("${males.size}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E3A8A))
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFFDF2F8),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFBCFE8))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFDB2777))
                            )
                            Column {
                                Text("Female Candidates", fontSize = 10.sp, color = Color(0xFF9D174D))
                                Text("${females.size}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF831843))
                            }
                        }
                    }
                }
            }
        }

        // Custom Drag & Drop OMR Studio Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
            border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF7C3AED)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DesignServices, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Full Custom OMR Studio", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF4C1D95))
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEDE9FE)) {
                                Text("PRO", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6D28D9), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Text("Drag & drop, custom text, label sizes, bubble count & print", fontSize = 10.5.sp, color = Color(0xFF6D28D9))
                    }
                }

                Button(
                    onClick = { navController.navigate(Screen.CustomOmrDesigner.route) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Drag & Drop OMR Designer", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
        }

        // Section 1: Pre-printed OMR Sheets
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFF1F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(15.dp))
                    }
                    Column {
                        Text("Pre-printed OMR Sheets (A4 PDF)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text("Generates individual OMR with candidate name & roll number", fontSize = 10.5.sp, color = Color(0xFF64748B))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumButton(
                        onClick = {
                            if (exam != null) {
                                pendingAction = "Male"
                                createDocumentLauncher.launch("${exam.name}_OMR_Male.pdf")
                            }
                        },
                        enabled = !isGeneratingMale && males.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 7.dp)
                    ) {
                        Text(if (isGeneratingMale) "Generating..." else "Male OMRs (${males.size})", fontSize = 11.5.sp)
                    }

                    PremiumButton(
                        onClick = {
                            if (exam != null) {
                                pendingAction = "Female"
                                createDocumentLauncher.launch("${exam.name}_OMR_Female.pdf")
                            }
                        },
                        enabled = !isGeneratingFemale && females.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 7.dp)
                    ) {
                        Text(if (isGeneratingFemale) "Generating..." else "Female OMRs (${females.size})", fontSize = 11.5.sp)
                    }
                }
            }
        }

        // Section 2: Hall Documents
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(15.dp))
                    }
                    Column {
                        Text("Examination Hall Materials", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text("Seating stickers and allocation tables for desks", fontSize = 10.5.sp, color = Color(0xFF64748B))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumOutlinedButton(
                        onClick = {
                            if (exam != null) {
                                pendingAction = "DeskSlips"
                                createDocumentLauncher.launch("${exam.name}_DeskSlips.pdf")
                            }
                        },
                        enabled = enrolledStudents.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 7.dp)
                    ) {
                        Text("Desk Slips (Stickers)", fontSize = 11.sp)
                    }

                    PremiumOutlinedButton(
                        onClick = {
                            if (exam != null) {
                                pendingAction = "SeatingPlan"
                                createDocumentLauncher.launch("${exam.name}_SeatingPlan.pdf")
                            }
                        },
                        enabled = enrolledStudents.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 7.dp)
                    ) {
                        Text("Seating Plan PDF", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateQuestionPaperTabInternal(viewModel: OmrViewModel, exam: Exam) {
    val context = LocalContext.current
    val dbQuestionsFlow = remember(exam.id) { viewModel.getQuestionsForExam(exam.id) }
    val dbQuestions by dbQuestionsFlow.collectAsStateWithLifecycle()
    val questions = remember { mutableStateListOf<QuestionEntity>() }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(dbQuestions) {
        if (!isInitialized) {
            questions.clear()
            questions.addAll(dbQuestions)
            isInitialized = true
        } else {
            val dbIds = dbQuestions.map { it.id }.toSet()
            val qIds = questions.map { it.id }.toSet()
            
            if (dbIds != qIds) {
                questions.removeAll { it.id !in dbIds }
                val newItems = dbQuestions.filter { it.id !in qIds }
                questions.addAll(newItems)
            }
        }
        
        if (dbQuestions.isNotEmpty()) {
            val correctAnswers = dbQuestions.map { it.correctIndex }
            viewModel.saveAnswerKey(exam.id, "A", dbQuestions.size, 4, correctAnswers) {}
        }
    }
    
    var isGenerating by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    
    // Paper Settings
    var showSettingsDialog by remember { mutableStateOf(false) }
    var headerText by remember { mutableStateOf("QUESTION PAPER") }
    var runByText by remember { mutableStateOf("") }
    var directorText by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf("★  ALL THE BEST  ★") }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }


    val coroutineScope = rememberCoroutineScope()
    val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            if (pendingAction == "generatePapers") {
                isGenerating = true
                generateQuestionPapersInternal(context, exam, questions, uri, viewModel, headerText, runByText, directorText, addressText, false, null) {
                    isGenerating = false
                }
            } else if (pendingAction == "downloadAnswerKey") {
                isGenerating = true
                coroutineScope.launch {
                    if (uri != null) { generateAnswerKeyPdf(context, exam, uri!!, viewModel) }
                    isGenerating = false
                }
            }
        }
    }

    var showJsonDialog by remember { mutableStateOf(false) }
    var jsonInput by remember { mutableStateOf("") }

    if (showSettingsDialog) {
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
                    bitmap = previewBitmap!!,
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxWidth().aspectRatio(previewBitmap!!.width.toFloat() / previewBitmap!!.height.toFloat())
                )
            },
            confirmButton = {
                TextButton(onClick = { showPreviewDialog = false }) { Text("Close") }
            }
        )
    }

    if (showSettingsDialog) {
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
                    bitmap = previewBitmap!!,
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxWidth().aspectRatio(previewBitmap!!.width.toFloat() / previewBitmap!!.height.toFloat())
                )
            },
            confirmButton = {
                TextButton(onClick = { showPreviewDialog = false }) { Text("Close") }
            }
        )
    }

    if (showJsonDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            title = { Text("Import Questions (JSON)") },
            text = {
                Column {
                    Text("Paste JSON array format:\n[\n  {\"text\": \"...\", \"options\": [\"A\", \"B\", \"C\", \"D\"], \"correctIndex\": 0}\n]")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        label = { Text("JSON Data") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Map::class.java)
                        val adapter = moshi.adapter<List<Map<String, Any>>>(type)
                        val parsed = adapter.fromJson(jsonInput)
                        if (parsed != null) {
                            val entities = parsed.map { map ->
                                val text = map["text"] as? String ?: ""
                                val options = map["options"] as? List<String> ?: listOf("", "", "", "")
                                val correctIndex = (map["correctIndex"] as? Double)?.toInt() ?: 0
                                QuestionEntity(
                                    examId = exam.id,
                                    text = text,
                                    optionA = options.getOrNull(0) ?: "",
                                    optionB = options.getOrNull(1) ?: "",
                                    optionC = options.getOrNull(2) ?: "",
                                    optionD = options.getOrNull(3) ?: "",
                                    correctIndex = correctIndex
                                )
                            }
                            viewModel.saveQuestions(entities) {
                                showJsonDialog = false
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid JSON format", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showJsonDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Questions (${questions.size}/100)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            IconButton(onClick = { showSettingsDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(17.dp))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PremiumOutlinedButton(
                onClick = { pendingAction = "downloadAnswerKey"; createDocumentLauncher.launch("${exam.subject}_AnswerKey.pdf") },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text(if (isGenerating && pendingAction == "downloadAnswerKey") "Key" else "Ans Key", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            PremiumOutlinedButton(
                onClick = { showJsonDialog = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("Import JSON", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            PremiumButton(
                onClick = {
                    if (questions.size < 100) {
                        viewModel.saveQuestion(QuestionEntity(examId = exam.id, text = "", optionA = "", optionB = "", optionC = "", optionD = "", correctIndex = 0)) {}
                    }
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("Add Question", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.weight(1f)) {
            items(
                count = questions.size,
                key = { index -> questions[index].id }
            ) { index ->
                val q = questions[index]
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Question ${index + 1}", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            IconButton(onClick = { viewModel.deleteQuestion(q) {} }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                        OutlinedTextField(
                            value = q.text,
                            onValueChange = { 
                                questions[index] = q.copy(text = it)
                                viewModel.updateQuestion(questions[index]) {} 
                            },
                            label = { Text("Question Text", fontSize = 11.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = q.optionA,
                            onValueChange = { 
                                questions[index] = q.copy(optionA = it)
                                viewModel.updateQuestion(questions[index]) {} 
                            },
                            label = { Text("Option A", fontSize = 11.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = q.optionB,
                            onValueChange = { 
                                questions[index] = q.copy(optionB = it)
                                viewModel.updateQuestion(questions[index]) {} 
                            },
                            label = { Text("Option B", fontSize = 11.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = q.optionC,
                            onValueChange = { 
                                questions[index] = q.copy(optionC = it)
                                viewModel.updateQuestion(questions[index]) {} 
                            },
                            label = { Text("Option C", fontSize = 11.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = q.optionD,
                            onValueChange = { 
                                questions[index] = q.copy(optionD = it)
                                viewModel.updateQuestion(questions[index]) {} 
                            },
                            label = { Text("Option D", fontSize = 11.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text("Correct Answer:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val labels = listOf("A", "B", "C", "D")
                            for (i in 0..3) {
                                FilterChip(
                                    selected = q.correctIndex == i,
                                    onClick = { 
                                        questions[index] = q.copy(correctIndex = i)
                                        viewModel.updateQuestion(questions[index]) {} 
                                    },
                                    label = { Text(labels[i], fontSize = 11.sp) },
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PremiumButton(
                onClick = {
                    if (questions.isEmpty()) {
                        Toast.makeText(context, "Add at least 1 question", Toast.LENGTH_SHORT).show()
                        return@PremiumButton
                    }
                    val pdfDoc = android.graphics.pdf.PdfDocument()
                    generateQuestionPapersInternal(context, exam, questions, null, viewModel, headerText, runByText, directorText, addressText, true, pdfDoc) {
                        Thread {
                            try {
                                val tempFile = java.io.File(context.cacheDir, "preview.pdf")
                                pdfDoc.writeTo(java.io.FileOutputStream(tempFile))
                                pdfDoc.close()
                                
                                val fd = android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                                val renderer = android.graphics.pdf.PdfRenderer(fd)
                                val page = renderer.openPage(0)
                                val bitmap = android.graphics.Bitmap.createBitmap(page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                page.close()
                                renderer.close()
                                fd.close()
                                
                                val imageBmp = bitmap.asImageBitmap()
                                (context as? android.app.Activity)?.runOnUiThread {
                                    previewBitmap = imageBmp
                                    showPreviewDialog = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }.start()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isGenerating && questions.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 9.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp).padding(end = 4.dp))
                Text("Preview", fontSize = 12.5.sp)
            }
            
            PremiumButton(
                onClick = {
                    if (questions.isEmpty()) {
                        Toast.makeText(context, "Add at least 1 question", Toast.LENGTH_SHORT).show()
                        return@PremiumButton
                    }
                    pendingAction = "generatePapers"
                    createDocumentLauncher.launch("${exam.subject}_QuestionPapers.pdf")
                },
                modifier = Modifier.weight(1f),
                enabled = !isGenerating && questions.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 9.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp).padding(end = 4.dp))
                Text("Download", fontSize = 12.5.sp)
            }
        }
    }
}

private fun generateQuestionPapersInternal(
    context: Context,
    exam: Exam,
    baseQuestions: List<QuestionEntity>,
    uri: android.net.Uri?,
    viewModel: OmrViewModel,
    headerText: String,
    runByText: String,
    directorText: String,
    addressText: String,
    isPreview: Boolean,
    providedPdfDoc: android.graphics.pdf.PdfDocument?,
    onDone: () -> Unit
) {
    Thread {
        val sets = if (isPreview) listOf("A") else listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        val pdfDocument = providedPdfDoc ?: PdfDocument()

        val pageW = 793f
        val pageH = 1122f

        val thinStroke = Paint().apply { strokeWidth = 1f; style = Paint.Style.STROKE; color = android.graphics.Color.BLACK }
        val thickStroke = Paint().apply { strokeWidth = 2f; style = Paint.Style.STROKE; color = android.graphics.Color.BLACK }
        val blackFill = Paint().apply { color = android.graphics.Color.BLACK; style = Paint.Style.FILL }

        val boldSmallPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = android.graphics.Color.BLACK
        }
        val whiteLargePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 70f
            textAlign = Paint.Align.CENTER
        }
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 34f
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.BLACK
        }
        val whiteNormalPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 16f
            textAlign = Paint.Align.CENTER
        }
        val instrBoldPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 13f
            color = android.graphics.Color.BLACK
        }
        val instrRegularPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 13f
            color = android.graphics.Color.BLACK
        }
        val qTextPaint = Paint().apply { 
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.BLACK
        }
        val qDotsPaint = Paint().apply { 
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) 
            color = android.graphics.Color.BLACK
        }
        val optPaint = Paint().apply { 
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) 
            color = android.graphics.Color.BLACK
        }
        val optLetterPaint = Paint().apply {
            textSize = 10f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = android.graphics.Color.BLACK
        }
        val footerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 16f
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.BLACK
        }
        val watermarkPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 700f
            textAlign = Paint.Align.CENTER
        }
        val verticalWatermarkPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#B0B0B0")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }

        for (setName in sets) {
            val shuffledQuestions = baseQuestions.shuffled()
            val correctAnswers = shuffledQuestions.map { it.correctIndex }
            viewModel.saveAnswerKey(exam.id, setName, shuffledQuestions.size, 4, correctAnswers) {}

            var questionsDrawn = 0
            val totalQuestions = shuffledQuestions.size
            var pageIndex = 1

            while (questionsDrawn < totalQuestions) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), pageIndex).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Border
                canvas.drawRect(20f, 20f, pageW - 20f, pageH - 20f, thinStroke)

                // Watermark
                canvas.drawText(setName, pageW / 2f, pageH / 2f + 250f, watermarkPaint)

                // Vertical Watermarks
                canvas.save()
                canvas.translate(35f, pageH / 2f)
                canvas.rotate(-90f)
                canvas.drawText("MADE BY SUMIT SHARMA", 0f, 0f, verticalWatermarkPaint)
                canvas.restore()
                
                canvas.save()
                canvas.translate(pageW - 35f, pageH / 2f)
                canvas.rotate(90f)
                canvas.drawText("MADE BY SUMIT SHARMA", 0f, 0f, verticalWatermarkPaint)
                canvas.restore()

                // Footer
                canvas.drawLine(40f, pageH - 80f, pageW - 40f, pageH - 80f, thinStroke)
                canvas.drawText(addressText.ifBlank { "★  ALL THE BEST  ★" }, pageW / 2f, pageH - 45f, footerPaint)

                val questionsOnThisPage = if (pageIndex == 1) 24 else 26
                val qPerCol = questionsOnThisPage / 2
                
                var startY = 0f
                var rowSpacing = 0f

                if (pageIndex == 1) {
                    // Top Left
                    canvas.drawText("SET CODE", 40f, 45f, boldSmallPaint)
                    canvas.drawRect(40f, 55f, 120f, 135f, blackFill)
                    canvas.drawText(setName, 80f, 120f, whiteLargePaint)

                    // Top Center
                    canvas.drawText(headerText.ifBlank { "QUESTION PAPER" }, pageW / 2f, 65f, titlePaint)
                    canvas.drawLine(pageW / 2f - 180f, 85f, pageW / 2f - 20f, 85f, thickStroke)
                    canvas.drawLine(pageW / 2f + 20f, 85f, pageW / 2f + 180f, 85f, thickStroke)
                    

                    val diamondPath = android.graphics.Path().apply {
                        moveTo(pageW / 2f, 80f)
                        lineTo(pageW / 2f + 8f, 85f)
                        lineTo(pageW / 2f, 90f)
                        lineTo(pageW / 2f - 8f, 85f)
                        close()
                    }
                    canvas.drawPath(diamondPath, blackFill)

                    val mcqRect = android.graphics.RectF(pageW / 2f - 170f, 105f, pageW / 2f + 170f, 135f)
                    canvas.drawRoundRect(mcqRect, 15f, 15f, blackFill)
                    canvas.drawText("MULTIPLE CHOICE QUESTIONS (MCQ)", pageW / 2f, 127f, whiteNormalPaint)

                    // Top Right
                    val detailsRect = android.graphics.RectF(pageW - 220f, 35f, pageW - 40f, 115f)
                    canvas.drawRoundRect(detailsRect, 5f, 5f, thinStroke)
                    
                    val labelX = pageW - 210f
                    val colonX = pageW - 105f
                    val valueX = pageW - 95f
                    
                    canvas.drawText("Total Questions", labelX, 60f, boldSmallPaint)
                    canvas.drawText(":", colonX, 60f, boldSmallPaint)
                    canvas.drawText("${baseQuestions.size}", valueX, 60f, boldSmallPaint)
                    
                    canvas.drawText("Total Marks", labelX, 80f, boldSmallPaint)
                    canvas.drawText(":", colonX, 80f, boldSmallPaint)
                    canvas.drawText("${baseQuestions.size}", valueX, 80f, boldSmallPaint)
                    
                    canvas.drawText("Time Allowed", labelX, 100f, boldSmallPaint)
                    canvas.drawText(":", colonX, 100f, boldSmallPaint)
                    canvas.drawText("60 Minutes", valueX, 100f, boldSmallPaint)

                    if (runByText.isNotBlank()) {
                        canvas.drawText("Run By: $runByText", 40f, 150f, boldSmallPaint.apply { textAlign = android.graphics.Paint.Align.LEFT })
                    }
                    if (directorText.isNotBlank()) {
                        canvas.drawText("Director: $directorText", pageW - 40f, 150f, boldSmallPaint.apply { textAlign = android.graphics.Paint.Align.RIGHT })
                    }
                    boldSmallPaint.textAlign = android.graphics.Paint.Align.LEFT

                    // Instructions Box
                    val instrRect = android.graphics.RectF(40f, 160f, pageW - 40f, 220f)
                    canvas.drawRoundRect(instrRect, 5f, 5f, thinStroke)
                    canvas.drawText("निर्देश : ", 60f, 185f, instrBoldPaint)
                    canvas.drawText("प्रत्येक प्रश्न 1 अंक का है। सही विकल्प (A / B / C / D) चुनें और", 185f, 185f, instrRegularPaint)
                    canvas.drawText("अपना उत्तर OMR शीट पर अंकित करें।", 185f, 205f, instrRegularPaint)

                    // Center Divider
                    canvas.drawLine(pageW / 2f, 230f, pageW / 2f, pageH - 80f, thinStroke)
                    
                    startY = 245f
                    rowSpacing = 65f
                } else {
                    // Small header for subsequent pages
                    canvas.drawText("SET CODE : $setName", pageW - 150f, 45f, boldSmallPaint)
                    // Center Divider
                    canvas.drawLine(pageW / 2f, 60f, pageW / 2f, pageH - 80f, thinStroke)
                    
                    startY = 70f
                    rowSpacing = 72f
                }

                // Questions
                val endQIndex = Math.min(questionsDrawn + questionsOnThisPage, totalQuestions)

                for (i in questionsDrawn until endQIndex) {
                    val qIndexOnPage = i - questionsDrawn
                    val isCol2 = qIndexOnPage >= qPerCol
                    val colX = if (isCol2) pageW / 2f + 20f else 40f
                    val rowIndex = qIndexOnPage % qPerCol
                    val qY = startY + (rowIndex * rowSpacing)
                    
                    val q = shuffledQuestions[i]
                    val qNum = "${i + 1}."
                    canvas.drawText(qNum, colX, qY, qTextPaint)
                    
                    var text = q.text
                    if (text.isBlank()) text = "......................................................................... ?"
                    
                    val maxW = 320f
                    var line1 = text
                    var line2 = ""
                    
                    val breakIndex = qDotsPaint.breakText(text, true, maxW, null)
                    if (breakIndex < text.length) {
                        val spaceIdx = text.lastIndexOf(' ', breakIndex)
                        val splitIdx = if (spaceIdx > 0) spaceIdx else breakIndex
                        line1 = text.substring(0, splitIdx)
                        line2 = text.substring(splitIdx).trim()
                        
                        val b2 = qDotsPaint.breakText(line2, true, maxW, null)
                        if (b2 < line2.length) {
                            line2 = line2.substring(0, b2) + "..."
                        }
                    }
                    
                    val isTwoLines = line2.isNotEmpty()
                    val optOffset = if (isTwoLines) 14f else 0f
                    
                    canvas.drawText(line1, colX + 25f, qY, qDotsPaint)
                    if (isTwoLines) {
                        canvas.drawText(line2, colX + 25f, qY + 14f, qDotsPaint)
                    }
                    
                    val optY1 = qY + 17f + optOffset
                    val optY2 = qY + 34f + optOffset
                    val colSpacing = 160f
                    val labels = listOf("(a)", "(b)", "(c)", "(d)")
                    
                    for (optIndex in 0..3) {
                        val isOptCol2 = optIndex % 2 != 0
                        val isOptRow2 = optIndex >= 2
                        
                        val optX = colX + 25f + if (isOptCol2) colSpacing else 0f
                        val currentOptY = if (isOptRow2) optY2 else optY1
                        
                        canvas.drawText(labels[optIndex], optX, currentOptY, optPaint)
                        
                        val optStrText = when (optIndex) {
                            0 -> q.optionA.ifBlank { "Option" }
                            1 -> q.optionB.ifBlank { "Option" }
                            2 -> q.optionC.ifBlank { "Option" }
                            3 -> q.optionD.ifBlank { "Option" }
                            else -> "Option"
                        }
                        var displayStr = optStrText
                        if (displayStr.length > 18) displayStr = displayStr.substring(0, 16) + ".."
                        
                        canvas.drawText(displayStr, optX + 20f, currentOptY, optPaint)
                    }
                }
                pdfDocument.finishPage(page)
                pageIndex++
                questionsDrawn = endQIndex
            }
        }

        if (!isPreview && uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Saved 10 Sets successfully", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
                }
            } finally {
                pdfDocument.close()
                (context as? android.app.Activity)?.runOnUiThread {
                    onDone()
                }
            }
        } else {
            (context as? android.app.Activity)?.runOnUiThread {
                onDone()
            }
        }
    }.start()
}


@Composable
fun ScannerTab(navController: NavController, viewModel: OmrViewModel, examId: Int) {
    val resultsFlow = remember(examId) { viewModel.getScanResultsForExam(examId) }
    val results by resultsFlow.collectAsStateWithLifecycle()
    val allStudents by viewModel.students.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Card: Scanner Launch
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF1F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "OMR Sheet Scanner",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "Instant camera grading with auto-alignment & edge detection",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                PremiumButton(
                    onClick = { navController.navigate(Screen.ScanOmr.createRoute(examId)) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch Scanner Camera", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Section Title & Scanned Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Evaluation Records",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = Color(0xFF0F172A)
            )
            Surface(
                color = if (results.isNotEmpty()) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "${results.size} Evaluated",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = if (results.isNotEmpty()) Color(0xFF1D4ED8) else Color(0xFF64748B)
                )
            }
        }

        if (results.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF8FAFC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Assessment,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "No OMR Sheets Evaluated Yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap 'Launch Scanner Camera' above to start grading student OMR responses.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(results.size) { i ->
                    val result = results[i]
                    val studentName = allStudents.find { it.rollNo == result.studentId }?.name ?: "Student ${result.studentId}"
                    var expanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            studentName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    Column {
                                        Text(
                                            studentName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            "Roll: ${result.studentId} • Set: ${result.paperSet.ifEmpty { "A" }}",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "${result.score} / ${result.totalQuestions}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }

                            if (expanded && result.questionStatuses.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    "Question-by-Question Breakdown",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val statusesList = com.example.data.Converters().toList(result.questionStatuses)
                                val chunkedStatuses = statusesList.chunked(10)
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    chunkedStatuses.forEachIndexed { rowIndex, rowStatuses ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            rowStatuses.forEachIndexed { colIndex, status ->
                                                val qIndex = rowIndex * 10 + colIndex
                                                val bg = when (status) {
                                                    1 -> Color(0xFF22C55E)
                                                    0 -> Color(0xFFEF4444)
                                                    else -> Color(0xFFCBD5E1)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .padding(end = 4.dp)
                                                        .size(20.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(bg),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "${qIndex + 1}",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateOmrPdf(context: Context, exam: Exam, students: List<com.example.data.Student>, gender: String, uri: android.net.Uri, onDone: () -> Unit) {
    Thread {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            for (student in students) {
                // High quality OMR sheet by creating the page with exact OMR dimensions (1000x1414)
                val pageInfo = PdfDocument.PageInfo.Builder(com.example.util.OmrGenerator.SHEET_WIDTH, com.example.util.OmrGenerator.SHEET_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                
                // Draw OMR vector directly onto the PDF canvas for infinite scalability
                com.example.util.OmrGenerator.drawOmrToCanvas(
                    context = context, 
                    canvas = canvas, 
                    numQuestions = 100, 
                    numOptions = 4,
                    templateType = exam.templateType, 
                    student = student, 
                    title = exam.title, 
                    logoPath = exam.logoUrl, 
                    logoOpacity = exam.logoOpacity, 
                    logoSize = exam.logoSize, 
                    logoPosition = exam.logoPosition
                )
                
                pdfDocument.finishPage(page)
            }
            context.contentResolver.openOutputStream(uri!!)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Saved $gender OMRs successfully", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
            }
        } finally {
            pdfDocument?.close()
            (context as? android.app.Activity)?.runOnUiThread {
                onDone()
            }
        }
    }.start()
}

private fun generateDeskSlipsPdf(context: Context, exam: Exam, students: List<com.example.data.Student>, uri: android.net.Uri) {
    Thread {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            val males = students.filter { it.gender.equals("Male", ignoreCase = true) }
            val females = students.filter { it.gender.equals("Female", ignoreCase = true) }
            
            val orderedStudents = males + females // Print males first, then females
            
            val pageWidth = 595
            val pageHeight = 842
            
            val marginX = 20f
            val marginY = 30f
            val cols = 5
            val rows = 18
            val cellWidth = (pageWidth - 2 * marginX) / cols
            val cellHeight = (pageHeight - 2 * marginY) / rows
            val cellsPerPage = cols * rows
            
            var currentPageInfo: PdfDocument.PageInfo? = null
            var currentPage: PdfDocument.Page? = null
            var canvas: android.graphics.Canvas? = null
            
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 14f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            val borderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1f
            }
            
            // To match the image exactly, we will print a full grid on each page.
            // We will fill the grid with roll numbers from the list.
            val totalPages = Math.max(1, Math.ceil(orderedStudents.size.toDouble() / cellsPerPage).toInt())
            
            for (page in 0 until totalPages) {
                currentPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, page + 1).create()
                currentPage = pdfDocument.startPage(currentPageInfo)
                canvas = currentPage.canvas
                
                // Draw Grid
                for (r in 0..rows) {
                    val y = marginY + r * cellHeight
                    canvas.drawLine(marginX, y, pageWidth - marginX, y, borderPaint)
                }
                for (c in 0..cols) {
                    val x = marginX + c * cellWidth
                    canvas.drawLine(x, marginY, x, pageHeight - marginY, borderPaint)
                }
                
                // In the image, the top-left cell says "ROLL NO"
                // But if we put data, we should probably fill all cells.
                // We'll add "ROLL NO" to the top-left cell ONLY if it's the first page and maybe it's just a label?
                // Actually, the user wants the desk slips to look like this table. 
                // Let's just fill the table with roll numbers. If the first cell is meant to be a header, 
                // we can reserve the first cell for "ROLL NO" and start data from the second cell.
                // Let's do that for the very first page just in case.
                
                var cellIndex = 0
                if (page == 0) {
                    // Draw "ROLL NO" in the first cell
                    val x = marginX + (0.5f * cellWidth)
                    val y = marginY + (0.5f * cellHeight) + 5f // adjust for text baseline
                    canvas.drawText("ROLL NO", x, y, textPaint)
                    cellIndex = 1
                }
                
                val startIndex = page * cellsPerPage - if (page > 0) 1 else 0
                
                for (i in cellIndex until cellsPerPage) {
                    val studentIndex = startIndex + i - if (page == 0) 1 else 0
                    if (studentIndex < orderedStudents.size && studentIndex >= 0) {
                        val student = orderedStudents[studentIndex]
                        
                        val col = i % cols
                        val row = i / cols
                        
                        val x = marginX + col * cellWidth + (cellWidth / 2f)
                        val y = marginY + row * cellHeight + (cellHeight / 2f) + 5f
                        
                        canvas.drawText(student.rollNo, x, y, textPaint)
                    }
                }
                
                pdfDocument.finishPage(currentPage)
            }

            context.contentResolver.openOutputStream(uri!!)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            
            (context as? android.app.Activity)?.runOnUiThread {
                android.widget.Toast.makeText(context, "Saved Desk Slips successfully", android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as? android.app.Activity)?.runOnUiThread {
                android.widget.Toast.makeText(context, "Error saving Desk Slips", android.widget.Toast.LENGTH_SHORT).show()
            }
        } finally {
            pdfDocument?.close()
        }
    }.start()
}

@Composable
fun ReportsTab(viewModel: OmrViewModel, examId: Int, exam: Exam) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingReport by remember { mutableStateOf<String?>(null) }
    var viewingReport by remember { mutableStateOf<String?>(null) }

    val resultsFlow = remember(viewModel, examId) { viewModel.getScanResultsForExam(examId) }
    val results by resultsFlow.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val attendanceMap by viewModel.attendanceMap.collectAsStateWithLifecycle()
    val allStudents = remember(students, exam.subject) {
        students.filter { it.subjects.contains(exam.subject, ignoreCase = true) }
    }

    val csvLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && pendingReport == "CSV Exporter") {
            val r = results
            val s = students
            coroutineScope.launch {
                try {
                    com.example.util.CsvExporter.exportResults(context, uri, exam, r, s)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "CSV export error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        pendingReport = null
    }

    val pdfLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && pendingReport != null) {
            val r = results
            val s = students
            val att = attendanceMap
            val rep = pendingReport!!
            coroutineScope.launch {
                try {
                    when (rep) {
                        "Rank List" -> com.example.util.ReportPdfGenerator.generateRankListPdf(context, exam, r, s, uri)
                        "Merit List" -> com.example.util.ReportPdfGenerator.generateMeritListPdf(context, exam, r, s, uri)
                        "Pass / Fail Summary" -> com.example.util.ReportPdfGenerator.generatePassFailSummaryPdf(context, exam, r, s, uri)
                        "Top 10 High Achievers" -> com.example.util.ReportPdfGenerator.generateTopAchieversPdf(context, exam, r, s, uri)
                        "Item Difficulty Analysis" -> com.example.util.ReportPdfGenerator.generateItemDifficultyPdf(context, exam, r, uri)
                        "Attendance vs Scanned" -> com.example.util.ReportPdfGenerator.generateAttendanceVsScannedPdf(context, exam, r, s, att, uri)
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "$rep PDF exported successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "PDF export error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                pendingReport = null
            }
        }
    }

    val startExport = { reportName: String ->
        pendingReport = reportName
        if (reportName == "CSV Exporter") {
            csvLauncher.launch("${exam.name}_results.csv")
        } else {
            val cleanName = reportName.replace(" ", "_").replace("/", "_")
            pdfLauncher.launch("${exam.name}_$cleanName.pdf")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Evaluation Performance & Analytics",
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            color = Color(0xFF0F172A)
        )

        // Stat Overview Cards (2x2 Grid)
        val passCount = results.count { it.score >= exam.passMarks }
        val passRate = if (results.isNotEmpty()) (passCount * 100) / results.size else 0
        val avgScore = if (results.isNotEmpty()) results.map { it.score }.average() else 0.0
        val maxScore = results.maxOfOrNull { it.score } ?: 0f

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Total Scanned", fontSize = 10.5.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${results.size}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("of ${allStudents.size} enrolled", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Passing Candidates", fontSize = 10.5.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$passCount ($passRate%)", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    Text("Min marks: ${exam.passMarks.toInt()}", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Class Average", fontSize = 10.5.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${String.format("%.1f", avgScore)}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                    Text("Marks per candidate", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Topper High Score", fontSize = 10.5.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${String.format("%.1f", maxScore)}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                    Text("Highest in batch", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        Text(
            "Official Export Reports",
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            color = Color(0xFF0F172A)
        )

        val reports = listOf(
            "Rank List" to "All students ordered by total score and percentile",
            "Merit List" to "Categorized into performance grade bands (A+, A, B, C)",
            "Pass / Fail Summary" to "Official classification based on passing cutoff",
            "Top 10 High Achievers" to "Executive summary of highest-ranking candidates",
            "Item Difficulty Analysis" to "Question-by-question correctness breakdown",
            "Attendance vs Scanned" to "Comparison of present roll numbers vs evaluated OMRs",
            "CSV Exporter" to "Download complete mark sheet as structured CSV file"
        )

        reports.forEach { (title, desc) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (title == "CSV Exporter") {
                            startExport(title)
                        } else {
                            viewingReport = title
                        }
                    },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))
                        Text(desc, fontSize = 10.5.sp, color = Color(0xFF64748B))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (title != "CSV Exporter") {
                            FilledTonalButton(
                                onClick = { viewingReport = title },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFF1F5F9),
                                    contentColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("View", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        PremiumOutlinedButton(
                            onClick = { startExport(title) },
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (title == "CSV Exporter") "Export CSV" else "Export PDF",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // In-app Report Viewer Dialog
    if (viewingReport != null) {
        val ranked = remember(exam, results, students) {
            com.example.util.ReportPdfGenerator.getRankedResults(exam, results, students)
        }
        ReportViewerDialog(
            title = viewingReport!!,
            subtitle = "${exam.name} • ${exam.subject}",
            onDismiss = { viewingReport = null },
            onExportPdf = { startExport(viewingReport!!) }
        ) {
            when (viewingReport) {
                "Rank List" -> RankListContent(ranked)
                "Merit List" -> MeritListContent(ranked)
                "Pass / Fail Summary" -> PassFailSummaryContent(exam, ranked)
                "Top 10 High Achievers" -> TopAchieversContent(ranked)
                "Item Difficulty Analysis" -> ItemDifficultyContent(results)
                "Attendance vs Scanned" -> AttendanceAuditContent(allStudents, results, attendanceMap)
            }
        }
    }
}

@Composable
fun ExamDayTab(navController: NavController, viewModel: OmrViewModel, examId: Int, exam: Exam) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsStateWithLifecycle()
    val enrolledStudents = students.filter { it.subjects.contains(exam.subject, ignoreCase = true) }
    val attendanceMap by viewModel.attendanceMap.collectAsStateWithLifecycle()
    var pendingAction by remember { mutableStateOf<String?>(null) }
    
    val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            when (pendingAction) {
                "SeatingPlan" -> generateSeatingPlanPdf(context, exam, enrolledStudents, uri)
                "AttendanceReport" -> generateAttendanceReportPdf(context, exam, enrolledStudents, attendanceMap, uri)
            }
        }
        pendingAction = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Attendance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val presentCount = enrolledStudents.count { attendanceMap[it.rollNo] == true }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("1. Candidate Attendance", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                        Text("Check candidates present in examination room", fontSize = 10.5.sp, color = Color(0xFF64748B))
                    }
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "$presentCount / ${enrolledStudents.size} Present",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().height(190.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                        items(enrolledStudents.size) { i ->
                            val student = enrolledStudents[i]
                            val isPresent = attendanceMap[student.rollNo] ?: false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val newMap = attendanceMap.toMutableMap()
                                        newMap[student.rollNo] = !isPresent
                                        viewModel.attendanceMap.value = newMap
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Checkbox(
                                    checked = isPresent,
                                    onCheckedChange = { checked ->
                                        val newMap = attendanceMap.toMutableMap()
                                        newMap[student.rollNo] = checked
                                        viewModel.attendanceMap.value = newMap
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(student.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                    Text("Roll No: ${student.rollNo}", fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                                if (isPresent) {
                                    Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(4.dp)) {
                                        Text("Present", modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                    }
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0))
                        }
                    }
                }

                PremiumOutlinedButton(
                    onClick = {
                        pendingAction = "AttendanceReport"
                        createDocumentLauncher.launch("${exam.name}_Attendance.pdf")
                    },
                    enabled = enrolledStudents.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 7.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download Attendance Sheet (PDF)", fontSize = 11.5.sp)
                }
            }
        }

        // Seating Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Seating Plan Matrix", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                Text("Room allocation chart sorted by candidate roll numbers", fontSize = 10.5.sp, color = Color(0xFF64748B))
                
                PremiumOutlinedButton(
                    onClick = {
                        pendingAction = "SeatingPlan"
                        createDocumentLauncher.launch("${exam.name}_SeatingPlan.pdf")
                    },
                    enabled = enrolledStudents.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 7.dp)
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Seating Plan PDF", fontSize = 11.5.sp)
                }
            }
        }

        // OMR Errors / Discrepancies Card
        val resultsFlow = remember(viewModel, examId) { viewModel.getScanResultsForExam(examId) }
        val results by resultsFlow.collectAsStateWithLifecycle()
        val errorResults = remember(results) {
            results.filter { it.paperSet.isEmpty() || it.studentAnswers.contains("[-1,-1]") || it.studentId.isEmpty() }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("3. Scanning Discrepancies", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                    if (errorResults.isNotEmpty()) {
                        Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(4.dp)) {
                            Text("${errorResults.size} Issues", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                }

                if (errorResults.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF16A34A)))
                            Text("All scanned OMR sheets are verified and free of format errors.", fontSize = 10.5.sp, color = Color(0xFF15803D))
                        }
                    }
                } else {
                    errorResults.forEach { res ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFFF1F2),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Unknown Roll / Set: ${res.paperSet.ifEmpty { "MISSING" }}", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF9F1239))
                                    Text("Score: ${res.score}", fontSize = 10.sp, color = Color(0xFFBE123C))
                                }
                                PremiumButton(
                                    onClick = { Toast.makeText(context, "Opening manual resolver", Toast.LENGTH_SHORT).show() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("Resolve", fontSize = 10.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateSeatingPlanPdf(context: Context, exam: Exam, students: List<com.example.data.Student>, uri: android.net.Uri) {
    Thread {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            val males = students.filter { it.gender.equals("Male", ignoreCase = true) }.sortedBy { it.rollNo }
            val females = students.filter { it.gender.equals("Female", ignoreCase = true) }.sortedBy { it.rollNo }
            val orderedStudents = males + females
            
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }
            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 18f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            
            val studentsPerRoom = 40
            val rooms = orderedStudents.chunked(studentsPerRoom)
            
            for ((roomIndex, roomStudents) in rooms.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, roomIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                
                canvas.drawText("Seating Arrangement Plan - ${exam.title}", pageWidth / 2f, 50f, titlePaint)
                canvas.drawText("Subject: ${exam.subject}   |   Room: ${roomIndex + 1}", pageWidth / 2f, 80f, textPaint.apply { textAlign = android.graphics.Paint.Align.CENTER })
                
                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                var y = 120f
                canvas.drawText("Roll Number", margin, y, titlePaint.apply { textSize = 12f; textAlign = android.graphics.Paint.Align.LEFT })
                canvas.drawText("Name", margin + 120f, y, titlePaint)
                canvas.drawText("Bench No", pageWidth - margin - 80f, y, titlePaint)
                
                y += 20f
                canvas.drawLine(margin, y, pageWidth - margin, y, textPaint)
                y += 20f
                
                for ((index, student) in roomStudents.withIndex()) {
                    canvas.drawText(student.rollNo, margin, y, textPaint)
                    canvas.drawText(student.name, margin + 120f, y, textPaint)
                    canvas.drawText("Bench ${(index / 2) + 1}${if(index%2==0) "A" else "B"}", pageWidth - margin - 80f, y, textPaint)
                    y += 20f
                }
                
                pdfDocument.finishPage(page)
            }
            context.contentResolver.openOutputStream(uri!!)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument?.close()
        }
    }.start()
}

private fun generateAttendanceReportPdf(context: Context, exam: Exam, students: List<com.example.data.Student>, attendanceMap: Map<String, Boolean>, uri: android.net.Uri) {
    Thread {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            val orderedStudents = students.sortedBy { it.rollNo }
            
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }
            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 16f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            
            val studentsPerPage = 35
            val chunks = orderedStudents.chunked(studentsPerPage)
            
            for ((pageIndex, chunk) in chunks.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                
                canvas.drawText("Attendance & Absentee Report - ${exam.title}", pageWidth / 2f, 50f, titlePaint)
                canvas.drawText("Subject: ${exam.subject}   |   Date: ${exam.date}", pageWidth / 2f, 75f, textPaint.apply { textAlign = android.graphics.Paint.Align.CENTER })
                
                val presentCount = students.count { attendanceMap[it.rollNo] == true }
                val absentCount = students.size - presentCount
                canvas.drawText("Total: ${students.size} | Present: $presentCount | Absent: $absentCount", pageWidth / 2f, 95f, textPaint)
                
                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                var y = 130f
                canvas.drawText("Roll Number", margin, y, titlePaint.apply { textSize = 12f; textAlign = android.graphics.Paint.Align.LEFT })
                canvas.drawText("Name", margin + 120f, y, titlePaint)
                canvas.drawText("Status", pageWidth - margin - 100f, y, titlePaint)
                canvas.drawText("Sign", pageWidth - margin - 40f, y, titlePaint)
                
                y += 15f
                canvas.drawLine(margin, y, pageWidth - margin, y, textPaint)
                y += 20f
                
                for (student in chunk) {
                    val isPresent = attendanceMap[student.rollNo] == true
                    val statusText = if (isPresent) "Present" else "Absent"
                    
                    if (!isPresent) textPaint.color = android.graphics.Color.RED else textPaint.color = android.graphics.Color.BLACK
                    
                    canvas.drawText(student.rollNo, margin, y, textPaint)
                    canvas.drawText(student.name, margin + 120f, y, textPaint)
                    canvas.drawText(statusText, pageWidth - margin - 100f, y, textPaint)
                    canvas.drawText(if(isPresent) "_____" else "N/A", pageWidth - margin - 40f, y, textPaint)
                    y += 20f
                }
                
                pdfDocument.finishPage(page)
            }
            
            context.contentResolver.openOutputStream(uri!!)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument?.close()
        }
    }.start()
}
