package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.Exam
import com.example.data.Student
import com.example.util.CodeGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCardsScreen(
    navController: NavController,
    viewModel: OmrViewModel,
    initialRollNo: String = "ALL"
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: ID Card, 1: Admit Card
    var selectedStudentRoll by remember {
        mutableStateOf(if (initialRollNo != "ALL" && initialRollNo.isNotEmpty()) initialRollNo else "")
    }
    var selectedExamId by remember { mutableIntStateOf(exams.firstOrNull()?.id ?: 0) }

    LaunchedEffect(students) {
        if (selectedStudentRoll.isEmpty() && students.isNotEmpty()) {
            selectedStudentRoll = if (initialRollNo != "ALL" && students.any { it.rollNo == initialRollNo }) {
                initialRollNo
            } else {
                students.first().rollNo
            }
        }
    }

    LaunchedEffect(exams) {
        if (selectedExamId == 0 && exams.isNotEmpty()) {
            selectedExamId = exams.first().id
        }
    }

    val activeStudent = students.find { it.rollNo == selectedStudentRoll } ?: students.firstOrNull()
    val activeExam = exams.find { it.id == selectedExamId } ?: exams.firstOrNull()

    // Generate barcodes and QR codes for the active student
    val qrBitmap = remember(activeStudent, activeExam, selectedTab) {
        if (activeStudent != null) {
            val qrPayload = if (selectedTab == 0) {
                "STUDENT_ID|${activeStudent.rollNo}|${activeStudent.name}|${activeStudent.sessionName}|${activeStudent.stream}"
            } else {
                "ADMIT_CARD|${activeStudent.rollNo}|${activeStudent.name}|EXAM:${activeExam?.name ?: "Exam"}|${activeExam?.date ?: "Date"}"
            }
            CodeGenerator.generateQrCodeBitmap(qrPayload, 260)
        } else null
    }

    val barcodeBitmap = remember(activeStudent) {
        if (activeStudent != null) {
            CodeGenerator.generateBarcodeBitmap(activeStudent.rollNo, 360, 90)
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedTab == 0) "Student ID Card" else "Exam Admit Card",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Instant Barcode & QR Generator",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B)
                        )
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
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(
                            context,
                            "Card ready for print & download",
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = "Print",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFAFBFD)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. MODE TABS (ID Card vs Admit Card)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = 0 },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 0) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 0) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) Color(0xFFE11D48) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Student ID Card",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) Color(0xFFE11D48) else Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = 1 },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 1) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 1) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Assignment,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) Color(0xFF4F46E5) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Test Admit Card",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) Color(0xFF4F46E5) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            // 2. STUDENT SELECTOR CAROUSEL
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Select Student",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (students.isEmpty()) {
                        Text(
                            text = "No students admitted yet. Go to New Admission to register students.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(students) { st ->
                                val isSelected = st.rollNo == selectedStudentRoll
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedStudentRoll = st.rollNo },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = st.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF334155)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "#${st.rollNo}",
                                            fontSize = 10.sp,
                                            color = if (isSelected) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. EXAM SELECTOR (Admit Card Mode Only)
            if (selectedTab == 1) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Select Exam / Test Paper",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (exams.isEmpty()) {
                            Text(
                                text = "No exams found. Create an exam to generate Admit Cards.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(exams) { ex ->
                                    val isSelected = ex.id == selectedExamId
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedExamId = ex.id },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text(
                                                text = ex.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = ex.subject,
                                                fontSize = 9.5.sp,
                                                color = if (isSelected) Color(0xFFE0E7FF) else Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // 4. THE LIVE GENERATED CARD VIEW
            item {
                if (activeStudent == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Please select or admit a student to generate card.", color = Color(0xFF64748B))
                    }
                } else {
                    if (selectedTab == 0) {
                        // ==================== ID CARD PREVIEW ====================
                        StudentIdCardView(
                            student = activeStudent,
                            qrBitmap = qrBitmap,
                            barcodeBitmap = barcodeBitmap
                        )
                    } else {
                        // ==================== ADMIT CARD PREVIEW ====================
                        StudentAdmitCardView(
                            student = activeStudent,
                            exam = activeExam,
                            qrBitmap = qrBitmap,
                            barcodeBitmap = barcodeBitmap
                        )
                    }
                }
            }

            // 5. QUICK ACTION BUTTONS
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Exporting card to high-res image...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) Color(0xFFE11D48) else Color(0xFF4F46E5)
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Print dialog initiated...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF334155))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print Card", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    }
                }
            }
        }
    }
}

