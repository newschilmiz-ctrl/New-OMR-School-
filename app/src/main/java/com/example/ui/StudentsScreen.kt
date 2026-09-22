package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.Student
import com.example.ui.components.PremiumButton
import com.example.ui.components.PremiumOutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(navController: NavController, viewModel: OmrViewModel) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsStateWithLifecycle()
    val isLoadingStudents by viewModel.isLoadingStudents.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedGenderFilter by remember { mutableStateOf("All") }
    var showDeleteConfirmDialog by remember { mutableStateOf<Student?>(null) }

    // Filter students
    val filteredStudents = remember(students, searchQuery, selectedGenderFilter) {
        students.filter { student ->
            val matchesSearch = searchQuery.isBlank() ||
                    student.name.contains(searchQuery, ignoreCase = true) ||
                    student.rollNo.contains(searchQuery, ignoreCase = true) ||
                    student.fatherName.contains(searchQuery, ignoreCase = true)

            val matchesGender = selectedGenderFilter == "All" ||
                    student.gender.equals(selectedGenderFilter, ignoreCase = true)

            matchesSearch && matchesGender
        }
    }

    if (showDeleteConfirmDialog != null) {
        val student = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = {
                Text(
                    "Delete Student Record?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to remove ${student.name} (Roll: ${student.rollNo})? This will also remove their exam mapping.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteStudent(student.rollNo)
                    showDeleteConfirmDialog = null
                }) {
                    Text("Delete", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Student Directory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "${students.size} Enrolled Students",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                actions = {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFE11D48),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { navController.navigate(Screen.StudentAdmission.route) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "New Admission",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFFAFBFD)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 1. STATS OVERVIEW CARDS
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val males = students.count { it.gender.equals("Male", ignoreCase = true) }
                    val females = students.count { it.gender.equals("Female", ignoreCase = true) }

                    // Total Card
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("TOTAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${students.size}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        }
                    }

                    // Male Card
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("MALES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$males", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0284C7))
                        }
                    }

                    // Female Card
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("FEMALES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$females", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE11D48))
                        }
                    }
                }
            }

            // Quick Hub Actions Row (ID Card, Fee Tracker, Attendance)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { navController.navigate(Screen.StudentCards.createRoute("ALL")) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ID Cards", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { navController.navigate(Screen.FeeTracker.route) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fee Tracker", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { navController.navigate(Screen.AttendanceRegister.route) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Attendance", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                        }
                    }
                }
            }

            // 2. SEARCH & FILTER ROW
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(19.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search by name, roll no, father...",
                                        style = TextStyle(color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color(0xFF0F172A),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(Color(0xFFE11D48)),
                                    modifier = Modifier.fillMaxWidth().testTag("search_students_input")
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Gender filter chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "Male", "Female").forEach { filter ->
                            val isSelected = selectedGenderFilter == filter
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedGenderFilter = filter }
                            ) {
                                Text(
                                    text = filter,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // 3. STUDENT LIST
            if (isLoadingStudents) {
                items(5) {
                    SkeletonStudentCard()
                }
            } else if (filteredStudents.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No students match '$searchQuery'" else "No students enrolled yet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add students with their Roll Number and subjects to automatically generate personalized OMR answer sheets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            PremiumButton(
                                onClick = { navController.navigate(Screen.StudentAdmission.route) },
                                containerColor = Color(0xFFE11D48),
                                borderColor = Color(0xFFBE123C)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Student Now")
                            }
                        }
                    }
                }
            } else {
                items(filteredStudents, key = { it.rollNo }) { student ->
                    ModernStudentCard(
                        student = student,
                        onGenerateCard = {
                            navController.navigate(Screen.StudentCards.createRoute(student.rollNo))
                        },
                        onDelete = { showDeleteConfirmDialog = student }
                    )
                }
            }
        }
    }
}

@Composable
fun ModernStudentCard(
    student: Student,
    onGenerateCard: () -> Unit = {},
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.5.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    // Avatar with initial
                    val isMale = student.gender.equals("Male", ignoreCase = true)
                    val avatarBg = if (isMale) Color(0xFFE0F2FE) else Color(0xFFFFE4E6)
                    val avatarTint = if (isMale) Color(0xFF0284C7) else Color(0xFFE11D48)

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.name.take(1).uppercase(),
                            color = avatarTint,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(9.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = student.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = if (isMale) Color(0xFFF0F9FF) else Color(0xFFFFF1F2)
                            ) {
                                Text(
                                    text = student.gender.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMale) Color(0xFF0284C7) else Color(0xFFE11D48),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Text(
                            text = "Father: ${student.fatherName.ifBlank { "N/A" }}",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B)
                        )
                        if (student.sessionName.isNotBlank() || student.className.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (student.sessionName.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = Color(0xFFF3E8FF)
                                    ) {
                                        Text(
                                            text = student.sessionName,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF7E22CE),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                if (student.className.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = Color(0xFFEDE9FE)
                                    ) {
                                        Text(
                                            text = student.className,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF6D28D9),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onGenerateCard,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = "ID Card",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(7.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(7.dp))

            // Bottom info row (Roll, Reg, Subjects)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "Roll #${student.rollNo}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (student.registrationNo.isNotBlank()) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Reg: ${student.registrationNo}",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                if (student.subjects.isNotBlank()) {
                    Text(
                        text = student.subjects,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 130.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SkeletonStudentCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 140.dp, height = 14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE2E8F0))
                )
                Box(
                    modifier = Modifier
                        .size(width = 90.dp, height = 10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF1F5F9))
                )
            }
        }
    }
}
