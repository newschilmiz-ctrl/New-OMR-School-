package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Exam
import com.example.data.Student
import com.example.data.SubjectiveMarksRecord
import com.example.util.CodeGenerator
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SubjectiveTab(
    exam: Exam,
    students: List<Student>,
    viewModel: OmrViewModel
) {
    val context = LocalContext.current
    val subjectiveMarks by viewModel.subjectiveMarks.collectAsStateWithLifecycle()
    val examMarks = remember(subjectiveMarks, exam.id) {
        subjectiveMarks.filter { it.examId == exam.id }
    }

    val marksByRoll = remember(examMarks) {
        examMarks.associateBy { it.studentRoll.trim().lowercase() }
    }

    // Filter enrolled students for this exam subject
    val enrolledStudents = remember(students, exam.subject) {
        students.filter { exam.subject.isBlank() || it.subjects.contains(exam.subject, ignoreCase = true) }
            .ifEmpty { students }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, EVALUATED, PENDING
    var studentForMarksEntry by remember { mutableStateOf<Student?>(null) }
    var showDeskSlipsDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }

    val filteredList = remember(enrolledStudents, searchQuery, selectedFilter, marksByRoll) {
        enrolledStudents.filter { st ->
            val matchesSearch = st.name.contains(searchQuery, ignoreCase = true) ||
                    st.rollNo.contains(searchQuery, ignoreCase = true)
            val isEvaluated = marksByRoll.containsKey(st.rollNo.trim().lowercase())
            val matchesFilter = when (selectedFilter) {
                "EVALUATED" -> isEvaluated
                "PENDING" -> !isEvaluated
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val evaluatedCount = remember(enrolledStudents, marksByRoll) {
        enrolledStudents.count { marksByRoll.containsKey(it.rollNo.trim().lowercase()) }
    }
    val pendingCount = (enrolledStudents.size - evaluatedCount).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1B4B),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Subjective Copy Evaluation",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF6366F1)) {
                                    Text(
                                        "MAX: ${exam.subjectiveMaxMarks.toInt()} MARKS",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                "Generate desk stickers with empty marks box & Roll QR code for copy checking",
                                fontSize = 10.5.sp,
                                color = Color(0xFFC7D2FE)
                            )
                        }

                        // Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { showDeskSlipsDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Slips (A4)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showQrScannerDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x22FFFFFF)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${enrolledStudents.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Enrolled", fontSize = 9.sp, color = Color(0xFFC7D2FE))
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x22FFFFFF)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$evaluatedCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                                Text("Evaluated", fontSize = 9.sp, color = Color(0xFFC7D2FE))
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x22FFFFFF)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$pendingCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                                Text("Pending", fontSize = 9.sp, color = Color(0xFFC7D2FE))
                            }
                        }
                    }
                }
            }
        }

        // 2. Search & Filters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, roll no...", fontSize = 11.5.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                listOf("ALL", "EVALUATED", "PENDING").forEach { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.height(34.dp)
                    )
                }
            }
        }

        // 3. Students Evaluation List
        items(filteredList, key = { it.rollNo }) { st ->
            val record = marksByRoll[st.rollNo.trim().lowercase()]
            val isEvaluated = record != null

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, if (isEvaluated) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (st.imagePath.isNotBlank()) {
                                val bmp = remember(st.imagePath) {
                                    try { BitmapFactory.decodeFile(st.imagePath) } catch (e: Exception) { null }
                                }
                                if (bmp != null) {
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B))
                                }
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B))
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(st.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text("Roll: ${st.rollNo} • Reg: ${st.registrationNo}", fontSize = 10.5.sp, color = Color(0xFF64748B))
                            if (record != null && record.correctionReason.isNotBlank()) {
                                Text("Correction: ${record.correctionReason}", fontSize = 9.sp, color = Color(0xFFD97706), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (isEvaluated) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${record!!.marksObtained.toInt()} / ${exam.subjectiveMaxMarks.toInt()}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF15803D)
                                    )
                                    Text("Evaluated", fontSize = 8.sp, color = Color(0xFF15803D))
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { studentForMarksEntry = st },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        Button(
                            onClick = { studentForMarksEntry = st },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Enter Marks", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal 1: Marks Entry Dialog with Audit Trail
    studentForMarksEntry?.let { student ->
        val existingRecord = marksByRoll[student.rollNo.trim().lowercase()]
        SubjectiveMarksEntryDialog(
            student = student,
            exam = exam,
            existingRecord = existingRecord,
            onDismiss = { studentForMarksEntry = null },
            onSave = { marks, reason, teacherName ->
                val newRecord = SubjectiveMarksRecord(
                    examId = exam.id,
                    studentRoll = student.rollNo,
                    studentName = student.name,
                    subjectName = exam.subject,
                    marksObtained = marks,
                    maxMarks = exam.subjectiveMaxMarks,
                    checkedBy = teacherName.ifBlank { "Teacher" },
                    originalMarks = existingRecord?.originalMarks ?: marks,
                    finalMarks = marks,
                    correctedBy = teacherName.ifBlank { "Teacher" },
                    correctionReason = reason,
                    correctedAt = System.currentTimeMillis()
                )
                viewModel.saveSubjectiveMarks(newRecord) {
                    Toast.makeText(context, "Subjective marks saved for ${student.name}", Toast.LENGTH_SHORT).show()
                }
                studentForMarksEntry = null
            }
        )
    }

    // Modal 2: Desk Slips Printable View
    if (showDeskSlipsDialog) {
        SubjectiveDeskSlipsDialog(
            exam = exam,
            students = enrolledStudents,
            onDismiss = { showDeskSlipsDialog = false }
        )
    }

    // Modal 3: Quick QR Scan Dialog
    if (showQrScannerDialog) {
        SubjectiveQrLookupDialog(
            students = enrolledStudents,
            onDismiss = { showQrScannerDialog = false },
            onStudentSelected = { st ->
                showQrScannerDialog = false
                studentForMarksEntry = st
            }
        )
    }
}

@Composable
fun SubjectiveMarksEntryDialog(
    student: Student,
    exam: Exam,
    existingRecord: SubjectiveMarksRecord?,
    onDismiss: () -> Unit,
    onSave: (marks: Float, reason: String, teacher: String) -> Unit
) {
    var marksText by remember { mutableStateOf(existingRecord?.marksObtained?.toInt()?.toString() ?: "") }
    var reasonText by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf(existingRecord?.checkedBy ?: "Evaluator") }

    val isRevision = existingRecord != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isRevision) "Revise Subjective Marks" else "Enter Subjective Marks",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(student.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text("Roll: ${student.rollNo} • Reg: ${student.registrationNo}", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text("Subject: ${exam.subject} (Max: ${exam.subjectiveMaxMarks.toInt()})", fontSize = 11.sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.SemiBold)
                    }
                }

                if (isRevision) {
                    Text(
                        "Original Marks: ${existingRecord!!.originalMarks.toInt()}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD97706)
                    )
                }

                OutlinedTextField(
                    value = marksText,
                    onValueChange = { marksText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Marks Obtained (Max: ${exam.subjectiveMaxMarks.toInt()})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text("Checked / Evaluated By") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isRevision) {
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("Correction Reason / Audit Note *") },
                        placeholder = { Text("e.g. Recheck total marks calculation") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            val marksVal = marksText.toFloatOrNull()
            val isValid = marksVal != null && marksVal >= 0f && marksVal <= exam.subjectiveMaxMarks && (!isRevision || reasonText.isNotBlank())
            Button(
                onClick = {
                    if (marksVal != null) {
                        onSave(marksVal, reasonText, teacherName)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Text(if (isRevision) "Save Correction" else "Save Marks")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * A4 Printable Desk Slips for Subjective Exam copies:
 * Shows Student Photo, Name, Roll No, Reg No, Exam Subject,
 * an EMPTY BOX for the teacher to write marks after checking,
 * and a QR CODE linked to Roll No for online entry!
 */
@Composable
fun SubjectiveDeskSlipsDialog(
    exam: Exam,
    students: List<Student>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Subjective Copy Evaluation Slips", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Includes Student Details, Marks Entry Box & Teacher Scan QR", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(students, key = { it.rollNo }) { st ->
                        SubjectiveDeskSlipItem(exam = exam, student = st)
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectiveDeskSlipItem(exam: Exam, student: Student) {
    val qrBitmap by produceState<Bitmap?>(initialValue = null, student.rollNo, exam.id) {
        val payload = "SUBJ_EXAM|${exam.id}|${student.rollNo}|${exam.subject}"
        value = CodeGenerator.getQrCodeBitmapAsync(payload, 180)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.5.dp, Color(0xFF0F172A)),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUBJECTIVE EXAM DESK SLIP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = exam.name,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4F46E5)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color(0xFF0F172A))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Photo
                Box(
                    modifier = Modifier
                        .size(50.dp, 60.dp)
                        .border(1.dp, Color(0xFFCBD5E1))
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    if (student.imagePath.isNotBlank()) {
                        val bmp = remember(student.imagePath) {
                            try { BitmapFactory.decodeFile(student.imagePath) } catch (e: Exception) { null }
                        }
                        if (bmp != null) {
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Text("PHOTO", fontSize = 8.sp, color = Color(0xFF94A3B8))
                        }
                    } else {
                        Text("PHOTO", fontSize = 8.sp, color = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Student Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(student.name, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, maxLines = 1)
                    Text("Roll No: ${student.rollNo}", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = Color(0xFF0F172A))
                    Text("Reg No: ${student.registrationNo}", fontSize = 9.sp, color = Color(0xFF64748B))
                    Text("Subject: ${exam.subject}", fontSize = 9.sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.SemiBold)
                }

                // QR Code
                qrBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "QR Roll",
                        modifier = Modifier.size(54.dp).border(1.dp, Color(0xFFE2E8F0))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User's specified: "ek empty box isme teacher copy check ke baad marks likhega"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFF0F172A), RoundedCornerShape(4.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("TEACHER MARKS ENTRY BOX", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("Max Marks: ${exam.subjectiveMaxMarks.toInt()}", fontSize = 7.5.sp, color = Color(0xFF64748B))
                }

                // Empty Box for teacher's handwritten marks
                Box(
                    modifier = Modifier
                        .size(50.dp, 28.dp)
                        .border(1.5.dp, Color(0xFFDC2626), RoundedCornerShape(3.dp))
                        .background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("/ ${exam.subjectiveMaxMarks.toInt()}", fontSize = 9.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SubjectiveQrLookupDialog(
    students: List<Student>,
    onDismiss: () -> Unit,
    onStudentSelected: (Student) -> Unit
) {
    var manualRollInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teacher Online Marks QR Scanner", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Scan the QR code printed on the student's subjective answer copy or type Roll Number to enter marks instantly.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF475569)
                )

                OutlinedTextField(
                    value = manualRollInput,
                    onValueChange = { manualRollInput = it },
                    label = { Text("Or Enter Roll Number") },
                    placeholder = { Text("e.g. 240001") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val found = students.find { it.rollNo.trim().equals(manualRollInput.trim(), ignoreCase = true) }
                    if (found != null) {
                        onStudentSelected(found)
                    }
                },
                enabled = manualRollInput.isNotBlank()
            ) {
                Text("Open Marks Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
