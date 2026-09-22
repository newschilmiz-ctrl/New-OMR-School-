package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.Exam
import com.example.ui.components.PremiumButton
import com.example.ui.components.PremiumOutlinedButton
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: OmrViewModel) {
    val context = LocalContext.current
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val isLoadingExams by viewModel.isLoadingExams.collectAsStateWithLifecycle()
    val coachingClasses by viewModel.classes.collectAsStateWithLifecycle()
    val coachingSubjects by viewModel.subjects.collectAsStateWithLifecycle()
    val coachingSessions by viewModel.sessions.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryTab by remember { mutableStateOf(0) }
    var selectedSubjectFilter by remember { mutableStateOf("All") }
    var showInstitutionDialog by remember { mutableStateOf(false) }
    var currentInstitution by remember { mutableStateOf("St. Xavier's Academy • Grade 10-A") }
    var currentBannerIndex by remember { mutableStateOf(0) }

    val categoryTabs = listOf("ALL", "COACHING", "ID & ADMIT", "FEES", "ATTENDANCE", "OMR STUDIO", "EXAMS", "LIVE SCAN", "STUDENTS", "ANSWER KEYS", "REPORTS")

    // Filter exams based on search query, category, and subject
    val filteredExams = remember(exams, searchQuery, selectedCategoryTab, selectedSubjectFilter) {
        exams.filter { exam ->
            val matchesSearch = searchQuery.isBlank() ||
                    exam.name.contains(searchQuery, ignoreCase = true) ||
                    exam.subject.contains(searchQuery, ignoreCase = true)

            val matchesSubject = selectedSubjectFilter == "All" ||
                    exam.subject.equals(selectedSubjectFilter, ignoreCase = true)

            matchesSearch && matchesSubject
        }
    }

    if (showInstitutionDialog) {
        AlertDialog(
            onDismissRequest = { showInstitutionDialog = false },
            title = {
                Text(
                    text = "Select Institution / Batch",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "St. Xavier's Academy • Grade 10-A",
                        "Delhi Public School • Grade 12-B",
                        "Kendriya Vidyalaya • Mock Batch",
                        "Allen Career Institute • NEET-01"
                    ).forEach { inst ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    currentInstitution = inst
                                    showInstitutionDialog = false
                                    Toast.makeText(context, "Switched to $inst", Toast.LENGTH_SHORT).show()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentInstitution == inst,
                                onClick = {
                                    currentInstitution = inst
                                    showInstitutionDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(inst, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInstitutionDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFFAFBFD),
        contentWindowInsets = WindowInsets.statusBars
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // 1. TOP HEADER: Institution & AI Engine Status Badge
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Location / Institution Selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showInstitutionDialog = true }
                            .padding(vertical = 2.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Institution",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentInstitution,
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Right: Cloud & MySQL Status Pill badge
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { navController.navigate(Screen.SyncSettings.route) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "☁️ Cloud & MySQL",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF1D4ED8)
                                )
                            )
                        }
                    }
                }
            }

            // 2. SEARCH ROW: Brand Logo + Pill Search Bar + Quick Icons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stylized App Emblem / Brand Icon
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_sp_logo),
                        contentDescription = "SP App Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Pill Search Input with Mic and Camera scanner inside
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(18.dp),
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
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search exams, subjects...",
                                        style = TextStyle(
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.5.sp
                                        )
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("search_exams_input")
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            // Mic icon
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Voice search listening...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            // Camera scan icon
                            IconButton(
                                onClick = {
                                    if (exams.isNotEmpty()) {
                                        navController.navigate(Screen.ScanOmr.createRoute(exams.first().id))
                                    } else {
                                        Toast.makeText(context, "Create an exam first to scan sheets", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Scan OMR",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right Actions: Notification & Profile
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "No new notifications", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp, end = 4.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                            .clickable {
                                Toast.makeText(context, "Teacher Profile: Active", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 3. HORIZONTAL CATEGORY PILL TABS
            item {
                Spacer(modifier = Modifier.height(2.dp))
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryTab,
                    containerColor = Color(0xFFFAFBFD),
                    contentColor = Color(0xFFE11D48),
                    edgePadding = 12.dp,
                    divider = { },
                    indicator = { tabPositions ->
                        if (selectedCategoryTab < tabPositions.size) {
                            val currentPos = tabPositions[selectedCategoryTab]
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(Alignment.BottomStart)
                                    .offset(x = currentPos.left)
                                    .width(currentPos.width)
                                    .height(2.dp)
                                    .padding(horizontal = 10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFE11D48))
                            )
                        }
                    }
                ) {
                    categoryTabs.forEachIndexed { index, tabTitle ->
                        Tab(
                            selected = selectedCategoryTab == index,
                            onClick = {
                                selectedCategoryTab = index
                                when (tabTitle) {
                                    "COACHING" -> navController.navigate(Screen.CoachingControl.route)
                                    "ID & ADMIT" -> navController.navigate(Screen.StudentCards.createRoute("ALL"))
                                    "FEES" -> navController.navigate(Screen.FeeTracker.route)
                                    "ATTENDANCE" -> navController.navigate(Screen.AttendanceRegister.route)
                                    "OMR STUDIO" -> navController.navigate(Screen.CustomOmrDesigner.route)
                                    "LIVE SCAN" -> {
                                        if (exams.isNotEmpty()) {
                                            navController.navigate(Screen.ScanOmr.createRoute(exams.first().id))
                                        } else {
                                            Toast.makeText(context, "Create an exam first to start scanning", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    "STUDENTS" -> navController.navigate(Screen.Students.route)
                                    "EXAMS" -> { /* stays on filtered exams */ }
                                    "ANSWER KEYS" -> {
                                        if (exams.isNotEmpty()) {
                                            navController.navigate(Screen.ExamDashboard.createRoute(exams.first().id))
                                        }
                                    }
                                    "REPORTS" -> {
                                        if (exams.isNotEmpty()) {
                                            navController.navigate(Screen.ExamDashboard.createRoute(exams.first().id))
                                        }
                                    }
                                }
                            },
                            text = {
                                Text(
                                    text = tabTitle,
                                    style = TextStyle(
                                        fontSize = 10.5.sp,
                                        fontWeight = if (selectedCategoryTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedCategoryTab == index) Color(0xFFE11D48) else Color(0xFF64748B)
                                    )
                                )
                            }
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            }

            // 4. CATEGORY SQUIRCLES / STORIES
            item {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        SquircleCategoryItem(
                            icon = Icons.Default.AccountBalance,
                            title = "Coaching",
                            bgGradient = listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6)),
                            iconTint = Color(0xFFE11D48)
                        ) {
                            navController.navigate(Screen.CoachingControl.route)
                        }
                    }
                    item {
                        SquircleCategoryItem(
                            icon = Icons.Default.Badge,
                            title = "ID Cards",
                            bgGradient = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)),
                            iconTint = Color(0xFF2563EB)
                        ) {
                            navController.navigate(Screen.StudentCards.createRoute("ALL"))
                        }
                    }
                    item {
                        SquircleCategoryItem(
                            icon = Icons.Default.Payments,
                            title = "Fees",
                            bgGradient = listOf(Color(0xFFECFDF5), Color(0xFFA7F3D0)),
                            iconTint = Color(0xFF059669)
                        ) {
                            navController.navigate(Screen.FeeTracker.route)
                        }
                    }
                    item {
                        SquircleCategoryItem(
                            icon = Icons.Default.FactCheck,
                            title = "Attendance",
                            bgGradient = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)),
                            iconTint = Color(0xFFD97706)
                        ) {
                            navController.navigate(Screen.AttendanceRegister.route)
                        }
                    }
                    item {
                        SquircleCategoryItem(
                            icon = Icons.Default.DesignServices,
                            title = "OMR Studio",
                            bgGradient = listOf(Color(0xFFEDE9FE), Color(0xFFDDD6FE)),
                            iconTint = Color(0xFF7C3AED)
                        ) {
                            navController.navigate(Screen.CustomOmrDesigner.route)
                        }
                    }
                    item {
                        SquircleCategoryItem(
                            icon = Icons.Default.DocumentScanner,
                            title = "Live Scan",
                            bgGradient = listOf(Color(0xFFD1FAE5), Color(0xFFA7F3D0)),
                            iconTint = Color(0xFF047857)
                        ) {
                            if (exams.isNotEmpty()) {
                                navController.navigate(Screen.ScanOmr.createRoute(exams.first().id))
                            } else {
                                Toast.makeText(context, "Please create an exam first", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    item {
                        SquircleCategoryItem(
                            icon = Icons.Default.PostAdd,
                            title = "New Exam",
                            bgGradient = listOf(Color(0xFFE0E7FF), Color(0xFFC7D2FE)),
                            iconTint = Color(0xFF4338CA)
                        ) {
                            navController.navigate(Screen.CreateExam.route)
                        }
                    }
                    item {
                        SquircleCategoryItem(
                            icon = Icons.Default.PeopleAlt,
                            title = "Students",
                            bgGradient = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)),
                            iconTint = Color(0xFFB45309)
                        ) {
                            navController.navigate(Screen.Students.route)
                        }
                    }
                    item {
                        SquircleCategoryItem(
                            icon = Icons.Default.VpnKey,
                            title = "Key Sets",
                            bgGradient = listOf(Color(0xFFF3E8FF), Color(0xFFE9D5FF)),
                            iconTint = Color(0xFF7E22CE)
                        ) {
                            if (exams.isNotEmpty()) {
                                navController.navigate(Screen.ExamDashboard.createRoute(exams.first().id, tab = 1))
                            } else {
                                Toast.makeText(context, "Create an exam first", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    item {
                        SquircleCategoryItem(
                            icon = Icons.Default.Insights,
                            title = "Reports",
                            bgGradient = listOf(Color(0xFFFFE4E6), Color(0xFFFECDD3)),
                            iconTint = Color(0xFFBE123C)
                        ) {
                            if (exams.isNotEmpty()) {
                                navController.navigate(Screen.ExamDashboard.createRoute(exams.first().id, tab = 4))
                            } else {
                                Toast.makeText(context, "Create an exam first", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    item {
                        SquircleCategoryItem(
                            icon = Icons.Default.Print,
                            title = "Print PDF",
                            bgGradient = listOf(Color(0xFFCCFBF1), Color(0xFF99F6E4)),
                            iconTint = Color(0xFF0F766E)
                        ) {
                            if (exams.isNotEmpty()) {
                                navController.navigate(Screen.ExamDashboard.createRoute(exams.first().id, tab = 0))
                            } else {
                                Toast.makeText(context, "Create an exam first", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    item {
                        SquircleCategoryItem(
                            icon = Icons.Default.CloudSync,
                            title = "MySQL Sync",
                            bgGradient = listOf(Color(0xFFEFF6FF), Color(0xFFBFDBFE)),
                            iconTint = Color(0xFF1D4ED8)
                        ) {
                            navController.navigate(Screen.SyncSettings.route)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 5. PROMO / HIGHLIGHT STRIP
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF1F2),
                    border = BorderStroke(1.dp, Color(0xFFFECDD3))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Instant 0.5s Scan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE11D48)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFFFFE4E6))
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text("PRO", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE11D48))
                                }
                            }
                            Text(
                                text = "Auto-aligns corner markers & evaluates Sets A-D",
                                fontSize = 10.sp,
                                color = Color(0xFF881337)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE11D48),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (exams.isNotEmpty()) {
                                        navController.navigate(Screen.ScanOmr.createRoute(exams.first().id))
                                    } else {
                                        navController.navigate(Screen.CreateExam.route)
                                    }
                                }
                        ) {
                            Text(
                                text = "Scan Now",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // 5B. PROMO: CUSTOM OMR STUDIO (DRAG & DROP DESIGNER)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF5F3FF),
                    border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF7C3AED), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DesignServices, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Custom OMR Studio",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF4C1D95)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFFEDE9FE)) {
                                        Text(
                                            "NEW",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF6D28D9),
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Drag & drop, custom labels, bubble sizes & print",
                                    fontSize = 10.sp,
                                    color = Color(0xFF6D28D9)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF7C3AED),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    navController.navigate(Screen.CustomOmrDesigner.route)
                                }
                        ) {
                            Text(
                                text = "Design",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 5C. COACHING MAIN CONTROL BANNER
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFFE11D48), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Coaching Main Control", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFFE11D48)) {
                                        Text("NEW", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                                Text(
                                    "${coachingClasses.size} Classes • ${coachingSubjects.size} Subjects • ${coachingSessions.size} Sessions",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFE11D48),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { navController.navigate(Screen.CoachingControl.route) }
                        ) {
                            Text(
                                text = "Open Hub",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 6. HERO BANNER CAROUSEL
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Column {
                            // Top Row: Tag & Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = Color(0xFF334155)
                                ) {
                                    Text(
                                        text = "SMART OMR ENGINE",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = Color(0x33FFFFFF)
                                ) {
                                    Text(
                                        text = "99.8% ACCURACY",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Automated Exam\nGrading & Reports",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    lineHeight = 22.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Grade hundreds of physical answer sheets in minutes with real-time bubble analysis & QR student ID reading.",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 15.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Bottom row: Action button & Circular Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White)
                                        .clickable {
                                            if (exams.isNotEmpty()) {
                                                navController.navigate(Screen.ScanOmr.createRoute(exams.first().id))
                                            } else {
                                                navController.navigate(Screen.CreateExam.route)
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (exams.isNotEmpty()) "Open Live Scanner" else "Create First Exam",
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Carousel dots indicator
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    repeat(4) { idx ->
                                        Box(
                                            modifier = Modifier
                                                .size(if (idx == 0) 7.dp else 5.dp)
                                                .clip(CircleShape)
                                                .background(if (idx == 0) Color.White else Color(0xFF475569))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 7. QUICK STATS & TIPS STRIP
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pro Tip: Keep Timing Marks In View",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Ensure the black edge bars are visible for automatic 100% calibration.",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 8. HORIZONTAL QUICK FILTERS
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exam Directory",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = "${filteredExams.size} Found",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val subjects = remember(exams, coachingSubjects) {
                        val examSubjects = exams.map { it.subject }.filter { it.isNotBlank() }
                        val coachingNames = coachingSubjects.map { it.name }
                        listOf("All") + (examSubjects + coachingNames).distinct()
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(subjects) { subj ->
                            val isSelected = selectedSubjectFilter == subj
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedSubjectFilter = subj }
                            ) {
                                Text(
                                    text = subj,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 9. EXAM CATALOG FEED (Mobile-friendly modern cards)
            if (isLoadingExams) {
                items(3) {
                    SkeletonExamCard()
                }
            } else if (filteredExams.isEmpty()) {
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
                                    Icons.AutoMirrored.Filled.Assignment,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No exams match '$searchQuery'" else "No exams added yet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Create an exam to generate answer keys and start scanning OMR sheets.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            PremiumButton(
                                onClick = { navController.navigate(Screen.CreateExam.route) },
                                containerColor = Color(0xFFE11D48),
                                borderColor = Color(0xFFBE123C)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create New Exam")
                            }
                        }
                    }
                }
            } else {
                items(filteredExams) { exam ->
                    ModernExamItemCard(
                        exam = exam,
                        onScanClick = {
                            navController.navigate(Screen.ScanOmr.createRoute(exam.id))
                        },
                        onManageClick = {
                            navController.navigate(Screen.ExamDashboard.createRoute(exam.id))
                        }
                    )
                }
            }
        }
    }
}

/**
 * Visual Category Squircles like Fashion, Beauty, Home Living in user's image.
 */
@Composable
fun SquircleCategoryItem(
    icon: ImageVector,
    title: String,
    bgGradient: List<Color>,
    iconTint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(bgGradient))
                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * High-fidelity, mobile-friendly Exam Card in catalog style.
 */
@Composable
fun ModernExamItemCard(
    exam: Exam,
    onScanClick: () -> Unit,
    onManageClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onManageClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = exam.name,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = exam.subject.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            Text(
                                text = sdf.format(Date(exam.timestamp)),
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Text(
                        text = "ACTIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Info tags & Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pass: ${exam.passMarks}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Text("•", color = Color(0xFFCBD5E1), fontSize = 10.sp)
                    Text(
                        text = "Sets: A-D",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PremiumOutlinedButton(
                        onClick = onManageClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Manage", fontSize = 11.sp)
                    }

                    PremiumButton(
                        onClick = onScanClick,
                        containerColor = Color(0xFFE11D48),
                        borderColor = Color(0xFFBE123C),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SkeletonExamCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.3f).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.fillMaxWidth(0.3f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Box(modifier = Modifier.width(60.dp).height(24.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
            }
        }
    }
}
