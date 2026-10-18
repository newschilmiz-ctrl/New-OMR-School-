package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Exam
import com.example.data.ScanResult
import com.example.data.Student
import com.example.data.SubjectiveMarksRecord

@Composable
fun ResultVerificationTab(
    exam: Exam,
    students: List<Student>,
    viewModel: OmrViewModel
) {
    val context = LocalContext.current
    val scanResultsFlow = remember(exam.id) { viewModel.getScanResultsForExam(exam.id) }
    val scanResults by scanResultsFlow.collectAsStateWithLifecycle()
    val subjectiveMarks by viewModel.subjectiveMarks.collectAsStateWithLifecycle()

    val examSubjectiveMarks = remember(subjectiveMarks, exam.id) {
        subjectiveMarks.filter { it.examId == exam.id }
    }

    // Filter enrolled students for this exam subject
    val enrolledStudents = remember(students, exam.subject) {
        students.filter { exam.subject.isBlank() || it.subjects.contains(exam.subject, ignoreCase = true) }
            .ifEmpty { students }
    }

    var isPublished by remember(exam.isResultPublished) { mutableStateOf(exam.isResultPublished) }
    var showRecheckDialog by remember { mutableStateOf(false) }

    // Map results by roll (lowercase trim)
    val omrByRoll = remember(scanResults) {
        scanResults.groupBy { it.studentId.trim().lowercase() }
    }
    val subjectiveByRoll = remember(examSubjectiveMarks) {
        examSubjectiveMarks.groupBy { it.studentRoll.trim().lowercase() }
    }

    // 1. Verification Checks
    val totalEnrolled = enrolledStudents.size
    val omrEvaluatedCount = enrolledStudents.count { omrByRoll.containsKey(it.rollNo.trim().lowercase()) }
    val subjectiveEvaluatedCount = enrolledStudents.count { subjectiveByRoll.containsKey(it.rollNo.trim().lowercase()) }

    // Duplicate check
    val duplicateOmrRolls = remember(omrByRoll) {
        omrByRoll.filter { it.value.size > 1 }.keys
    }
    val duplicateSubjectiveRolls = remember(subjectiveByRoll) {
        subjectiveByRoll.filter { it.value.size > 1 }.keys
    }
    val hasDuplicates = duplicateOmrRolls.isNotEmpty() || duplicateSubjectiveRolls.isNotEmpty()

    val isOmrRequired = exam.templateType != "None"
    val isSubjectiveRequired = exam.hasSubjective

    val omrPassed = !isOmrRequired || (omrEvaluatedCount == totalEnrolled && totalEnrolled > 0)
    val subjectivePassed = !isSubjectiveRequired || (subjectiveEvaluatedCount == totalEnrolled && totalEnrolled > 0)
    val duplicatesPassed = !hasDuplicates

    val allChecksPassed = omrPassed && subjectivePassed && duplicatesPassed && totalEnrolled > 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Card: Green Signal vs Warning
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isPublished) Color(0xFF0F172A) else if (allChecksPassed) Color(0xFF052E16) else Color(0xFF451A03),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isPublished) Color(0xFF38BDF8) else if (allChecksPassed) Color(0xFF22C55E) else Color(0xFFF59E0B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPublished) Icons.Default.Verified else if (allChecksPassed) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isPublished) "RESULTS PUBLISHED ✅" else if (allChecksPassed) "GREEN SIGNAL: READY TO PUBLISH 🟢" else "VERIFICATION PENDING ⚠️",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isPublished) "Result is active and accessible by students" else if (allChecksPassed) "All evaluations complete & data consistency verified" else "Resolve missing or duplicate entries before publishing",
                                    fontSize = 10.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }

                        if (!isPublished) {
                            Button(
                                onClick = {
                                    val updated = exam.copy(isResultPublished = true)
                                    viewModel.updateExam(updated) {
                                        isPublished = true
                                        Toast.makeText(context, "Results Published Successfully! Students can now view results.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = allChecksPassed,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Publish Result", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    val updated = exam.copy(isResultPublished = false)
                                    viewModel.updateExam(updated) {
                                        isPublished = false
                                        Toast.makeText(context, "Result re-opened for teacher revisions", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Text("Re-open", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Verification Checklist Grid
        item {
            Text("AUTOMATED INTEGRITY CHECKLIST", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChecklistRow(
                    title = "Student Enrollment Check",
                    subtitle = "$totalEnrolled students registered for ${exam.subject}",
                    isPassed = totalEnrolled > 0
                )

                if (isOmrRequired) {
                    ChecklistRow(
                        title = "OMR Marks Entry Check",
                        subtitle = "$omrEvaluatedCount / $totalEnrolled OMR sheets evaluated",
                        isPassed = omrPassed
                    )
                }

                if (isSubjectiveRequired) {
                    ChecklistRow(
                        title = "Subjective Marks Entry Check",
                        subtitle = "$subjectiveEvaluatedCount / $totalEnrolled subjective copies evaluated",
                        isPassed = subjectivePassed
                    )
                }

                ChecklistRow(
                    title = "Duplicate Entry Check",
                    subtitle = if (!hasDuplicates) "No duplicate student scores found" else "Warning: Duplicate entries detected for roll(s): ${(duplicateOmrRolls + duplicateSubjectiveRolls).joinToString()}",
                    isPassed = duplicatesPassed
                )
            }
        }

        // Teacher Action Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Candidate Marks Ledger",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                OutlinedButton(
                    onClick = { showRecheckDialog = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual Recheck", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Student Score List with combined OMR + Subjective Score
        items(enrolledStudents, key = { it.rollNo }) { st ->
            val rollKey = st.rollNo.trim().lowercase()
            val omrRecord = omrByRoll[rollKey]?.firstOrNull()
            val subjRecord = subjectiveByRoll[rollKey]?.firstOrNull()

            val omrScore = omrRecord?.score ?: 0f
            val subjScore = subjRecord?.marksObtained ?: 0f
            val totalScore = omrScore + subjScore
            val isPassed = totalScore >= exam.passMarks

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(st.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))
                        Text("Roll: ${st.rollNo} • Reg: ${st.registrationNo}", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isOmrRequired) {
                                Text("OMR: ${omrScore.toInt()}", fontSize = 10.sp, color = if (omrRecord != null) Color(0xFF0284C7) else Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                            }
                            if (isSubjectiveRequired) {
                                Text("Subjective: ${subjScore.toInt()}", fontSize = 10.sp, color = if (subjRecord != null) Color(0xFF7C3AED) else Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Total: ${totalScore.toInt()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPassed) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isPassed) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                        ) {
                            Text(
                                if (isPassed) "PASS" else "FAIL",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPassed) Color(0xFF15803D) else Color(0xFFB91C1C),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Manual Recheck Modal Dialog
    if (showRecheckDialog) {
        AlertDialog(
            onDismissRequest = { showRecheckDialog = false },
            title = { Text("Teacher Manual Recheck Mode", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Review student evaluations before publishing. If any marks require revision, you can adjust subjective marks or rescan answer sheets.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF475569)
                    )
                    Text("Total Candidates: ${enrolledStudents.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Evaluated: $omrEvaluatedCount OMR / $subjectiveEvaluatedCount Subjective", fontSize = 11.sp, color = Color(0xFF16A34A))
                }
            },
            confirmButton = {
                Button(onClick = { showRecheckDialog = false }) {
                    Text("Done Reviewing")
                }
            }
        )
    }
}

@Composable
fun ChecklistRow(title: String, subtitle: String, isPassed: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (isPassed) Color(0xFFBBF7D0) else Color(0xFFFECDD3))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isPassed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isPassed) Color(0xFF16A34A) else Color(0xFFE11D48),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                Text(subtitle, fontSize = 10.sp, color = Color(0xFF64748B))
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isPassed) Color(0xFFF0FDF4) else Color(0xFFFFF1F2)
            ) {
                Text(
                    if (isPassed) "PASSED" else "ACTION REQ",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPassed) Color(0xFF15803D) else Color(0xFFE11D48),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}
