package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
    val availableSubjects = listOf("Mathematics", "Science", "Physics", "Chemistry", "Biology", "English", "Social Studies")
    var selectedSubjects by remember { mutableStateOf(setOf("Mathematics", "Science", "English")) }

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
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. PROFILE PHOTO CARD
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .border(2.dp, Color(0xFFE2E8F0), CircleShape)
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
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (compressedBitmap != null) "Change Photo" else "Upload Student Photo",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE11D48),
                        modifier = Modifier.clickable { imagePicker.launch("image/*") }
                    )

                    if (compressedBitmap != null) {
                        val kb = imageSizeBytes / 1024.0
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Text(
                                text = "⚡ Auto Converted: ${String.format("%.1f", kb)} KB (Target: ~20KB)",
                                fontSize = 11.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Auto-converted to ~20KB for Server & OMR desk slip",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. PERSONAL DETAILS CARD
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Personal Details",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gender Pill Selector
                    Text("Gender", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            val isSelected = gender == g
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { gender = g },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                            ) {
                                Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = g,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = fatherName,
                        onValueChange = { fatherName = it },
                        label = { Text("Father's Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = motherName,
                        onValueChange = { motherName = it },
                        label = { Text("Mother's Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date of Birth Field
                    Box(modifier = Modifier.fillMaxWidth().clickable { datePickerVisible = true }) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = {},
                            label = { Text("Date of Birth (DOB)") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar", tint = Color(0xFF64748B)) },
                            shape = RoundedCornerShape(10.dp),
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

            Spacer(modifier = Modifier.height(14.dp))

            // 3. CONTACT & ACADEMICS CARD
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Contact & Academics",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = mobileNo,
                        onValueChange = { mobileNo = it },
                        label = { Text("Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stream selection
                    Text("Academic Stream", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        streams.forEach { st ->
                            val isSelected = stream == st
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { stream = st },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = st,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Subjects Multi-select Chips
                    Text("Enrolled Subjects (Used for Auto OMR Mapping)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableSubjects.chunked(3).forEach { rowSubjects ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowSubjects.forEach { subj ->
                                    val isSelected = selectedSubjects.contains(subj)
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                selectedSubjects = if (isSelected) {
                                                    selectedSubjects - subj
                                                } else {
                                                    selectedSubjects + subj
                                                }
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = subj,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF0369A1) else Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. SUBMIT BUTTON
            PremiumButton(
                onClick = {
                    viewModel.addStudent(
                        name = name,
                        fatherName = fatherName,
                        motherName = motherName,
                        gender = gender,
                        dob = dob,
                        mobileNo = mobileNo,
                        email = email,
                        stream = stream,
                        subjects = selectedSubjects.joinToString(", "),
                        imagePath = imagePath
                    ) {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = name.isNotBlank() && fatherName.isNotBlank(),
                containerColor = Color(0xFFE11D48),
                borderColor = Color(0xFFBE123C)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete Student Admission")
            }

            Spacer(modifier = Modifier.height(30.dp))
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
