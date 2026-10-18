package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdmissionFeeSetting
import com.example.data.CoachingClass
import com.example.data.CoachingSession
import com.example.data.CoachingSubject
import com.example.data.FacultyMember
import java.text.SimpleDateFormat
import java.util.*

// =========================================================================
// 1. CREATE CLASS WITH SUBJECTS AND SUBJECT CODES
// =========================================================================

data class InitialSubjectItem(
    val name: String,
    val code: String,
    val stream: String = "SCIENCE"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClassWithSubjectsDialog(
    initialClass: CoachingClass?,
    onDismiss: () -> Unit,
    onSave: (name: String, section: String, description: String, subjects: List<InitialSubjectItem>) -> Unit
) {
    var name by remember { mutableStateOf(initialClass?.name ?: "") }
    var section by remember { mutableStateOf(initialClass?.section ?: "") }
    var description by remember { mutableStateOf(initialClass?.description ?: "") }

    var subjectNameInput by remember { mutableStateOf("") }
    var subjectCodeInput by remember { mutableStateOf("") }
    var subjectStreamInput by remember { mutableStateOf("SCIENCE") }

    val initialSubjectList = remember { mutableStateListOf<InitialSubjectItem>() }

    val commonClasses = listOf("Class 10th", "Class 11th", "Class 12th", "Target NEET", "Target JEE", "Foundation Batch")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialClass == null) "Create Class with Subjects" else "Edit Class",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Class Details:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                // Presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(commonClasses) { preset ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (name == preset) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { name = preset }
                        ) {
                            Text(
                                preset,
                                fontSize = 10.sp,
                                fontWeight = if (name == preset) FontWeight.Bold else FontWeight.Normal,
                                color = if (name == preset) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Class Name *") },
                    placeholder = { Text("e.g. Class 12th") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Section / Stream") },
                        placeholder = { Text("Batch A") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Room / Note") },
                        placeholder = { Text("Hall 101") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Subject + Subject Code input
                Text("Add Subjects with Subject Codes:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("Subjects created here will be automatically mapped to this class.", fontSize = 10.sp, color = Color(0xFF64748B))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = subjectNameInput,
                        onValueChange = { subjectNameInput = it },
                        label = { Text("Subject (e.g. Physics)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = subjectCodeInput,
                        onValueChange = { subjectCodeInput = it.uppercase() },
                        label = { Text("Code (PHY-101)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("SCIENCE", "ARTS", "COMMERCE").forEach { st ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (subjectStreamInput == st) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { subjectStreamInput = st }
                            ) {
                                Text(
                                    st,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (subjectStreamInput == st) Color.White else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (subjectNameInput.isNotBlank()) {
                                val code = if (subjectCodeInput.isNotBlank()) subjectCodeInput.trim() else "${subjectNameInput.take(3).uppercase()}-101"
                                initialSubjectList.add(
                                    InitialSubjectItem(
                                        name = subjectNameInput.trim(),
                                        code = code,
                                        stream = subjectStreamInput
                                    )
                                )
                                subjectNameInput = ""
                                subjectCodeInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        enabled = subjectNameInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Add Subject", fontSize = 11.sp)
                    }
                }

                // Quick preset subject bundles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.clickable {
                            initialSubjectList.clear()
                            initialSubjectList.addAll(
                                listOf(
                                    InitialSubjectItem("Physics", "PHY-101", "SCIENCE"),
                                    InitialSubjectItem("Chemistry", "CHE-102", "SCIENCE"),
                                    InitialSubjectItem("Mathematics", "MTH-103", "SCIENCE")
                                )
                            )
                        }
                    ) {
                        Text("+ Quick PCM Bundle", fontSize = 9.sp, color = Color(0xFF1E40AF), modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.clickable {
                            initialSubjectList.clear()
                            initialSubjectList.addAll(
                                listOf(
                                    InitialSubjectItem("Physics", "PHY-101", "SCIENCE"),
                                    InitialSubjectItem("Chemistry", "CHE-102", "SCIENCE"),
                                    InitialSubjectItem("Biology", "BIO-104", "SCIENCE")
                                )
                            )
                        }
                    ) {
                        Text("+ Quick PCB Bundle", fontSize = 9.sp, color = Color(0xFF166534), modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }

                // Added subjects chips
                if (initialSubjectList.isNotEmpty()) {
                    Text("Configured Subjects (${initialSubjectList.size}):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        initialSubjectList.forEachIndexed { idx, item ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${idx + 1}. ${item.name}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDCFCE7)) {
                                            Text(item.code, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                    IconButton(
                                        onClick = { initialSubjectList.removeAt(idx) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), section.trim(), description.trim(), initialSubjectList.toList())
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Save Class & Subjects")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// =========================================================================
// 2. CREATE TEACHER WITH SUBJECT ASSIGNMENT (ALL SPECIFIED FIELDS)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeacherDialog(
    classes: List<CoachingClass>,
    subjects: List<CoachingSubject>,
    onDismiss: () -> Unit,
    onSave: (FacultyMember) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf(classes.firstOrNull()?.id ?: "") }
    var selectedClassName by remember { mutableStateOf(classes.firstOrNull()?.name ?: "") }
    var selectedSubject by remember { mutableStateOf("") }

    val filteredSubjects = remember(subjects, selectedClassId) {
        val matching = subjects.filter { it.classId == selectedClassId || (selectedClassName.isNotBlank() && it.className.equals(selectedClassName, ignoreCase = true)) }
        if (matching.isNotEmpty()) matching else subjects
    }

    LaunchedEffect(filteredSubjects) {
        if (selectedSubject.isBlank() && filteredSubjects.isNotEmpty()) {
            selectedSubject = filteredSubjects.first().name
        }
    }

    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("5 Years") }
    var idProofName by remember { mutableStateOf("Aadhaar Card") }
    var idNumber by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf("") }

    var datePickerVisible by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (datePickerVisible) {
        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        dob = sdf.format(Date(millis))
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

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bytes = com.example.util.PhotoCompressor.compressTo20Kb(context, it)
            if (bytes.isNotEmpty()) {
                photoPath = com.example.util.PhotoCompressor.saveBytesToInternalStorage(context, bytes)
                Toast.makeText(context, "Teacher Photo Uploaded (~20KB)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Teacher (Academic Roster)", fontWeight = FontWeight.Bold, fontSize = 16.5.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Class Selection
                Text("1. Select Class *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(classes) { cls ->
                        val isSel = cls.id == selectedClassId
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable {
                                selectedClassId = cls.id
                                selectedClassName = cls.name
                            }
                        ) {
                            Text(
                                cls.name,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Subject Selection
                Text("2. Select Subject *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filteredSubjects) { subj ->
                        val isSel = subj.name == selectedSubject
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0xFF10B981) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF10B981) else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable { selectedSubject = subj.name }
                        ) {
                            Text(
                                "${subj.name} (${subj.subjectCode.ifBlank { "GEN" }})",
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Teacher Personal Info
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Teacher Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // DOB Box
                    Box(modifier = Modifier.weight(1f).clickable { datePickerVisible = true }) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = {},
                            label = { Text("Date of Birth", fontSize = 11.sp) },
                            readOnly = true,
                            enabled = false,
                            trailingIcon = { Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Gender Selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gender", fontSize = 10.sp, color = Color(0xFF64748B))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Male", "Female").forEach { g ->
                                val isSel = gender == g
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSel) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { gender = g }
                                ) {
                                    Text(
                                        g,
                                        fontSize = 9.sp,
                                        color = if (isSel) Color.White else Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile No. *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email ID") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = experience,
                    onValueChange = { experience = it },
                    label = { Text("Teaching Experience (e.g. 5 Years)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ID Proof details
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = idProofName,
                        onValueChange = { idProofName = it },
                        label = { Text("ID Name (Aadhaar/PAN)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = idNumber,
                        onValueChange = { idNumber = it },
                        label = { Text("ID Number") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Passport Photo Upload
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (photoPath.isNotBlank()) {
                                    val bmp = remember(photoPath) {
                                        try { BitmapFactory.decodeFile(photoPath) } catch (e: Exception) { null }
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (photoPath.isNotBlank()) "Photo Attached ✓" else "Passport Photo", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = { photoPicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(if (photoPath.isNotBlank()) "Change" else "Upload", fontSize = 10.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedSubject.isNotBlank()) {
                        val member = FacultyMember(
                            name = name.trim(),
                            subject = selectedSubject.trim(),
                            classId = selectedClassId,
                            className = selectedClassName,
                            dob = dob,
                            gender = gender,
                            email = email.trim(),
                            phone = phone.trim(),
                            experience = experience.trim(),
                            idProofName = idProofName.trim(),
                            idNumber = idNumber.trim(),
                            photoPath = photoPath,
                            qualification = experience.ifBlank { "M.Sc, B.Ed" },
                            assignedBatches = selectedClassName.ifBlank { "All Batches" }
                        )
                        onSave(member)
                    }
                },
                enabled = name.isNotBlank() && selectedSubject.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
            ) {
                Text("Save Teacher")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// =========================================================================
// 3. CREATE SESSION OR COACHING BATCH (SELECT CLASS -> NAME -> SET TIME)
// =========================================================================

@Composable
fun CreateSessionOrBatchDialog(
    classes: List<CoachingClass>,
    isCoachingBatch: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, classId: String, className: String, startTime: String, endTime: String, isBatch: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf(classes.firstOrNull()?.id ?: "") }
    var selectedClassName by remember { mutableStateOf(classes.firstOrNull()?.name ?: "") }
    var startTime by remember { mutableStateOf("07:00 AM") }
    var endTime by remember { mutableStateOf("08:30 AM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isCoachingBatch) "Create Coaching Batch" else "Create Academic Session",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Class Selector
                Text("Select Class *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(classes) { cls ->
                        val isSel = cls.id == selectedClassId
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable {
                                selectedClassId = cls.id
                                selectedClassName = cls.name
                            }
                        ) {
                            Text(
                                cls.name,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isCoachingBatch) "Batch Name *" else "Session Name *") },
                    placeholder = { Text(if (isCoachingBatch) "e.g. Morning JEE Batch A" else "e.g. Session 2026-27") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Set Time Schedule *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), selectedClassId, selectedClassName, startTime.trim(), endTime.trim(), isCoachingBatch)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isCoachingBatch) Color(0xFFE11D48) else Color(0xFF2563EB))
            ) {
                Text(if (isCoachingBatch) "Create Batch" else "Create Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// =========================================================================
// 4. ADMISSION SETTING (SELECT BATCH/SESSION -> SELECT CLASS -> SET FEE)
// =========================================================================

@Composable
fun AdmissionSettingDialog(
    classes: List<CoachingClass>,
    sessions: List<CoachingSession>,
    onDismiss: () -> Unit,
    onSave: (AdmissionFeeSetting) -> Unit
) {
    var selectedSessionOrBatchId by remember { mutableStateOf(sessions.firstOrNull()?.id ?: "") }
    var selectedSessionOrBatchName by remember { mutableStateOf(sessions.firstOrNull()?.title ?: "") }

    var selectedClassId by remember { mutableStateOf(classes.firstOrNull()?.id ?: "") }
    var selectedClassName by remember { mutableStateOf(classes.firstOrNull()?.name ?: "") }

    var admissionFee by remember { mutableStateOf("2000") }
    var totalFee by remember { mutableStateOf("10000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Admission Fee Setting", fontWeight = FontWeight.Bold, fontSize = 16.5.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("1. Select Session / Batch *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                if (sessions.isEmpty()) {
                    Text("No sessions/batches created yet. Create a session first.", fontSize = 10.sp, color = Color(0xFFB91C1C))
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(sessions) { s ->
                            val isSel = s.id == selectedSessionOrBatchId
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
                                modifier = Modifier.clickable {
                                    selectedSessionOrBatchId = s.id
                                    selectedSessionOrBatchName = s.title
                                    if (s.classId.isNotBlank()) {
                                        selectedClassId = s.classId
                                        selectedClassName = s.className
                                    }
                                }
                            ) {
                                Text(
                                    "${if (s.isBatch) "[Batch]" else "[Session]"} ${s.title}",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Text("2. Select Class *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(classes) { cls ->
                        val isSel = cls.id == selectedClassId
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable {
                                selectedClassId = cls.id
                                selectedClassName = cls.name
                            }
                        ) {
                            Text(
                                cls.name,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text("3. Set Fee Amount *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = admissionFee,
                        onValueChange = { admissionFee = it },
                        label = { Text("Admission Fee (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalFee,
                        onValueChange = { totalFee = it },
                        label = { Text("Total Course Fee (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val adm = admissionFee.toDoubleOrNull() ?: 0.0
                    val tot = totalFee.toDoubleOrNull() ?: adm
                    onSave(
                        AdmissionFeeSetting(
                            sessionOrBatchId = selectedSessionOrBatchId,
                            sessionOrBatchName = selectedSessionOrBatchName,
                            classId = selectedClassId,
                            className = selectedClassName,
                            admissionFee = adm,
                            totalFee = tot
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
            ) {
                Text("Save Admission Fee Setting")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// =========================================================================
// 5. TEACHERS DIRECTORY TAB
// =========================================================================

@Composable
fun TeachersTab(
    faculty: List<FacultyMember>,
    onAddTeacher: () -> Unit,
    onDeleteTeacher: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Action Banner to Add Teacher
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAddTeacher() },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE11D48)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Create Teacher & Assign Subject", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Select Class • Assign Subject • Personal & ID Details", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE11D48)) {
                        Text("+ ADD TEACHER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                    }
                }
            }
        }

        item {
            Text("Registered Teachers (${faculty.size})", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
        }

        if (faculty.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Teachers Registered Yet", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Text("Create teachers and assign them subjects for their classes.", fontSize = 11.5.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onAddTeacher,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                        ) {
                            Text("+ Add Teacher", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(faculty, key = { it.id }) { teacher ->
                TeacherProfileCard(
                    teacher = teacher,
                    onDelete = { onDeleteTeacher(teacher.id) }
                )
            }
        }
    }
}

@Composable
fun TeacherProfileCard(
    teacher: FacultyMember,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFCBD5E1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (teacher.photoPath.isNotBlank()) {
                        val bmp = remember(teacher.photoPath) {
                            try { BitmapFactory.decodeFile(teacher.photoPath) } catch (e: Exception) { null }
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

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(teacher.name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDCFCE7)) {
                            Text(
                                "Subject: ${teacher.subject}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        if (teacher.className.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEFF6FF)) {
                                Text(
                                    teacher.className,
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF1E40AF),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (teacher.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(teacher.phone, fontSize = 11.sp, color = Color(0xFF475569))
                    }
                }
                if (teacher.experience.isNotBlank()) {
                    Text("Exp: ${teacher.experience}", fontSize = 10.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }
                if (teacher.idProofName.isNotBlank() && teacher.idNumber.isNotBlank()) {
                    Text("${teacher.idProofName}: ${teacher.idNumber}", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

// =========================================================================
// 6. ADMISSION FEE SETTING TAB
// =========================================================================

@Composable
fun AdmissionFeeTab(
    classes: List<CoachingClass>,
    sessions: List<CoachingSession>,
    feeSettings: List<AdmissionFeeSetting>,
    onAddSetting: () -> Unit,
    onDeleteSetting: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAddSetting() },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF065F46),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Admission Fee Setting", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Select Session / Batch • Select Class • Set Admission & Total Fee", fontSize = 10.5.sp, color = Color(0xFFD1FAE5))
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF10B981)) {
                        Text("+ SET FEE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                    }
                }
            }
        }

        item {
            Text("Configured Fee Schedules (${feeSettings.size})", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
        }

        if (feeSettings.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Fee Settings Configured", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Text("Set standard admission fees and total course fees for batches & classes.", fontSize = 11.5.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onAddSetting,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46))
                        ) {
                            Text("+ Configure Fee", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(feeSettings, key = { it.id }) { setting ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEFF6FF)) {
                                    Text(setting.className.ifBlank { "All Classes" }, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(setting.sessionOrBatchName.ifBlank { "All Sessions" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Admission Fee: ₹${setting.admissionFee.toInt()}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                Text("Total Course Fee: ₹${setting.totalFee.toInt()}", fontSize = 11.5.sp, color = Color(0xFF475569))
                            }
                        }

                        IconButton(onClick = { onDeleteSetting(setting.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

