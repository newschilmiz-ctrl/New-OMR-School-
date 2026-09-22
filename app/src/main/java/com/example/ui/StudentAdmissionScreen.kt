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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ui.components.PremiumButton
import com.example.ui.components.PremiumOutlinedButton
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAdmissionScreen(navController: NavController, viewModel: OmrViewModel) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var mobileNo by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var stream by remember { mutableStateOf("GENERAL") }

    val streams = listOf("GENERAL", "SCIENCE", "ARTS", "COMMERCE")
    val coachingSessions by viewModel.sessions.collectAsStateWithLifecycle()
    val coachingClasses by viewModel.classes.collectAsStateWithLifecycle()
    val coachingSubjects by viewModel.subjects.collectAsStateWithLifecycle()

    var selectedSessionId by remember { mutableStateOf("") }

    LaunchedEffect(coachingSessions) {
        if (selectedSessionId.isEmpty() && coachingSessions.isNotEmpty()) {
            selectedSessionId = coachingSessions.first().id
        }
    }

    val selectedSession = coachingSessions.find { it.id == selectedSessionId }

    // Dynamic subjects filtered by the selected session and stream
    val sessionSubjects: List<com.example.data.CoachingSubject> = remember(coachingSubjects, selectedSession, stream) {
        val forSession = if (selectedSession != null) {
            val matching = coachingSubjects.filter { subj ->
                (subj.classId.isNotEmpty() && subj.classId == selectedSession.classId) ||
                (subj.className.isNotEmpty() && subj.className.equals(selectedSession.className, ignoreCase = true))
            }
            if (matching.isNotEmpty()) matching else coachingSubjects
        } else {
            coachingSubjects
        }

        val forStream = if (stream != "GENERAL") {
            val filtered = forSession.filter { it.stream.equals(stream, ignoreCase = true) }
            if (filtered.isNotEmpty()) filtered else forSession
        } else {
            forSession
        }

        if (forStream.isNotEmpty()) forStream else coachingSubjects
    }

    var selectedSubjects by remember { mutableStateOf(setOf<String>()) }

    // When session or available subjects change, ensure selected subjects update gracefully
    LaunchedEffect(selectedSessionId) {
        if (sessionSubjects.isNotEmpty()) {
            selectedSubjects = sessionSubjects.map { it.name }.toSet()
        }
    }

    LaunchedEffect(sessionSubjects) {
        if (selectedSubjects.isEmpty() && sessionSubjects.isNotEmpty()) {
            selectedSubjects = sessionSubjects.map { it.name }.toSet()
        }
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

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var compressedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imagePath by remember { mutableStateOf("") }
    var imageSizeBytes by remember { mutableStateOf(0) }

    val imagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            val bytes = com.example.util.PhotoCompressor.compressTo20Kb(context, it)
            if (bytes.isNotEmpty()) {
                imageSizeBytes = bytes.size
                compressedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imagePath = com.example.util.PhotoCompressor.saveBytesToInternalStorage(context, bytes)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "New Student Admission",
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. PROFILE PHOTO CARD
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .border(1.5.dp, Color(0xFFE2E8F0), CircleShape)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (compressedBitmap != null) {
                            Image(
                                bitmap = compressedBitmap!!.asImageBitmap(),
                                contentDescription = "Student Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Outlined.AddAPhoto,
                                contentDescription = "Add Photo",
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (compressedBitmap != null) "Change Photo" else "Upload Student Photo",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE11D48),
                        modifier = Modifier.clickable { imagePicker.launch("image/*") }
                    )

                    if (compressedBitmap != null) {
                        val kb = imageSizeBytes / 1024.0
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(5.dp),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Text(
                                text = "⚡ Auto Converted: ${String.format("%.1f", kb)} KB (~20KB target)",
                                fontSize = 10.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Auto-converted to ~20KB for Server & OMR desk slip",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. PERSONAL DETAILS CARD
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Personal Details",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *", fontSize = 11.5.sp) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.5.sp),
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

                    // Gender Pill Selector
                    Text("Gender", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            val isSelected = gender == g
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { gender = g },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                            ) {
                                Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = g,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = fatherName,
                        onValueChange = { fatherName = it },
                        label = { Text("Father's Name *", fontSize = 11.5.sp) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.5.sp),
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

                    OutlinedTextField(
                        value = motherName,
                        onValueChange = { motherName = it },
                        label = { Text("Mother's Name", fontSize = 11.5.sp) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.5.sp),
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

                    // Date of Birth Field
                    Box(modifier = Modifier.fillMaxWidth().clickable { datePickerVisible = true }) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = {},
                            label = { Text("Date of Birth (DOB)", fontSize = 11.5.sp) },
                            textStyle = TextStyle(fontSize = 12.5.sp),
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color(0xFF0F172A),
                                disabledContainerColor = Color(0xFFF8FAFC),
                                disabledBorderColor = Color(0xFFE2E8F0),
                                disabledLabelColor = Color(0xFF64748B)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. ACADEMIC SESSION & BATCH CARD
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Select Academic Session / Batch",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Text(
                            "+ Coaching Control",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { navController.navigate(Screen.CoachingControl.route) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Subjects for admission will be filtered according to the selected batch & session.",
                        fontSize = 10.5.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (coachingSessions.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No Sessions Created Yet",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF334155)
                                    )
                                    Text(
                                        text = "Student will be enrolled in Default Batch. You can create sessions in Coaching Control anytime.",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(coachingSessions) { session ->
                                val isSelected = session.id == selectedSessionId
                                Surface(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedSessionId = session.id },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSelected) Color(0xFF16A34A) else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = session.title,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF15803D) else Color(0xFF0F172A),
                                                maxLines = 1
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = Color(0xFF16A34A),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (session.className.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isSelected) Color(0xFFDCFCE7) else Color(0xFFE2E8F0)
                                                ) {
                                                    Text(
                                                        text = session.className,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isSelected) Color(0xFF166534) else Color(0xFF475569),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            if (session.startTime.isNotBlank() && session.endTime.isNotBlank()) {
                                                Text(
                                                    text = "${session.startTime} - ${session.endTime}",
                                                    fontSize = 9.5.sp,
                                                    color = Color(0xFF64748B)
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

            Spacer(modifier = Modifier.height(10.dp))

            // 4. CONTACT DETAILS CARD
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Contact Details",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = mobileNo,
                        onValueChange = { mobileNo = it },
                        label = { Text("Mobile Number", fontSize = 11.5.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.5.sp),
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

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address", fontSize = 11.5.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.5.sp),
                        modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(10.dp))

            // 5. SESSION SUBJECTS MULTI-SELECT CARD
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Session Subjects",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (selectedSession != null)
                                    "Showing subjects for: ${selectedSession.title} (${selectedSession.className})"
                                else
                                    "Showing all coaching subjects",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = { selectedSubjects = sessionSubjects.map { it.name }.toSet() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Select All", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            }
                            TextButton(
                                onClick = { selectedSubjects = emptySet() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Clear", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stream selection
                    Text("Filter By Stream", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        streams.forEach { st ->
                            val isSelected = stream == st
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(7.dp))
                                    .clickable { stream = st },
                                shape = RoundedCornerShape(7.dp),
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = st,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected count indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap to select multiple subjects:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (selectedSubjects.isNotEmpty()) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "${selectedSubjects.size} Selected",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedSubjects.isNotEmpty()) Color(0xFF2563EB) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (sessionSubjects.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "No subjects found for this session / stream.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Go to Coaching Control -> Add Subject or Manage Subjects to configure subjects for this session.",
                                    fontSize = 10.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sessionSubjects.chunked(2).forEach { rowSubjects ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowSubjects.forEach { subj ->
                                        val isSelected = selectedSubjects.contains(subj.name)
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedSubjects = if (isSelected) {
                                                        selectedSubjects - subj.name
                                                    } else {
                                                        selectedSubjects + subj.name
                                                    }
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF94A3B8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(7.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = subj.name,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF1E293B),
                                                        maxLines = 1
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        if (subj.stream.isNotBlank()) {
                                                            Text(
                                                                text = subj.stream,
                                                                fontSize = 9.sp,
                                                                color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF64748B),
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                        if (subj.subjectCode.isNotBlank()) {
                                                            Text(
                                                                text = "• ${subj.subjectCode}",
                                                                fontSize = 9.sp,
                                                                color = Color(0xFF94A3B8)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // If odd number in row, fill empty space
                                    if (rowSubjects.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    if (selectedSubjects.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Enrolled: ${selectedSubjects.joinToString(", ")}",
                                fontSize = 10.sp,
                                color = Color(0xFF334155),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. SUBMIT BUTTON
            PremiumButton(
                onClick = {
                    val assignedSessionId = selectedSession?.id ?: ""
                    val assignedSessionName = selectedSession?.title ?: (if (coachingSessions.isEmpty()) "General Batch" else "")
                    val assignedClassName = selectedSession?.className ?: ""

                    viewModel.addStudent(
                        name = name.trim(),
                        fatherName = fatherName.trim(),
                        motherName = motherName.trim(),
                        gender = gender,
                        dob = dob,
                        mobileNo = mobileNo.trim(),
                        email = email.trim(),
                        stream = stream,
                        subjects = selectedSubjects.joinToString(", "),
                        imagePath = imagePath,
                        sessionId = assignedSessionId,
                        sessionName = assignedSessionName,
                        className = assignedClassName
                    ) {
                        Toast.makeText(
                            context,
                            "Student admitted successfully in ${assignedSessionName.ifEmpty { "Session" }}!",
                            Toast.LENGTH_SHORT
                        ).show()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = name.isNotBlank() && fatherName.isNotBlank() && selectedSubjects.isNotEmpty(),
                containerColor = Color(0xFFE11D48),
                borderColor = Color(0xFFBE123C)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Complete Student Admission", fontSize = 12.5.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun compressImage(context: Context, uri: Uri): Bitmap {
    val inputStream = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream?.close()

    var width = bitmap.width
    var height = bitmap.height
    val maxSize = 800
    if (width > maxSize || height > maxSize) {
        val ratio = width.toFloat() / height.toFloat()
        if (ratio > 1) {
            width = maxSize
            height = (width / ratio).toInt()
        } else {
            height = maxSize
            width = (height * ratio).toInt()
        }
    }
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

    var quality = 90
    var stream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

    while (stream.toByteArray().size > 100 * 1024 && quality > 10) {
        stream.reset()
        quality -= 10
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    }

    val compressedBytes = stream.toByteArray()
    return BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
}

fun saveImageToInternalStorage(context: Context, bitmap: Bitmap): String {
    val filename = "student_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, filename)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
    return file.absolutePath
}
