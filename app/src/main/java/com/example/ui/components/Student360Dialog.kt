package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AttendanceRecord
import com.example.data.FeeRecord
import com.example.data.Student
import com.example.data.TimetablePeriod
import com.example.ui.OmrViewModel
import com.example.util.CoachingFeaturesManager
import com.example.util.DynamicTimetableEngine
import com.example.util.FeeAndAttendanceManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Student360Dialog(
    student: Student,
    viewModel: OmrViewModel,
    onDismiss: () -> Unit,
    onNavigateToTimetable: (String) -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToFees: () -> Unit = {},
    onNavigateToIdCard: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableIntStateOf(0) }
    val sectionTabs = listOf("Overview", "Routine", "Attendance", "Fees", "Faculty")

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    val todayDayOfWeek = remember {
        SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date()).uppercase(Locale.ENGLISH)
    }

    // Dynamic Connected Timetable for this student
    var studentTimetable by remember {
        mutableStateOf(
            CoachingFeaturesManager.getTimetableForStudent(
                className = student.className.ifBlank { student.sessionName },
                sessionName = student.sessionName
            )
        )
    }

    // Dynamic Attendance for this student
    var studentAttendance by remember {
        mutableStateOf(FeeAndAttendanceManager.getAttendanceForStudent(student.rollNo))
    }

    // Dynamic Fees for this student
    var studentFees by remember {
        mutableStateOf(FeeAndAttendanceManager.getFeesForStudent(student.rollNo))
    }

    // Dynamic Faculty teaching this student's class
    val facultyList = remember {
        CoachingFeaturesManager.getFaculty().filter { f ->
            f.assignedBatches.contains("All", ignoreCase = true) ||
                    (student.className.isNotBlank() && f.assignedBatches.contains(student.className, ignoreCase = true)) ||
                    (student.sessionName.isNotBlank() && f.assignedBatches.contains(student.sessionName, ignoreCase = true))
        }.ifEmpty { CoachingFeaturesManager.getFaculty().take(4) }
    }

    // Attendance stats
    val totalAttendanceDays = studentAttendance.size
    val presentDays = studentAttendance.count { it.status == "PRESENT" }
    val absentDays = studentAttendance.count { it.status == "ABSENT" }
    val lateDays = studentAttendance.count { it.status == "LATE" }
    val attendancePct = if (totalAttendanceDays > 0) (presentDays.toFloat() / totalAttendanceDays * 100).toInt() else 100
    val todayAttendanceRecord = studentAttendance.find { it.date == todayDate }

    // Fee stats
    val totalPaid = studentFees.sumOf { it.amountPaid }
    val totalFeeAssigned = studentFees.maxOfOrNull { it.totalFee } ?: 12000.0
    val feeBalance = (totalFeeAssigned - totalPaid).coerceAtLeast(0.0)

    // Quick Fee Payment states
    var quickFeeAmount by remember { mutableStateOf("") }
    var quickFeeMode by remember { mutableStateOf("Cash") }
    var isAddingFee by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ==================== HEADER BAR ====================
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isMale = student.gender.equals("Male", ignoreCase = true)
                            val avatarBg = if (isMale) Color(0xFFE0F2FE) else Color(0xFFFFE4E6)
                            val avatarTint = if (isMale) Color(0xFF0284C7) else Color(0xFFE11D48)

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(avatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = student.name.take(1).uppercase(),
                                    color = avatarTint,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = student.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFF1F5F9)
                                    ) {
                                        Text(
                                            text = "Roll: ${student.rollNo}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF334155),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${student.className.ifBlank { "Class 12th" }} • ${student.stream} • ${student.sessionName.ifBlank { "Regular Batch" }}",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                            }
                        }

                        // ==================== SECTION TABS ====================
                        ScrollableTabRow(
                            selectedTabIndex = selectedSection,
                            containerColor = Color.White,
                            contentColor = Color(0xFFE11D48),
                            edgePadding = 12.dp,
                            indicator = { tabPositions ->
                                if (selectedSection < tabPositions.size) {
                                    val currentPos = tabPositions[selectedSection]
                                    Box(
                                        modifier = Modifier
                                            .wrapContentSize(Alignment.BottomStart)
                                            .offset(x = currentPos.left)
                                            .width(currentPos.width)
                                            .height(2.5.dp)
                                            .padding(horizontal = 8.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFFE11D48))
                                    )
                                }
                            },
                            divider = { HorizontalDivider(color = Color(0xFFE2E8F0)) }
                        ) {
                            sectionTabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedSection == index,
                                    onClick = { selectedSection = index },
                                    text = {
                                        Text(
                                            text = title,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (selectedSection == index) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedSection == index) Color(0xFFE11D48) else Color(0xFF64748B)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // ==================== CONTENT BODY ====================
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    when (selectedSection) {
                        0 -> StudentOverviewSection(
                            student = student,
                            attendancePct = attendancePct,
                            feeBalance = feeBalance,
                            onNavigateToIdCard = {
                                onDismiss()
                                onNavigateToIdCard(student.rollNo)
                            }
                        )
                        1 -> StudentRoutineSection(
                            student = student,
                            timetable = studentTimetable,
                            todayDayOfWeek = todayDayOfWeek,
                            onOpenTimetableStudio = {
                                onDismiss()
                                onNavigateToTimetable(student.className.ifBlank { "Class 12th" })
                            }
                        )
                        2 -> StudentAttendanceSection(
                            student = student,
                            attendance = studentAttendance,
                            todayDate = todayDate,
                            todayRecord = todayAttendanceRecord,
                            attendancePct = attendancePct,
                            presentDays = presentDays,
                            absentDays = absentDays,
                            lateDays = lateDays,
                            onMarkToday = { status ->
                                viewModel.markAttendance(
                                    studentRollNo = student.rollNo,
                                    studentName = student.name,
                                    date = todayDate,
                                    status = status,
                                    sessionId = student.sessionId,
                                    sessionName = student.sessionName
                                )
                                studentAttendance = FeeAndAttendanceManager.getAttendanceForStudent(student.rollNo)
                                Toast.makeText(context, "Attendance marked: $status", Toast.LENGTH_SHORT).show()
                            },
                            onOpenAttendanceRegister = {
                                onDismiss()
                                onNavigateToAttendance()
                            }
                        )
                        3 -> StudentFeeSection(
                            student = student,
                            fees = studentFees,
                            totalPaid = totalPaid,
                            totalFeeAssigned = totalFeeAssigned,
                            balance = feeBalance,
                            isAddingFee = isAddingFee,
                            quickFeeAmount = quickFeeAmount,
                            quickFeeMode = quickFeeMode,
                            onAmountChange = { quickFeeAmount = it },
                            onModeChange = { quickFeeMode = it },
                            onToggleAdd = { isAddingFee = !isAddingFee },
                            onSubmitFee = {
                                val amt = quickFeeAmount.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    val newFee = FeeRecord(
                                        studentRollNo = student.rollNo,
                                        studentName = student.name,
                                        amountPaid = amt,
                                        totalFee = totalFeeAssigned,
                                        paymentDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
                                        paymentMode = quickFeeMode,
                                        receiptNo = "REC-${System.currentTimeMillis() % 100000}",
                                        monthOrInstallment = "Installment",
                                        remarks = "Direct 360 payment"
                                    )
                                    viewModel.addFeeRecord(newFee) {
                                        studentFees = FeeAndAttendanceManager.getFeesForStudent(student.rollNo)
                                        quickFeeAmount = ""
                                        isAddingFee = false
                                        Toast.makeText(context, "Payment of ₹$amt recorded!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenFeeTracker = {
                                onDismiss()
                                onNavigateToFees()
                            }
                        )
                        4 -> StudentFacultySection(
                            student = student,
                            facultyList = facultyList
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// SECTION 0: OVERVIEW
// =========================================================================
@Composable
private fun StudentOverviewSection(
    student: Student,
    attendancePct: Int,
    feeBalance: Double,
    onNavigateToIdCard: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Stat Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Attendance Pill
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (attendancePct >= 75) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, if (attendancePct >= 75) Color(0xFFBBF7D0) else Color(0xFFFECACA))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (attendancePct >= 75) Color(0xFF16A34A) else Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Attendance", fontSize = 10.5.sp, color = Color(0xFF475569))
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$attendancePct%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (attendancePct >= 75) Color(0xFF15803D) else Color(0xFFB91C1C)
                    )
                }
            }

            // Fee Dues Pill
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (feeBalance <= 0) Color(0xFFEFF6FF) else Color(0xFFFFFBEB)),
                border = BorderStroke(1.dp, if (feeBalance <= 0) Color(0xFFBFDBFE) else Color(0xFFFDE68A))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null,
                            tint = if (feeBalance <= 0) Color(0xFF2563EB) else Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fee Status", fontSize = 10.5.sp, color = Color(0xFF475569))
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (feeBalance <= 0) "CLEARED" else "DUE: ₹${feeBalance.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (feeBalance <= 0) Color(0xFF1D4ED8) else Color(0xFFB45309),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Contact & Guardians Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Guardian & Contact Information",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                InfoRow(icon = Icons.Default.Person, label = "Father's Name", value = student.fatherName.ifBlank { "Not Provided" })
                if (student.motherName.isNotBlank()) {
                    InfoRow(icon = Icons.Default.FavoriteBorder, label = "Mother's Name", value = student.motherName)
                }

                // Mobile with Dial action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mobile Phone", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        Text(student.mobileNo.ifBlank { "Not Provided" }, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                    }
                    if (student.mobileNo.isNotBlank()) {
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.mobileNo}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (student.email.isNotBlank()) {
                    InfoRow(icon = Icons.Default.Email, label = "Email Address", value = student.email)
                }

                if (student.registrationNo.isNotBlank()) {
                    InfoRow(icon = Icons.Default.Badge, label = "Reg. Number", value = student.registrationNo)
                }
            }
        }

        // Subjects Enrolled Card
        if (student.subjects.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Enrolled Academic Subjects",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val subjectList = student.subjects.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjectList.forEach { subj ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = subj,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons Row
        Button(
            onClick = onNavigateToIdCard,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
        ) {
            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Generate ID Card / Admit Card", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// =========================================================================
// SECTION 1: ROUTINE / DYNAMIC TIMETABLE
// =========================================================================
@Composable
private fun StudentRoutineSection(
    student: Student,
    timetable: List<TimetablePeriod>,
    todayDayOfWeek: String,
    onOpenTimetableStudio: () -> Unit
) {
    val days = DynamicTimetableEngine.DAYS
    var selectedDay by remember { mutableStateOf(if (todayDayOfWeek in days) todayDayOfWeek else "MONDAY") }

    val dayPeriods = remember(timetable, selectedDay) {
        timetable.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
            .sortedBy { it.startTime }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Day selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            days.take(6).forEach { day ->
                val isSelected = selectedDay.equals(day, ignoreCase = true)
                val isToday = todayDayOfWeek.equals(day, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedDay = day },
                    label = {
                        Text(
                            text = if (isToday) "${day.take(3)} (Today)" else day.take(3),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE11D48),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (dayPeriods.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No scheduled classes for $selectedDay",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "Batch: ${student.className.ifBlank { "Class 12th" }}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dayPeriods, key = { it.id }) { period ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.width(100.dp)) {
                                Text(
                                    text = period.startTime,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = period.endTime,
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            VerticalDivider(modifier = Modifier.height(30.dp), color = Color(0xFFE2E8F0))
                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = period.subject,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Faculty: ${period.teacherName} • ${period.roomNumber}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onOpenTimetableStudio,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open Timetable Studio", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// =========================================================================
// SECTION 2: ATTENDANCE
// =========================================================================
@Composable
private fun StudentAttendanceSection(
    student: Student,
    attendance: List<AttendanceRecord>,
    todayDate: String,
    todayRecord: AttendanceRecord?,
    attendancePct: Int,
    presentDays: Int,
    absentDays: Int,
    lateDays: Int,
    onMarkToday: (String) -> Unit,
    onOpenAttendanceRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Live Today Action Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Today's Attendance Status", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text(todayDate, fontSize = 10.5.sp, color = Color(0xFF64748B))
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (todayRecord?.status) {
                            "PRESENT" -> Color(0xFFF0FDF4)
                            "ABSENT" -> Color(0xFFFEF2F2)
                            "LATE" -> Color(0xFFFEF3C7)
                            else -> Color(0xFFF1F5F9)
                        }
                    ) {
                        Text(
                            text = todayRecord?.status ?: "NOT MARKED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (todayRecord?.status) {
                                "PRESENT" -> Color(0xFF16A34A)
                                "ABSENT" -> Color(0xFFDC2626)
                                "LATE" -> Color(0xFFD97706)
                                else -> Color(0xFF64748B)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onMarkToday("PRESENT") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Present", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onMarkToday("ABSENT") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Absent", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onMarkToday("LATE") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Late", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBox(title = "Present", count = "$presentDays", color = Color(0xFF16A34A), modifier = Modifier.weight(1f))
            StatBox(title = "Absent", count = "$absentDays", color = Color(0xFFDC2626), modifier = Modifier.weight(1f))
            StatBox(title = "Late", count = "$lateDays", color = Color(0xFFD97706), modifier = Modifier.weight(1f))
            StatBox(title = "Overall", count = "$attendancePct%", color = Color(0xFF2563EB), modifier = Modifier.weight(1f))
        }

        // Recent Logs
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Recent Attendance Logs", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(8.dp))
                if (attendance.isEmpty()) {
                    Text("No past attendance logged yet.", fontSize = 11.5.sp, color = Color(0xFF64748B))
                } else {
                    attendance.take(5).forEach { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(rec.date, fontSize = 12.sp, color = Color(0xFF334155))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (rec.status == "PRESENT") Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                            ) {
                                Text(
                                    text = rec.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rec.status == "PRESENT") Color(0xFF16A34A) else Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onOpenAttendanceRegister,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open Attendance Register", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// =========================================================================
// SECTION 3: FEES
// =========================================================================
@Composable
private fun StudentFeeSection(
    student: Student,
    fees: List<FeeRecord>,
    totalPaid: Double,
    totalFeeAssigned: Double,
    balance: Double,
    isAddingFee: Boolean,
    quickFeeAmount: String,
    quickFeeMode: String,
    onAmountChange: (String) -> Unit,
    onModeChange: (String) -> Unit,
    onToggleAdd: () -> Unit,
    onSubmitFee: () -> Unit,
    onOpenFeeTracker: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Fee Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Fee Account Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Assigned: ₹${totalFeeAssigned.toInt()} • Paid: ₹${totalPaid.toInt()}", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (balance <= 0) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                    ) {
                        Text(
                            text = if (balance <= 0) "ALL CLEARED" else "DUE: ₹${balance.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (balance <= 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Quick Payment Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Record New Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    TextButton(onClick = onToggleAdd) {
                        Text(if (isAddingFee) "Hide" else "+ Pay Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isAddingFee) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quickFeeAmount,
                        onValueChange = onAmountChange,
                        label = { Text("Amount Paid (₹)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cash", "UPI", "Cheque").forEach { mode ->
                            FilterChip(
                                selected = quickFeeMode == mode,
                                onClick = { onModeChange(mode) },
                                label = { Text(mode, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onSubmitFee,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("Confirm & Generate Receipt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Past Receipts
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Payment Receipts (${fees.size})", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(8.dp))
                if (fees.isEmpty()) {
                    Text("No fee payments recorded yet.", fontSize = 11.5.sp, color = Color(0xFF64748B))
                } else {
                    fees.forEach { fee ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("₹${fee.amountPaid.toInt()} • ${fee.paymentMode}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("${fee.receiptNo} • ${fee.paymentDate}", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFECFDF5)) {
                                Text("SUCCESS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onOpenFeeTracker,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open Fee Management Studio", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// =========================================================================
// SECTION 4: FACULTY
// =========================================================================
@Composable
private fun StudentFacultySection(
    student: Student,
    facultyList: List<com.example.data.FacultyMember>
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Assigned Faculty for ${student.className.ifBlank { "Class 12th" }}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        facultyList.forEach { f ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(f.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Subject: ${f.subject} • ${f.qualification}", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    if (f.phone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${f.phone}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 10.5.sp, color = Color(0xFF64748B))
            Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
        }
    }
}

@Composable
private fun StatBox(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(2.dp))
            Text(count, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
