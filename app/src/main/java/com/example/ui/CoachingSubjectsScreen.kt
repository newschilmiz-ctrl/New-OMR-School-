package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.CoachingClass
import com.example.data.CoachingSubject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachingSubjectsScreen(
    navController: NavController,
    viewModel: OmrViewModel
) {
    val context = LocalContext.current
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStreamFilter by remember { mutableStateOf("ALL") }
    var selectedClassFilter by remember { mutableStateOf("ALL") }

    // Dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<CoachingSubject?>(null) }
    var subjectToDelete by remember { mutableStateOf<CoachingSubject?>(null) }

    val streams = listOf("ALL", "SCIENCE", "ARTS", "COMMERCE", "GENERAL")

    val filteredSubjects = remember(subjects, searchQuery, selectedStreamFilter, selectedClassFilter) {
        subjects.filter { subject ->
            val matchSearch = searchQuery.isBlank() ||
                    subject.name.contains(searchQuery, ignoreCase = true) ||
                    subject.className.contains(searchQuery, ignoreCase = true) ||
                    subject.subjectCode.contains(searchQuery, ignoreCase = true) ||
                    subject.stream.contains(searchQuery, ignoreCase = true)

            val matchStream = selectedStreamFilter == "ALL" ||
                    subject.stream.equals(selectedStreamFilter, ignoreCase = true)

            val matchClass = selectedClassFilter == "ALL" ||
                    subject.classId == selectedClassFilter

            matchSearch && matchStream && matchClass
        }.sortedWith(compareBy({ it.className }, { it.name }))
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Coaching Subjects",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "${subjects.size} Total Subjects • Edit & Delete Controls",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Add Subject Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F766E),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showAddDialog = true }
                                .testTag("add_subject_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Add Subject",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Search & Filter Header
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search subjects by name, code, or class...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("search_subjects_field"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Color(0xFF0F766E)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stream Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Stream: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(streams) { streamName ->
                                val isSelected = selectedStreamFilter == streamName
                                val count = if (streamName == "ALL") subjects.size else subjects.count { it.stream.equals(streamName, ignoreCase = true) }
                                val badgeColor = when (streamName) {
                                    "SCIENCE" -> Color(0xFF10B981)
                                    "ARTS" -> Color(0xFFF59E0B)
                                    "COMMERCE" -> Color(0xFF6366F1)
                                    "GENERAL" -> Color(0xFF0EA5E9)
                                    else -> Color(0xFF0F766E)
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) badgeColor else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) badgeColor else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { selectedStreamFilter = streamName }
                                ) {
                                    Text(
                                        text = "$streamName ($count)",
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Class Filter if classes exist
                    if (classes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Class: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedClassFilter == "ALL",
                                        onClick = { selectedClassFilter = "ALL" },
                                        label = { Text("All Classes", fontSize = 10.sp) }
                                    )
                                }
                                items(classes) { cls ->
                                    val count = subjects.count { it.classId == cls.id }
                                    FilterChip(
                                        selected = selectedClassFilter == cls.id,
                                        onClick = { selectedClassFilter = cls.id },
                                        label = { Text("${cls.name} ($count)", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Results count and quick actions bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing ${filteredSubjects.size} of ${subjects.size} subjects",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE0F2FE),
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showAddDialog = true }
                    ) {
                        Text(
                            "+ Bulk Add",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Subjects List
            if (filteredSubjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No Subjects Found",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (searchQuery.isNotEmpty()) "Try adjusting your search query or filters."
                                else "No subjects added yet. Tap '+ Add Subject' to get started.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedStreamFilter = "ALL"
                                    selectedClassFilter = "ALL"
                                    showAddDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add New Subject", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSubjects, key = { it.id }) { subject ->
                        SubjectManagementCard(
                            subject = subject,
                            onEdit = { subjectToEdit = subject },
                            onDelete = { subjectToDelete = subject }
                        )
                    }
                }
            }
        }
    }

    // Edit Subject Dialog
    if (subjectToEdit != null) {
        val subject = subjectToEdit!!
        ManageSubjectEditDialog(
            subject = subject,
            classes = classes,
            onDismiss = { subjectToEdit = null },
            onSave = { updatedSubject ->
                viewModel.saveSubject(updatedSubject) {
                    Toast.makeText(context, "Subject '${updatedSubject.name}' updated successfully!", Toast.LENGTH_SHORT).show()
                }
                subjectToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (subjectToDelete != null) {
        val subject = subjectToDelete!!
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFE4E6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.DeleteForever,
                        contentDescription = "Delete",
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    "Delete Subject?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete '${subject.name}' (${subject.stream} - ${subject.className.ifEmpty { "General" }})? This action cannot be undone.",
                    fontSize = 12.5.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val subName = subject.name
                        viewModel.deleteSubject(subject.id) {
                            Toast.makeText(context, "Subject '$subName' deleted", Toast.LENGTH_SHORT).show()
                        }
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { subjectToDelete = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
        )
    }

    // Add Subject Dialog (Single or Bulk)
    if (showAddDialog) {
        ManageSubjectAddDialog(
            classes = classes,
            initialStream = if (selectedStreamFilter != "ALL") selectedStreamFilter else "SCIENCE",
            initialClassId = if (selectedClassFilter != "ALL") selectedClassFilter else "",
            onDismiss = { showAddDialog = false },
            onAddSingle = { name, classId, className, stream, code ->
                val newSubject = CoachingSubject(
                    name = name,
                    classId = classId,
                    className = className,
                    stream = stream,
                    subjectCode = code
                )
                viewModel.saveSubject(newSubject) {
                    Toast.makeText(context, "Subject '$name' added successfully!", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            },
            onAddBulk = { classId, className, stream, names ->
                viewModel.addMultipleSubjects(classId, className, stream, names) {
                    Toast.makeText(context, "${names.size} subjects added for $stream!", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun SubjectManagementCard(
    subject: CoachingSubject,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (streamBg, streamText) = when (subject.stream.uppercase()) {
        "SCIENCE" -> Pair(Color(0xFFECFDF5), Color(0xFF047857))
        "ARTS" -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
        "COMMERCE" -> Pair(Color(0xFFEEF2FF), Color(0xFF4338CA))
        "GENERAL" -> Pair(Color(0xFFE0F2FE), Color(0xFF0369A1))
        else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
    }

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
                // Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(streamBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (subject.stream.uppercase()) {
                            "SCIENCE" -> Icons.Default.Science
                            "ARTS" -> Icons.Default.Palette
                            "COMMERCE" -> Icons.Default.TrendingUp
                            "GENERAL" -> Icons.Default.School
                            else -> Icons.Default.MenuBook
                        },
                        contentDescription = null,
                        tint = streamText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = streamBg
                        ) {
                            Text(
                                text = subject.stream.uppercase(),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = streamText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (subject.className.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = subject.className,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (subject.subjectCode.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    text = "Code: ${subject.subjectCode}",
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Action Buttons: Edit & Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Edit Button
                    FilledTonalIconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit Subject",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Delete Button
                    FilledTonalIconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFFFFF1F2),
                            contentColor = Color(0xFFE11D48)
                        )
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete Subject",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSubjectEditDialog(
    subject: CoachingSubject,
    classes: List<CoachingClass>,
    onDismiss: () -> Unit,
    onSave: (CoachingSubject) -> Unit
) {
    var name by remember { mutableStateOf(subject.name) }
    var stream by remember { mutableStateOf(subject.stream) }
    var selectedClassId by remember { mutableStateOf(subject.classId) }
    var selectedClassName by remember { mutableStateOf(subject.className) }
    var subjectCode by remember { mutableStateOf(subject.subjectCode) }

    val streams = listOf("SCIENCE", "ARTS", "COMMERCE", "GENERAL")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F766E).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Edit Subject", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("Update subject details", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name", fontSize = 11.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Stream Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Academic Stream", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        streams.forEach { st ->
                            val isSelected = stream.equals(st, ignoreCase = true)
                            val color = when (st) {
                                "SCIENCE" -> Color(0xFF10B981)
                                "ARTS" -> Color(0xFFF59E0B)
                                "COMMERCE" -> Color(0xFF6366F1)
                                else -> Color(0xFF0EA5E9)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) color else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSelected) color else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { stream = st }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = st,
                                        fontSize = 9.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }

                // Class selection if available
                if (classes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Assigned Class / Batch", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedClassId.isEmpty(),
                                    onClick = {
                                        selectedClassId = ""
                                        selectedClassName = "All Classes"
                                    },
                                    label = { Text("All Classes", fontSize = 10.sp) }
                                )
                            }
                            items(classes) { cls ->
                                FilterChip(
                                    selected = selectedClassId == cls.id,
                                    onClick = {
                                        selectedClassId = cls.id
                                        selectedClassName = cls.name
                                    },
                                    label = { Text(cls.name, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                // Subject Code
                OutlinedTextField(
                    value = subjectCode,
                    onValueChange = { subjectCode = it },
                    label = { Text("Subject Code (Optional, e.g. PHY-101)", fontSize = 11.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val updated = subject.copy(
                            name = name.trim(),
                            stream = stream,
                            classId = selectedClassId,
                            className = selectedClassName,
                            subjectCode = subjectCode.trim()
                        )
                        onSave(updated)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Changes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSubjectAddDialog(
    classes: List<CoachingClass>,
    initialStream: String,
    initialClassId: String,
    onDismiss: () -> Unit,
    onAddSingle: (name: String, classId: String, className: String, stream: String, code: String) -> Unit,
    onAddBulk: (classId: String, className: String, stream: String, names: List<String>) -> Unit
) {
    var isBulkMode by remember { mutableStateOf(false) }
    var singleName by remember { mutableStateOf("") }
    var bulkNamesText by remember { mutableStateOf("") }
    var stream by remember { mutableStateOf(initialStream) }
    var selectedClassId by remember { mutableStateOf(initialClassId) }
    var selectedClassName by remember {
        mutableStateOf(classes.find { it.id == initialClassId }?.name ?: if (initialClassId.isEmpty()) "All Classes" else "")
    }
    var subjectCode by remember { mutableStateOf("") }

    val streams = listOf("SCIENCE", "ARTS", "COMMERCE", "GENERAL")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBulkMode) "Add Multiple Subjects" else "Add New Subject",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    // Toggle Single / Bulk
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.clickable { isBulkMode = !isBulkMode }
                    ) {
                        Text(
                            text = if (isBulkMode) "Switch to Single" else "Switch to Bulk",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F766E),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = if (isBulkMode) "Enter multiple subjects separated by commas" else "Define subject name, stream, and class",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stream selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Select Stream", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        streams.forEach { st ->
                            val isSelected = stream.equals(st, ignoreCase = true)
                            val color = when (st) {
                                "SCIENCE" -> Color(0xFF10B981)
                                "ARTS" -> Color(0xFFF59E0B)
                                "COMMERCE" -> Color(0xFF6366F1)
                                else -> Color(0xFF0EA5E9)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) color else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSelected) color else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { stream = st }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = st,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }

                // Class selector
                if (classes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Select Class", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedClassId.isEmpty(),
                                    onClick = {
                                        selectedClassId = ""
                                        selectedClassName = "All Classes"
                                    },
                                    label = { Text("All Classes", fontSize = 10.sp) }
                                )
                            }
                            items(classes) { cls ->
                                FilterChip(
                                    selected = selectedClassId == cls.id,
                                    onClick = {
                                        selectedClassId = cls.id
                                        selectedClassName = cls.name
                                    },
                                    label = { Text(cls.name, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                if (!isBulkMode) {
                    // Single Subject inputs
                    OutlinedTextField(
                        value = singleName,
                        onValueChange = { singleName = it },
                        label = { Text("Subject Name (e.g. Physics, History)", fontSize = 11.5.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = subjectCode,
                        onValueChange = { subjectCode = it },
                        label = { Text("Subject Code (Optional)", fontSize = 11.5.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    // Bulk subjects input
                    OutlinedTextField(
                        value = bulkNamesText,
                        onValueChange = { bulkNamesText = it },
                        label = { Text("Subject Names (Comma-separated)", fontSize = 11.5.sp) },
                        placeholder = { Text("e.g. Physics, Chemistry, Mathematics, Biology", fontSize = 11.sp) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isBulkMode) {
                        if (singleName.isNotBlank()) {
                            onAddSingle(singleName.trim(), selectedClassId, selectedClassName, stream, subjectCode.trim())
                        }
                    } else {
                        val names = bulkNamesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        if (names.isNotEmpty()) {
                            onAddBulk(selectedClassId, selectedClassName, stream, names)
                        }
                    }
                },
                enabled = if (!isBulkMode) singleName.isNotBlank() else bulkNamesText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isBulkMode) "Add All Subjects" else "Add Subject",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }
    )
}
