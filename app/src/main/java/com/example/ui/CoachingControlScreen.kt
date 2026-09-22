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
import androidx.compose.ui.graphics.Brush
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
import com.example.data.CoachingSession
import com.example.data.CoachingSubject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachingControlScreen(navController: NavController, viewModel: OmrViewModel) {
    val context = LocalContext.current
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Classes", "Subjects", "Sessions", "Overview")

    // Dialog States
    var showCreateClassDialog by remember { mutableStateOf(false) }
    var editingClass by remember { mutableStateOf<CoachingClass?>(null) }

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<CoachingSubject?>(null) }

    var showCreateSessionDialog by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<CoachingSession?>(null) }

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Filters for Subjects tab
    var subjectStreamFilter by remember { mutableStateOf("ALL") }
    var subjectClassFilter by remember { mutableStateOf("ALL") }

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Coaching Control Center",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFF1F2)
                                ) {
                                    Text(
                                        "MAIN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE11D48),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                "Classes • Streams • Subjects • Sessions Management",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Refresh / Reload Button
                        IconButton(
                            onClick = {
                                viewModel.loadCoachingData()
                                Toast.makeText(context, "Coaching data refreshed", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Stat Pills Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill("Classes", "${classes.size}", Color(0xFF3B82F6), Modifier.weight(1f))
                        StatPill("Subjects", "${subjects.size}", Color(0xFF10B981), Modifier.weight(1f))
                        StatPill("Sessions", "${sessions.size}", Color(0xFFF59E0B), Modifier.weight(1f))
                        StatPill("Students", "${students.size}", Color(0xFF8B5CF6), Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Primary Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = Color(0xFFE11D48),
                        divider = { HorizontalDivider(color = Color(0xFFF1F5F9)) }
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.5.sp
                                    )
                                },
                                icon = {
                                    when (index) {
                                        0 -> Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(17.dp))
                                        1 -> Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(17.dp))
                                        2 -> Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(17.dp))
                                        else -> Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(17.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ClassesTab(
                    classes = classes,
                    subjects = subjects,
                    sessions = sessions,
                    onCreateClass = { showCreateClassDialog = true },
                    onEditClass = { cls ->
                        editingClass = cls
                        showCreateClassDialog = true
                    },
                    onDeleteClass = { clsId ->
                        viewModel.deleteClass(clsId) {
                            Toast.makeText(context, "Class deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                1 -> SubjectsTab(
                    classes = classes,
                    subjects = subjects,
                    streamFilter = subjectStreamFilter,
                    classFilter = subjectClassFilter,
                    onStreamFilterChange = { subjectStreamFilter = it },
                    onClassFilterChange = { subjectClassFilter = it },
                    onAddSubject = { showAddSubjectDialog = true },
                    onEditSubject = { subj -> editingSubject = subj },
                    onDeleteSubject = { subId ->
                        viewModel.deleteSubject(subId) {
                            Toast.makeText(context, "Subject removed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onManageAllSubjects = {
                        navController.navigate(Screen.CoachingSubjects.route)
                    },
                    onResetDemo = { showResetConfirmDialog = true }
                )
                2 -> SessionsTab(
                    classes = classes,
                    sessions = sessions,
                    onCreateSession = { showCreateSessionDialog = true },
                    onEditSession = { s ->
                        editingSession = s
                        showCreateSessionDialog = true
                    },
                    onDeleteSession = { sId ->
                        viewModel.deleteSession(sId) {
                            Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                3 -> OverviewTab(
                    classes = classes,
                    subjects = subjects,
                    sessions = sessions,
                    students = students,
                    navController = navController,
                    onNavigateToTab = { selectedTab = it }
                )
            }
        }
    }

    // ==================== DIALOGS ====================

    // 1. Create/Edit Class Dialog
    if (showCreateClassDialog) {
        CreateClassDialog(
            initialClass = editingClass,
            onDismiss = {
                showCreateClassDialog = false
                editingClass = null
            },
            onSave = { name, section, desc ->
                if (editingClass != null) {
                    viewModel.updateClass(editingClass!!.copy(name = name, section = section, description = desc)) {
                        Toast.makeText(context, "Class updated", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.createClass(name, section, desc) {
                        Toast.makeText(context, "Class created successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                showCreateClassDialog = false
                editingClass = null
            }
        )
    }

    // 2. Add Subject Dialog (Select Class + Stream ARTS/SCIENCE/COMMERCE + Add Multiple Subjects)
    if (showAddSubjectDialog) {
        AddSubjectDialog(
            classes = classes,
            onDismiss = { showAddSubjectDialog = false },
            onSaveMultiple = { classId, className, stream, subjectNames ->
                viewModel.addMultipleSubjects(classId, className, stream, subjectNames) {
                    Toast.makeText(context, "${subjectNames.size} subjects added to $className ($stream)", Toast.LENGTH_SHORT).show()
                }
                showAddSubjectDialog = false
            }
        )
    }

    // 2b. Edit Subject Dialog
    if (editingSubject != null) {
        ManageSubjectEditDialog(
            subject = editingSubject!!,
            classes = classes,
            onDismiss = { editingSubject = null },
            onSave = { updatedSubject ->
                viewModel.saveSubject(updatedSubject) {
                    Toast.makeText(context, "Subject '${updatedSubject.name}' updated successfully!", Toast.LENGTH_SHORT).show()
                }
                editingSubject = null
            }
        )
    }

    // 3. Create/Edit Session Dialog (Select Class + Set Time Session)
    if (showCreateSessionDialog) {
        CreateSessionDialog(
            classes = classes,
            initialSession = editingSession,
            onDismiss = {
                showCreateSessionDialog = false
                editingSession = null
            },
            onSave = { title, classId, className, startTime, endTime, days, year ->
                if (editingSession != null) {
                    viewModel.updateSession(
                        editingSession!!.copy(
                            title = title,
                            classId = classId,
                            className = className,
                            startTime = startTime,
                            endTime = endTime,
                            days = days,
                            academicYear = year
                        )
                    ) {
                        Toast.makeText(context, "Session updated", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.createSession(
                        title = title,
                        classId = classId,
                        className = className,
                        startTime = startTime,
                        endTime = endTime,
                        days = days,
                        academicYear = year
                    ) {
                        Toast.makeText(context, "Session created successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                showCreateSessionDialog = false
                editingSession = null
            }
        )
    }

    // 4. Reset / Remove Demo Subjects Confirmation
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Update & Clean Subjects", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will remove any obsolete demo subjects and keep only your customized coaching curriculum subjects.",
                    fontSize = 13.5.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearDemoSubjects {
                            Toast.makeText(context, "Demo subjects cleaned and updated", Toast.LENGTH_SHORT).show()
                        }
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("Clean & Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatPill(title: String, count: String, accentColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                count,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                title,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )
        }
    }
}

// =========================================================================
// TAB 1: CLASSES
// =========================================================================

@Composable
fun ClassesTab(
    classes: List<CoachingClass>,
    subjects: List<CoachingSubject>,
    sessions: List<CoachingSession>,
    onCreateClass: () -> Unit,
    onEditClass: (CoachingClass) -> Unit,
    onDeleteClass: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Action Banner to Create Class
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCreateClass() },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
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
                            .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create Class",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Create New Class / Batch",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Add Class 10th, 11th, 12th, or Target competitive groups",
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF2563EB)
                    ) {
                        Text(
                            "+ ADD CLASS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "All Active Classes (${classes.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
                Text(
                    "Tap edit or delete to manage",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        if (classes.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.School,
                    title = "No Classes Created Yet",
                    message = "Tap 'Create New Class' above to define your coaching batches.",
                    buttonText = "Create First Class",
                    onClick = onCreateClass
                )
            }
        } else {
            items(classes, key = { it.id }) { cls ->
                val classSubjects = subjects.filter { it.classId == cls.id }
                val classSessions = sessions.filter { it.classId == cls.id }

                ClassItemCard(
                    coachingClass = cls,
                    subjectCount = classSubjects.size,
                    sessionCount = classSessions.size,
                    onEdit = { onEditClass(cls) },
                    onDelete = { onDeleteClass(cls.id) }
                )
            }
        }
    }
}

@Composable
fun ClassItemCard(
    coachingClass: CoachingClass,
    subjectCount: Int,
    sessionCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            coachingClass.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        if (coachingClass.section.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    coachingClass.section,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (coachingClass.description.isNotEmpty()) {
                        Text(
                            coachingClass.description,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF8FAFC))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$subjectCount Subjects Mapped", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$sessionCount Time Sessions", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 2: SUBJECTS (select class, select stream ARTS, SCIENCE, COMMERCE, add multiple)
// =========================================================================

@Composable
fun SubjectsTab(
    classes: List<CoachingClass>,
    subjects: List<CoachingSubject>,
    streamFilter: String,
    classFilter: String,
    onStreamFilterChange: (String) -> Unit,
    onClassFilterChange: (String) -> Unit,
    onAddSubject: () -> Unit,
    onEditSubject: (CoachingSubject) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onManageAllSubjects: () -> Unit,
    onResetDemo: () -> Unit
) {
    val streams = listOf("ALL", "SCIENCE", "ARTS", "COMMERCE")

    val filteredSubjects = remember(subjects, streamFilter, classFilter) {
        subjects.filter { sub ->
            val matchStream = streamFilter == "ALL" || sub.stream.equals(streamFilter, ignoreCase = true)
            val matchClass = classFilter == "ALL" || sub.classId == classFilter
            matchStream && matchClass
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Action Banner to Add Multiple Subjects
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAddSubject() },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F766E),
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
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LibraryAdd,
                            contentDescription = "Add Subjects",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Add Multiple Subjects",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Select Class • Select Stream (ARTS/SCIENCE/COMMERCE) • Add Bulk",
                            fontSize = 10.5.sp,
                            color = Color(0xFFCCFBF1)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF14B8A6)
                    ) {
                        Text(
                            "+ ADD BULK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Dedicated Button to Navigate to Manage Subjects Screen
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onManageAllSubjects() },
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFF0F766E).copy(alpha = 0.35f)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE6FFFA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Manage All Subjects Screen",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Search, view, edit or delete existing subjects",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F766E)
                    ) {
                        Text(
                            "MANAGE →",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Stream Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Stream Filter",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )

                    Text(
                        "Clean / Update",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE11D48),
                        modifier = Modifier.clickable { onResetDemo() }
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(streams) { streamName ->
                        val isSelected = streamFilter == streamName
                        val badgeColor = when (streamName) {
                            "SCIENCE" -> Color(0xFF10B981)
                            "ARTS" -> Color(0xFFF59E0B)
                            "COMMERCE" -> Color(0xFF6366F1)
                            else -> Color(0xFF64748B)
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) badgeColor else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) badgeColor else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable { onStreamFilterChange(streamName) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (streamName != "ALL") {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else badgeColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    streamName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Class Filter Selector if classes exist
        if (classes.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = classFilter == "ALL",
                            onClick = { onClassFilterChange("ALL") },
                            label = { Text("All Classes (${subjects.size})", fontSize = 10.sp) }
                        )
                    }
                    items(classes) { cls ->
                        val count = subjects.count { it.classId == cls.id }
                        FilterChip(
                            selected = classFilter == cls.id,
                            onClick = { onClassFilterChange(cls.id) },
                            label = { Text("${cls.name} ($count)", fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Configured Subjects (${filteredSubjects.size})",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569)
            )
        }

        if (filteredSubjects.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.MenuBook,
                    title = "No Subjects Configured",
                    message = "Tap 'Add Multiple Subjects' to populate subjects for this stream/class.",
                    buttonText = "+ Add Subjects",
                    onClick = onAddSubject
                )
            }
        } else {
            items(filteredSubjects, key = { it.id }) { subj ->
                SubjectItemCard(
                    subject = subj,
                    onEdit = { onEditSubject(subj) },
                    onDelete = { onDeleteSubject(subj.id) }
                )
            }
        }
    }
}

@Composable
fun SubjectItemCard(
    subject: CoachingSubject,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (streamBg, streamText) = when (subject.stream.uppercase()) {
        "SCIENCE" -> Pair(Color(0xFFECFDF5), Color(0xFF047857))
        "ARTS" -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
        "COMMERCE" -> Pair(Color(0xFFEEF2FF), Color(0xFF4338CA))
        else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(streamBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (subject.stream.uppercase()) {
                        "SCIENCE" -> Icons.Default.Science
                        "ARTS" -> Icons.Default.Palette
                        "COMMERCE" -> Icons.Default.TrendingUp
                        else -> Icons.Default.Book
                    },
                    contentDescription = null,
                    tint = streamText,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    subject.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = streamBg
                    ) {
                        Text(
                            subject.stream.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = streamText,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    if (subject.className.isNotEmpty()) {
                        Text(
                            "•  ${subject.className}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit Subject",
                    tint = Color(0xFF0F766E),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete Subject",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// =========================================================================
// TAB 3: SESSIONS (select class, set time session)
// =========================================================================

@Composable
fun SessionsTab(
    classes: List<CoachingClass>,
    sessions: List<CoachingSession>,
    onCreateSession: () -> Unit,
    onEditSession: (CoachingSession) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Action Banner to Create Session
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCreateSession() },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF7C2D12),
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
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MoreTime,
                            contentDescription = "Create Session",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Create Session & Timing",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Select Class • Set Start & End Time • Schedule Batches",
                            fontSize = 10.5.sp,
                            color = Color(0xFFFFEDD5)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEA580C)
                    ) {
                        Text(
                            "+ NEW SESSION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Active Sessions & Batch Schedules (${sessions.size})",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            )
        }

        if (sessions.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Schedule,
                    title = "No Sessions Defined",
                    message = "Create class sessions to fix batch timings (e.g. 07:00 AM - 08:30 AM).",
                    buttonText = "Set First Session",
                    onClick = onCreateSession
                )
            }
        } else {
            items(sessions, key = { it.id }) { s ->
                SessionItemCard(
                    session = s,
                    onEdit = { onEditSession(s) },
                    onDelete = { onDeleteSession(s.id) }
                )
            }
        }
    }
}

@Composable
fun SessionItemCard(
    session: CoachingSession,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF7ED)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.title.ifEmpty { "Coaching Session" },
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (session.className.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    session.className,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            session.days,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time Highlight Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timelapse,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Time Session:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }

                    Text(
                        "${session.startTime}  —  ${session.endTime}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0369A1)
                    )
                }
            }
        }
    }
}

// =========================================================================
// TAB 4: OVERVIEW
// =========================================================================

@Composable
fun OverviewTab(
    classes: List<CoachingClass>,
    subjects: List<CoachingSubject>,
    sessions: List<CoachingSession>,
    students: List<com.example.data.Student>,
    navController: NavController,
    onNavigateToTab: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Coaching Management Master Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A),
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Coaching Ecosystem", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Unified Academic & Evaluation System", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OverviewMetricCard("Classes", "${classes.size}", "Batches", Color(0xFF60A5FA), Modifier.weight(1f))
                        OverviewMetricCard("Subjects", "${subjects.size}", "Course Units", Color(0xFF34D399), Modifier.weight(1f))
                        OverviewMetricCard("Sessions", "${sessions.size}", "Active Slots", Color(0xFFFBBF24), Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Text("Quick Coaching Actions", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.School,
                    title = "Manage Classes",
                    color = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(0) }
                )
                QuickActionButton(
                    icon = Icons.Default.MenuBook,
                    title = "Add Subjects",
                    color = Color(0xFF059669),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(1) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Schedule,
                    title = "Batch Timings",
                    color = Color(0xFFD97706),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(2) }
                )
                QuickActionButton(
                    icon = Icons.Default.PersonAdd,
                    title = "Admit Student",
                    color = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.StudentAdmission.route) }
                )
            }
        }

        // Stream Distribution
        item {
            val scienceCount = subjects.count { it.stream.equals("SCIENCE", ignoreCase = true) }
            val artsCount = subjects.count { it.stream.equals("ARTS", ignoreCase = true) }
            val commerceCount = subjects.count { it.stream.equals("COMMERCE", ignoreCase = true) }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Subject Distribution by Stream", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(10.dp))

                    StreamDistributionBar("SCIENCE", scienceCount, subjects.size, Color(0xFF10B981))
                    Spacer(modifier = Modifier.height(8.dp))
                    StreamDistributionBar("ARTS", artsCount, subjects.size, Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.height(8.dp))
                    StreamDistributionBar("COMMERCE", commerceCount, subjects.size, Color(0xFF6366F1))
                }
            }
        }
    }
}

@Composable
fun OverviewMetricCard(title: String, value: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E293B)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(title, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 9.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
    }
}

