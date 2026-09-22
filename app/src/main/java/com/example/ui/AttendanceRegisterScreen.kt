package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.AttendanceRecord
import com.example.data.CoachingSession
import com.example.data.Student
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceRegisterScreen(
    navController: NavController,
    viewModel: OmrViewModel
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val allAttendance by viewModel.attendance.collectAsStateWithLifecycle()

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    var selectedDate by remember { mutableStateOf(todayDate) }
    var selectedSessionId by remember { mutableStateOf("ALL") }
    var showScanPromptDialog by remember { mutableStateOf(false) }
    var quickRollInput by remember { mutableStateOf("") }

    val activeSession = sessions.find { it.id == selectedSessionId }

    // Filter students by selected batch/session if chosen
    val sessionStudents = remember(students, selectedSessionId) {
        if (selectedSessionId == "ALL") {
            students
        } else {
            val matching = students.filter { it.sessionId == selectedSessionId }
            if (matching.isNotEmpty()) matching else students
        }
    }

    // Today's attendance map: Roll -> AttendanceRecord
    val dateAttendanceMap = remember(allAttendance, selectedDate, selectedSessionId) {
        allAttendance
            .filter { it.date == selectedDate }
            .associateBy { it.studentRollNo }
    }

    // Counts
    val counts = remember(sessionStudents, dateAttendanceMap) {
        var p = 0
        var a = 0
        var l = 0
        for (st in sessionStudents) {
            when (dateAttendanceMap[st.rollNo]?.status) {
                "PRESENT" -> p++
                "ABSENT" -> a++
                "LATE" -> l++
            }
        }
        val pending = (sessionStudents.size - (p + a + l)).coerceAtLeast(0)
        listOf(p, a, l, pending)
    }
    val presentCount = counts[0]
    val absentCount = counts[1]
    val lateCount = counts[2]
    val unmarkedCount = counts[3]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Attendance Register",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Classroom Check-In & Roll Scanner",
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0284C7),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showScanPromptDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fast Scan", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
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
            // 1. DATE & BATCH FILTER BAR
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Date: $selectedDate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }

                            // Mark All Present button
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                                        val batchRecords = sessionStudents.map { st ->
                                            AttendanceRecord(
                                                studentRollNo = st.rollNo,
                                                studentName = st.name,
                                                date = selectedDate,
                                                status = "PRESENT",
                                                sessionId = activeSession?.id ?: st.sessionId,
                                                sessionName = activeSession?.title ?: st.sessionName,
                                                inTime = currentTime
                                            )
                                        }
                                        viewModel.markBatchAttendance(batchRecords) {
                                            Toast.makeText(context, "All ${batchRecords.size} students marked Present!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Text(
                                    text = "Mark All Present",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Batch filter chips
                        Text("Filter By Session / Batch", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                val isSelected = selectedSessionId == "ALL"
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { selectedSessionId = "ALL" }
                                ) {
                                    Text(
                                        text = "All Batches (${students.size})",
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            items(sessions, key = { it.id }) { s ->
                                val isSelected = selectedSessionId == s.id
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) Color(0xFF6366F1) else Color(0xFFF1F5F9),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { selectedSessionId = s.id }
                                ) {
                                    Text(
                                        text = s.title,
                                        fontSize = 10.5.sp,
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

            // 2. SUMMARY METRICS ROW
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendanceMetricPill(label = "Present", count = presentCount, color = Color(0xFF15803D), bg = Color(0xFFDCFCE7), modifier = Modifier.weight(1f))
                    AttendanceMetricPill(label = "Absent", count = absentCount, color = Color(0xFFBE123C), bg = Color(0xFFFFE4E6), modifier = Modifier.weight(1f))
                    AttendanceMetricPill(label = "Late", count = lateCount, color = Color(0xFFB45309), bg = Color(0xFFFEF3C7), modifier = Modifier.weight(1f))
                    AttendanceMetricPill(label = "Pending", count = unmarkedCount, color = Color(0xFF475569), bg = Color(0xFFF1F5F9), modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. STUDENT ATTENDANCE LIST
            if (sessionStudents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No students in this session. Go to New Admission to register.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                }
            } else {
                items(sessionStudents, key = { it.rollNo }) { student ->
                    val record = dateAttendanceMap[student.rollNo]
                    val currentStatus = record?.status ?: "UNMARKED"

                    AttendanceStudentRow(
                        student = student,
                        currentStatus = currentStatus,
                        inTime = record?.inTime.orEmpty(),
                        onStatusChange = { newStatus ->
                            val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                            viewModel.markAttendance(
                                studentRollNo = student.rollNo,
                                studentName = student.name,
                                date = selectedDate,
                                status = newStatus,
                                sessionId = activeSession?.id ?: student.sessionId,
                                sessionName = activeSession?.title ?: student.sessionName,
                                inTime = if (newStatus == "PRESENT" || newStatus == "LATE") now else ""
                            )
                        }
                    )
                }
            }
        }
    }

    // DIALOG: FAST SCANNER / ROLL LOOKUP
    if (showScanPromptDialog) {
        AlertDialog(
            onDismissRequest = { showScanPromptDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF0284C7))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Classroom Roll / QR Check-In", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Scan or type the student's Roll Number / QR to instantly mark them PRESENT with current timestamp.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF475569)
                    )

                    OutlinedTextField(
                        value = quickRollInput,
                        onValueChange = { quickRollInput = it },
                        label = { Text("Roll Number (e.g. 240001)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = quickRollInput.trim()
                        val matched = students.find { it.rollNo.equals(trimmed, ignoreCase = true) }
                        if (matched != null) {
                            val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                            viewModel.markAttendance(
                                studentRollNo = matched.rollNo,
                                studentName = matched.name,
                                date = selectedDate,
                                status = "PRESENT",
                                sessionId = activeSession?.id ?: matched.sessionId,
                                sessionName = activeSession?.title ?: matched.sessionName,
                                inTime = now
                            )
                            Toast.makeText(context, "Marked ${matched.name} (#${matched.rollNo}) PRESENT at $now", Toast.LENGTH_SHORT).show()
                            quickRollInput = ""
                            showScanPromptDialog = false
                        } else {
                            Toast.makeText(context, "Student with roll '$trimmed' not found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Mark Present")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScanPromptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AttendanceMetricPill(
    label: String,
    count: Int,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun AttendanceStudentRow(
    student: Student,
    currentStatus: String,
    inTime: String,
    onStatusChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Student identity
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = student.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "#${student.rollNo}",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold
                        )
                        if (inTime.isNotBlank()) {
                            Text(
                                text = "• In: $inTime",
                                fontSize = 9.5.sp,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Toggle Buttons (P, A, L)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AttendanceToggleButton(
                    label = "P",
                    isSelected = currentStatus == "PRESENT",
                    activeBg = Color(0xFF16A34A),
                    activeFg = Color.White,
                    onClick = { onStatusChange("PRESENT") }
                )
                AttendanceToggleButton(
                    label = "A",
                    isSelected = currentStatus == "ABSENT",
                    activeBg = Color(0xFFE11D48),
                    activeFg = Color.White,
                    onClick = { onStatusChange("ABSENT") }
                )
                AttendanceToggleButton(
                    label = "L",
                    isSelected = currentStatus == "LATE",
                    activeBg = Color(0xFFD97706),
                    activeFg = Color.White,
                    onClick = { onStatusChange("LATE") }
                )
            }
        }
    }
}

@Composable
fun AttendanceToggleButton(
    label: String,
    isSelected: Boolean,
    activeBg: Color,
    activeFg: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (isSelected) activeBg else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isSelected) activeBg else Color(0xFFE2E8F0))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) activeFg else Color(0xFF64748B)
            )
        }
    }
}