@Composable
fun StudentIdCardView(
    student: Student,
    qrBitmap: Bitmap?,
    barcodeBitmap: Bitmap?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFFE11D48))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_sp_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SHREE PRABHA COACHING",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "STUDENT IDENTITY CARD",
                                color = Color(0xFFFDE047),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE11D48)
                    ) {
                        Text(
                            text = student.stream.uppercase(),
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Body Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Photo Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(90.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(86.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(2.dp, Color(0xFF0F172A)),
                        color = Color(0xFFF1F5F9)
                    ) {
                        if (student.imagePath.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(student.imagePath)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Student Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "ROLL #${student.rollNo}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Student Attributes
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    IdCardFieldRow(label = "Father:", value = student.fatherName.ifBlank { "N/A" })
                    IdCardFieldRow(label = "Batch:", value = student.sessionName.ifBlank { "Regular Batch" })
                    IdCardFieldRow(label = "Class:", value = student.className.ifBlank { "12th Standard" })
                    if (student.mobileNo.isNotBlank()) {
                        IdCardFieldRow(label = "Contact:", value = student.mobileNo)
                    }
                    if (student.dob.isNotBlank()) {
                        IdCardFieldRow(label = "DOB:", value = student.dob)
                    }
                    if (student.subjects.isNotBlank()) {
                        IdCardFieldRow(label = "Subjects:", value = student.subjects)
                    }
                }

                // QR Code
                if (qrBitmap != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(65.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(62.dp)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                .padding(2.dp)
                        )
                        Text(
                            text = "Scan to Verify",
                            fontSize = 7.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Bottom Barcode & Footer Strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (barcodeBitmap != null) {
                    Image(
                        bitmap = barcodeBitmap.asImageBitmap(),
                        contentDescription = "Barcode",
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(28.dp),
                        contentScale = ContentScale.FillBounds
                    )
                    Text(
                        text = "* ${student.rollNo} *",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF475569)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Valid for Academic Session 2024-2025 • Non-Transferable",
                    fontSize = 7.5.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun StudentAdmitCardView(
    student: Student,
    exam: Exam?,
    qrBitmap: Bitmap?,
    barcodeBitmap: Bitmap?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4F46E5))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SHREE PRABHA COACHING INSTITUTE",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "OFFICIAL EXAMINATION ADMIT CARD",
                            color = Color(0xFF818CF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF4338CA)
                    ) {
                        Text(
                            text = "HALL TICKET",
                            color = Color(0xFFE0E7FF),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Exam Info Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFEEF2FF),
                border = BorderStroke(1.dp, Color(0xFFE0E7FF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TEST: ${exam?.name ?: "Mid-Term OMR Assessment"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF312E81)
                        )
                        Text(
                            text = "Subject: ${exam?.subject ?: "All Subjects"} • Date: ${exam?.date?.ifBlank { "Exam Scheduled" } ?: "Upcoming"}",
                            fontSize = 9.5.sp,
                            color = Color(0xFF4338CA)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                    ) {
                        Text(
                            text = "OMR EVAL",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4338CA),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Student Body
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Photo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(80.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(76.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF312E81)),
                        color = Color(0xFFF8FAFC)
                    ) {
                        if (student.imagePath.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(student.imagePath)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Student Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(40.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "#${student.rollNo}",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1B4B)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Fields
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    IdCardFieldRow(label = "Father's Name:", value = student.fatherName.ifBlank { "N/A" })
                    IdCardFieldRow(label = "Roll Number:", value = student.rollNo)
                    if (student.registrationNo.isNotBlank()) {
                        IdCardFieldRow(label = "Registration:", value = student.registrationNo)
                    }
                    IdCardFieldRow(label = "Batch / Class:", value = "${student.sessionName} (${student.className})")
                    IdCardFieldRow(label = "Stream:", value = student.stream)
                }

                // QR Code
                if (qrBitmap != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(65.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(60.dp)
                                .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(4.dp))
                                .padding(2.dp)
                        )
                        Text(
                            text = "Admit QR",
                            fontSize = 7.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // Instructions Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, Color(0xFFFEF3C7))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "IMPORTANT CANDIDATE INSTRUCTIONS:",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                    Text(
                        text = "1. Carry this Admit Card and Student ID Card to the examination hall.\n2. Darken OMR bubbles completely with blue or black ballpoint pen only.\n3. Verify your Roll No bubble grid matching the barcode below.",
                        fontSize = 8.sp,
                        color = Color(0xFF78350F),
                        lineHeight = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Signature Rows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.width(100.dp).height(1.dp).background(Color(0xFF94A3B8)))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Student Signature", fontSize = 8.sp, color = Color(0xFF64748B))
                }

                if (barcodeBitmap != null) {
                    Image(
                        bitmap = barcodeBitmap.asImageBitmap(),
                        contentDescription = "Barcode",
                        modifier = Modifier
                            .width(120.dp)
                            .height(26.dp),
                        contentScale = ContentScale.FillBounds
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.width(100.dp).height(1.dp).background(Color(0xFF94A3B8)))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Controller of Exams", fontSize = 8.sp, color = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun IdCardFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier.width(52.dp)
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