@Composable
fun StreamDistributionBar(stream: String, count: Int, total: Int, color: Color) {
    val fraction = if (total > 0) (count.toFloat() / total).coerceIn(0f, 1f) else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stream, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
            Text("$count Subjects", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = Color(0xFFF1F5F9)
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                message,
                fontSize = 11.5.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Text(buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =========================================================================
// DIALOG 1: CREATE / EDIT CLASS
// =========================================================================

@Composable
fun CreateClassDialog(
    initialClass: CoachingClass?,
    onDismiss: () -> Unit,
    onSave: (name: String, section: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf(initialClass?.name ?: "") }
    var section by remember { mutableStateOf(initialClass?.section ?: "") }
    var description by remember { mutableStateOf(initialClass?.description ?: "") }

    val commonClasses = listOf("Class 10th", "Class 11th", "Class 12th", "Target NEET", "Target JEE", "Foundation Batch")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialClass == null) "Create Class" else "Edit Class",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select or enter Class name:", fontSize = 11.sp, color = Color(0xFF64748B))

                // Quick preset chips
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
                    modifier = Modifier.fillMaxWidth().testTag("class_name_input")
                )

                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Section / Batch (Optional)") },
                    placeholder = { Text("e.g. Batch A, Morning") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("class_section_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Room (Optional)") },
                    placeholder = { Text("e.g. Room 201, Board Prep") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), section.trim(), description.trim())
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier.testTag("save_class_button")
            ) {
                Text("Save Class")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// =========================================================================
// DIALOG 2: ADD SUBJECT DIALOG (Select Class, Select Stream, Add Multiple)
// =========================================================================

@Composable
fun AddSubjectDialog(
    classes: List<CoachingClass>,
    onDismiss: () -> Unit,
    onSaveMultiple: (classId: String, className: String, stream: String, subjectNames: List<String>) -> Unit
) {
    var selectedClassId by remember { mutableStateOf(classes.firstOrNull()?.id ?: "") }
    var selectedClassName by remember { mutableStateOf(classes.firstOrNull()?.name ?: "General") }

    var selectedStream by remember { mutableStateOf("SCIENCE") } // ARTS, SCIENCE, COMMERCE

    // Multi-subject selection from chips + custom text field
    val streamPresets = remember(selectedStream) {
        when (selectedStream) {
            "SCIENCE" -> listOf("Physics", "Chemistry", "Mathematics", "Biology", "Computer Science", "Information Tech")
            "ARTS" -> listOf("History", "Geography", "Political Science", "Economics", "Hindi Literature", "English Core", "Psychology", "Sociology")
            "COMMERCE" -> listOf("Accountancy", "Business Studies", "Economics", "Entrepreneurship", "Applied Mathematics", "Commercial Arts")
            else -> listOf("English", "Mathematics", "Science", "Social Studies")
        }
    }

    var selectedChips by remember { mutableStateOf(setOf<String>()) }
    var customSubjectsText by remember { mutableStateOf("") }

    // When stream changes, clear chips
    LaunchedEffect(selectedStream) {
        selectedChips = emptySet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Multiple Subjects", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. SELECT CLASS
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("1. Select Class:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    if (classes.isEmpty()) {
                        Text("No classes created yet. (Will attach to General)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(classes) { cls ->
                                val isSelected = selectedClassId == cls.id
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable {
                                        selectedClassId = cls.id
                                        selectedClassName = cls.name
                                    }
                                ) {
                                    Text(
                                        cls.name,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. SELECT STREAM (ARTS , SCIENCE, COMMERCE)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("2. Select Stream (ARTS, SCIENCE, COMMERCE):", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("SCIENCE", "ARTS", "COMMERCE").forEach { str ->
                            val isSelected = selectedStream == str
                            val (activeColor, labelText) = when (str) {
                                "SCIENCE" -> Pair(Color(0xFF10B981), "🔬 SCIENCE")
                                "ARTS" -> Pair(Color(0xFFF59E0B), "🎨 ARTS")
                                "COMMERCE" -> Pair(Color(0xFF6366F1), "💼 COMMERCE")
                                else -> Pair(Color(0xFF64748B), str)
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedStream = str },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) activeColor else activeColor.copy(alpha = 0.08f),
                                border = BorderStroke(1.5.dp, if (isSelected) activeColor else activeColor.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        labelText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else activeColor
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. SELECT MULTIPLE SUBJECTS (Presets + Input)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3. Tap Subjects to Add:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        if (selectedChips.isNotEmpty()) {
                            Text(
                                "${selectedChips.size} Selected",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }

                    // Chips multi-select
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        streamPresets.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                row.forEach { subjName ->
                                    val isSelected = selectedChips.contains(subjName)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedChips = if (isSelected) {
                                                    selectedChips - subjName
                                                } else {
                                                    selectedChips + subjName
                                                }
                                            },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                subjName,
                                                fontSize = 9.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Color(0xFF334155),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Custom subject input
                    OutlinedTextField(
                        value = customSubjectsText,
                        onValueChange = { customSubjectsText = it },
                        label = { Text("Or Type Custom (Comma Separated)") },
                        placeholder = { Text("e.g. Sanskrit, Statistics") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("custom_subjects_input")
                    )
                }
            }
        },
        confirmButton = {
            val totalToAdd = remember(selectedChips, customSubjectsText) {
                val fromCustom = customSubjectsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                (selectedChips + fromCustom).distinct()
            }

            Button(
                onClick = {
                    if (totalToAdd.isNotEmpty()) {
                        onSaveMultiple(selectedClassId, selectedClassName, selectedStream, totalToAdd)
                    }
                },
                enabled = totalToAdd.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                modifier = Modifier.testTag("save_multiple_subjects_button")
            ) {
                Text("Add ${totalToAdd.size} Subjects")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// =========================================================================
// DIALOG 3: CREATE / EDIT SESSION (Select Class, Set Time Session)
// =========================================================================

@Composable
fun CreateSessionDialog(
    classes: List<CoachingClass>,
    initialSession: CoachingSession?,
    onDismiss: () -> Unit,
    onSave: (title: String, classId: String, className: String, startTime: String, endTime: String, days: String, year: String) -> Unit
) {
    var title by remember { mutableStateOf(initialSession?.title ?: "") }
    var selectedClassId by remember { mutableStateOf(initialSession?.classId ?: (classes.firstOrNull()?.id ?: "")) }
    var selectedClassName by remember { mutableStateOf(initialSession?.className ?: (classes.firstOrNull()?.name ?: "General")) }

    var startTime by remember { mutableStateOf(initialSession?.startTime ?: "07:00 AM") }
    var endTime by remember { mutableStateOf(initialSession?.endTime ?: "08:30 AM") }
    var days by remember { mutableStateOf(initialSession?.days ?: "Mon - Sat") }
    var academicYear by remember { mutableStateOf(initialSession?.academicYear ?: "2024-2025") }

    val timeSlots = listOf("06:00 AM", "07:00 AM", "08:00 AM", "09:00 AM", "10:00 AM", "03:00 PM", "04:00 PM", "05:00 PM", "06:00 PM")
    val dayPresets = listOf("Mon - Sat", "Daily", "Mon, Wed, Fri", "Tue, Thu, Sat", "Sundays Only")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialSession == null) "Create Time Session" else "Edit Session",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Select Class
                Text("1. Select Class:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                if (classes.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(classes) { cls ->
                            val isSelected = selectedClassId == cls.id
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Color(0xFFEA580C) else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable {
                                    selectedClassId = cls.id
                                    selectedClassName = cls.name
                                }
                            ) {
                                Text(
                                    cls.name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Session Name
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session / Batch Name") },
                    placeholder = { Text("e.g. Morning Regular Batch") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("session_name_input")
                )

                // 2. Set Time Session
                Text("2. Set Time Session:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        modifier = Modifier.weight(1f).testTag("session_start_time_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        modifier = Modifier.weight(1f).testTag("session_end_time_input"),
                        singleLine = true
                    )
                }

                // Quick Start Time Pickers
                Text("Quick Timing Presets:", fontSize = 10.sp, color = Color(0xFF64748B))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(timeSlots) { slot ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (startTime == slot) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { startTime = slot }
                        ) {
                            Text(
                                slot,
                                fontSize = 9.sp,
                                color = if (startTime == slot) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Days selection
                Text("Days Schedule:", fontSize = 10.sp, color = Color(0xFF64748B))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(dayPresets) { d ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (days == d) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { days = d }
                        ) {
                            Text(
                                d,
                                fontSize = 9.5.sp,
                                color = if (days == d) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = academicYear,
                    onValueChange = { academicYear = it },
                    label = { Text("Academic Year / Session") },
                    placeholder = { Text("2024-2025") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.ifBlank { "$selectedClassName Batch ($startTime)" }
                    onSave(finalTitle, selectedClassId, selectedClassName, startTime, endTime, days, academicYear)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                modifier = Modifier.testTag("save_session_button")
            ) {
                Text("Save Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
