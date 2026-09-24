package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.TimetablePeriod
import com.example.util.CoachingFeaturesManager
import com.example.util.DynamicTimetableEngine
import com.example.util.GenerationResult
import com.example.util.ScheduleClash
import com.example.util.SubjectRequirement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTimetableScreen(
    navController: NavController,
    initialBatch: String = "ALL"
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        CoachingFeaturesManager.init(context)
    }

    // Master state
    var timetable by remember { mutableStateOf(CoachingFeaturesManager.getTimetable()) }

    // Active View Tab: 0 = Daily Routine, 1 = Weekly Matrix, 2 = Room Occupancy, 3 = Teacher Workload
    var selectedViewTab by remember { mutableIntStateOf(0) }
    val viewTabs = listOf("Daily Cards", "Weekly Matrix", "Room Tracker", "Faculty Load")

    // Filter states
    val allBatches = remember(timetable) {
        (listOf("ALL") + timetable.map { it.batchName }.distinct()).distinct()
    }
    var selectedBatch by remember { mutableStateOf(if (initialBatch in allBatches) initialBatch else "ALL") }

    // Day selection for Daily View
    val days = DynamicTimetableEngine.DAYS
    val todayName = remember {
        SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date()).uppercase(Locale.ENGLISH)
    }
    var selectedDay by remember {
        mutableStateOf(if (todayName in days) todayName else "MONDAY")
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubjectFilter by remember { mutableStateOf("ALL") }

    // Dialogs & Sheets
    var showGeneratorDialog by remember { mutableStateOf(false) }
    var showManualPeriodDialog by remember { mutableStateOf(false) }
    var periodToEdit by remember { mutableStateOf<TimetablePeriod?>(null) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    fun refreshTimetable() {
        timetable = CoachingFeaturesManager.getTimetable()
    }

    // Detected Clashes
    val conflicts = remember(timetable) {
        DynamicTimetableEngine.findConflicts(timetable)
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
                    // Main Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Dynamic Timetable",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        text = "${timetable.size} periods",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Smart collision-free daily & weekly routine generator",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Top Action Icons
                        IconButton(
                            onClick = { showExportSheet = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = "Export Routine",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Outlined.DeleteSweep,
                                contentDescription = "Clear Routine",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // View Selector Tabs (Daily, Weekly, Room, Teacher)
                    PrimaryTabRow(
                        selectedTabIndex = selectedViewTab,
                        containerColor = Color.White,
                        contentColor = Color(0xFFE11D48),
                        divider = { HorizontalDivider(color = Color(0xFFF1F5F9)) }
                    ) {
                        viewTabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedViewTab == index,
                                onClick = { selectedViewTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedViewTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedViewTab == index) Color(0xFFE11D48) else Color(0xFF64748B)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Secondary quick add period
                SmallFloatingActionButton(
                    onClick = {
                        periodToEdit = null
                        showManualPeriodDialog = true
                    },
                    containerColor = Color.White,
                    contentColor = Color(0xFF0F172A),
                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                    modifier = Modifier.testTag("fab_quick_add_period")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Single Period", modifier = Modifier.size(18.dp))
                }

                // Primary Smart Auto-Generator FAB
                ExtendedFloatingActionButton(
                    onClick = { showGeneratorDialog = true },
                    containerColor = Color(0xFFE11D48),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_auto_generate_schedule")
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto-Generate", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Conflict Alert Bar (if clashes exist)
            if (conflicts.isNotEmpty()) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${conflicts.size} Clash Alert: ${conflicts.first().entityName} double-booked on ${conflicts.first().dayOfWeek}!",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB91C1C),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { selectedViewTab = 1 },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Resolve", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Batch selection row
            if (allBatches.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Text(
                            text = "Batch:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                    items(allBatches, key = { it }) { batch ->
                        val isSelected = selectedBatch == batch
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedBatch = batch },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Text(
                                text = if (batch == "ALL") "All Batches (${timetable.size})" else batch,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Content according to selected tab
            when (selectedViewTab) {
                0 -> DailyRoutineView(
                    timetable = timetable,
                    selectedBatch = selectedBatch,
                    selectedDay = selectedDay,
                    todayName = todayName,
                    searchQuery = searchQuery,
                    selectedSubjectFilter = selectedSubjectFilter,
                    onSelectDay = { selectedDay = it },
                    onSearchQueryChange = { searchQuery = it },
                    onSubjectFilterChange = { selectedSubjectFilter = it },
                    onEditPeriod = { p ->
                        periodToEdit = p
                        showManualPeriodDialog = true
                    },
                    onDeletePeriod = { id ->
                        CoachingFeaturesManager.deleteTimetablePeriod(id)
                        refreshTimetable()
                        Toast.makeText(context, "Period removed", Toast.LENGTH_SHORT).show()
                    },
                    onOpenGenerator = { showGeneratorDialog = true }
                )
                1 -> WeeklyMatrixView(
                    timetable = timetable,
                    selectedBatch = selectedBatch,
                    conflicts = conflicts,
                    onEditPeriod = { p ->
                        periodToEdit = p
                        showManualPeriodDialog = true
                    },
                    onDeletePeriod = { id ->
                        CoachingFeaturesManager.deleteTimetablePeriod(id)
                        refreshTimetable()
                    },
                    onOpenGenerator = { showGeneratorDialog = true }
                )
                2 -> RoomTrackerView(
                    timetable = timetable,
                    selectedDay = selectedDay,
                    onSelectDay = { selectedDay = it }
                )
                3 -> TeacherWorkloadView(
                    timetable = timetable
                )
            }
        }
    }

    // =========================================================================
    // DIALOG 1: SMART SCHEDULE GENERATOR WIZARD
    // =========================================================================
    if (showGeneratorDialog) {
        ScheduleGeneratorWizardDialog(
            existingPeriods = timetable,
            onDismiss = { showGeneratorDialog = false },
            onApplyGeneratedSchedule = { result, replaceExisting ->
                if (replaceExisting && result.periods.isNotEmpty()) {
                    val batchToClear = result.periods.first().batchName
                    CoachingFeaturesManager.clearTimetableForBatch(batchToClear)
                }
                CoachingFeaturesManager.addTimetablePeriods(result.periods)
                refreshTimetable()
                selectedBatch = result.periods.firstOrNull()?.batchName ?: selectedBatch
                showGeneratorDialog = false
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        )
    }

    // =========================================================================
    // DIALOG 2: MANUAL ADD / EDIT PERIOD DIALOG
    // =========================================================================
    if (showManualPeriodDialog) {
        ManualPeriodDialog(
            period = periodToEdit,
            existingPeriods = timetable,
            defaultBatch = if (selectedBatch != "ALL") selectedBatch else "Class 12 - JEE Advanced",
            defaultDay = selectedDay,
            onDismiss = {
                showManualPeriodDialog = false
                periodToEdit = null
            },
            onSave = { updated ->
                if (periodToEdit != null) {
                    CoachingFeaturesManager.updateTimetablePeriod(updated)
                    Toast.makeText(context, "Period updated", Toast.LENGTH_SHORT).show()
                } else {
                    CoachingFeaturesManager.addTimetablePeriod(updated)
                    Toast.makeText(context, "Period scheduled", Toast.LENGTH_SHORT).show()
                }
                refreshTimetable()
                showManualPeriodDialog = false
                periodToEdit = null
            }
        )
    }

    // =========================================================================
    // DIALOG 3: EXPORT / SHARE BOTTOM SHEET
    // =========================================================================
    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            containerColor = Color.White
        ) {
            val shareText = remember(timetable, selectedBatch) {
                DynamicTimetableEngine.exportTimetableAsShareText(timetable, selectedBatch)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Timetable Routine",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            text = shareText,
                            fontSize = 11.sp,
                            color = Color(0xFF334155),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Timetable", shareText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            showExportSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Text", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Class Routine Timetable")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Routine"))
                            showExportSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share via App", fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // =========================================================================
    // DIALOG 4: CLEAR CONFIRMATION
    // =========================================================================
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text("Clear Timetable Routine?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = if (selectedBatch == "ALL") {
                        "This will delete ALL scheduled periods across all batches. This action cannot be undone."
                    } else {
                        "This will delete all scheduled periods for '$selectedBatch'."
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedBatch == "ALL") {
                            CoachingFeaturesManager.clearAllTimetable()
                        } else {
                            CoachingFeaturesManager.clearTimetableForBatch(selectedBatch)
                        }
                        refreshTimetable()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Timetable cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Clear Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
// SUB-VIEW 1: DAILY ROUTINE (Scrollable Card Format)
// =============================================================================
@Composable
fun DailyRoutineView(
    timetable: List<TimetablePeriod>,
    selectedBatch: String,
    selectedDay: String,
    todayName: String,
    searchQuery: String,
    selectedSubjectFilter: String,
    onSelectDay: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSubjectFilterChange: (String) -> Unit,
    onEditPeriod: (TimetablePeriod) -> Unit,
    onDeletePeriod: (String) -> Unit,
    onOpenGenerator: () -> Unit
) {
    val days = DynamicTimetableEngine.DAYS

    // Filter day periods
    val dayPeriods = remember(timetable, selectedDay, selectedBatch, searchQuery, selectedSubjectFilter) {
        timetable.filter { p ->
            p.dayOfWeek.equals(selectedDay, ignoreCase = true) &&
                    (selectedBatch == "ALL" || p.batchName.equals(selectedBatch, ignoreCase = true)) &&
                    (selectedSubjectFilter == "ALL" || p.subject.equals(selectedSubjectFilter, ignoreCase = true)) &&
                    (searchQuery.isEmpty() ||
                            p.subject.contains(searchQuery, ignoreCase = true) ||
                            p.teacherName.contains(searchQuery, ignoreCase = true) ||
                            p.roomNumber.contains(searchQuery, ignoreCase = true))
        }.sortedBy { it.startTime }
    }

    // Available subjects for chips
    val subjectsInDay = remember(timetable, selectedDay) {
        listOf("ALL") + timetable
            .filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
            .map { it.subject }
            .distinct()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Day of Week Horizontal Ribbon
        item {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(days, key = { it }) { day ->
                        val isSelected = selectedDay == day
                        val isToday = day == todayName
                        val count = timetable.count {
                            it.dayOfWeek.equals(day, ignoreCase = true) &&
                                    (selectedBatch == "ALL" || it.batchName.equals(selectedBatch, ignoreCase = true))
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectDay(day) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFF0F172A) else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF0F172A) else if (isToday) Color(0xFFE11D48) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = day.take(3),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$count cls",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                    if (isToday) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color(0xFFFDA4AF) else Color(0xFFE11D48))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Day Status & Stats Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFE11D48), Color(0xFFBE123C))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedDay,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            if (selectedDay == todayName) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFE4E6)
                                ) {
                                    Text(
                                        text = "TODAY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE11D48),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${dayPeriods.size} lectures scheduled • ${if (selectedBatch == "ALL") "All Batches" else selectedBatch}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Generator quick link
                    FilledTonalButton(
                        onClick = onOpenGenerator,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Subject Filter Chips (if more than 1 subject exists today)
        if (subjectsInDay.size > 2) {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(subjectsInDay, key = { it }) { sub ->
                        val isSel = selectedSubjectFilter == sub
                        FilterChip(
                            selected = isSel,
                            onClick = { onSubjectFilterChange(sub) },
                            label = { Text(sub, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE11D48),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // 4. Scrollable Period Cards List
        if (dayPeriods.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF1F2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Classes on $selectedDay",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No lecture periods have been scheduled for $selectedDay for ${if (selectedBatch == "ALL") "any batch" else selectedBatch}.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onOpenGenerator,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Full Schedule", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(dayPeriods, key = { it.id }) { period ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DynamicPeriodCard(
                        period = period,
                        onEdit = { onEditPeriod(period) },
                        onDelete = { onDeletePeriod(period.id) }
                    )
                }
            }
        }
    }
}

// =============================================================================
// CLEAN SCROLLABLE CARD FORMAT FOR EACH PERIOD
// =============================================================================
@Composable
fun DynamicPeriodCard(
    period: TimetablePeriod,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val subjectAccentColor = remember(period.subject) {
        when {
            period.subject.contains("Physics", ignoreCase = true) -> Color(0xFF2563EB)
            period.subject.contains("Chemistry", ignoreCase = true) -> Color(0xFF059669)
            period.subject.contains("Math", ignoreCase = true) -> Color(0xFFD97706)
            period.subject.contains("Bio", ignoreCase = true) -> Color(0xFF0D9488)
            period.subject.contains("English", ignoreCase = true) -> Color(0xFF7C3AED)
            period.subject.contains("Science", ignoreCase = true) -> Color(0xFF0284C7)
            period.subject.contains("Reasoning", ignoreCase = true) || period.subject.contains("Aptitude", ignoreCase = true) -> Color(0xFFEA580C)
            else -> Color(0xFFE11D48)
        }
    }

    val subjectIcon = remember(period.subject) {
        when {
            period.subject.contains("Physics", ignoreCase = true) -> Icons.Default.Science
            period.subject.contains("Chemistry", ignoreCase = true) -> Icons.Default.Biotech
            period.subject.contains("Math", ignoreCase = true) -> Icons.Default.Calculate
            period.subject.contains("Bio", ignoreCase = true) -> Icons.Default.Spa
            period.subject.contains("English", ignoreCase = true) -> Icons.Default.MenuBook
            else -> Icons.Default.School
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Time Column
            Column(
                modifier = Modifier.width(84.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = period.startTime,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "to ${period.endTime}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = "Lecture",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 2. Colored Pillar Divider
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(subjectAccentColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 3. Main Details
            Column(modifier = Modifier.weight(1f)) {
                // Subject title with subject icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        subjectIcon,
                        contentDescription = null,
                        tint = subjectAccentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = period.subject,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Teacher with avatar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = period.teacherName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Room and Batch tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Room Tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(0.5.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = period.roomNumber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    // Batch Tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = period.batchName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 4. Quick Actions (Edit, Delete)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit Class",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete Class",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// SUB-VIEW 2: WEEKLY MATRIX (Grid of Days & Periods)
// =============================================================================
@Composable
fun WeeklyMatrixView(
    timetable: List<TimetablePeriod>,
    selectedBatch: String,
    conflicts: List<ScheduleClash>,
    onEditPeriod: (TimetablePeriod) -> Unit,
    onDeletePeriod: (String) -> Unit,
    onOpenGenerator: () -> Unit
) {
    val workingDays = DynamicTimetableEngine.WORKING_DAYS
    val filtered = remember(timetable, selectedBatch) {
        if (selectedBatch == "ALL") timetable else timetable.filter { it.batchName.equals(selectedBatch, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Clash detection banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (conflicts.isEmpty()) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                ),
                border = BorderStroke(
                    1.dp,
                    if (conflicts.isEmpty()) Color(0xFFBBF7D0) else Color(0xFFFECACA)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (conflicts.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (conflicts.isEmpty()) Color(0xFF16A34A) else Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (conflicts.isEmpty()) "100% Collision-Free Routine" else "${conflicts.size} Schedule Conflicts Detected",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (conflicts.isEmpty()) Color(0xFF15803D) else Color(0xFF991B1B)
                        )
                        Text(
                            text = if (conflicts.isEmpty())
                                "No teacher or room is double-booked across batches."
                            else
                                "Multiple classes overlap at the same room or teacher. Review below.",
                            fontSize = 11.5.sp,
                            color = if (conflicts.isEmpty()) Color(0xFF166534) else Color(0xFFB91C1C)
                        )
                    }
                }
            }
        }

        // Each Day Accordion / Card
        items(workingDays, key = { it }) { day ->
            val dayLectures = filtered.filter { it.dayOfWeek.equals(day, ignoreCase = true) }.sortedBy { it.startTime }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Day Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A)
                        ) {
                            Text(
                                text = day.take(3),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = day,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${dayLectures.size} periods",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (dayLectures.isEmpty()) {
                        Text(
                            text = "No periods scheduled for this day.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        dayLectures.forEachIndexed { idx, p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onEditPeriod(p) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = p.startTime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.width(68.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.subject,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${p.teacherName} • ${p.roomNumber}",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (selectedBatch == "ALL") {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFF1F5F9)
                                    ) {
                                        Text(
                                            text = p.batchName.take(12),
                                            fontSize = 9.sp,
                                            color = Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(onClick = { onDeletePeriod(p.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Delete", tint = Color(0xFFCBD5E1), modifier = Modifier.size(13.dp))
                                }
                            }
                            if (idx < dayLectures.size - 1) {
                                HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// SUB-VIEW 3: ROOM OCCUPANCY TRACKER
// =============================================================================
@Composable
fun RoomTrackerView(
    timetable: List<TimetablePeriod>,
    selectedDay: String,
    onSelectDay: (String) -> Unit
) {
    val rooms = remember(timetable) {
        val fromTimetable = timetable.map { it.roomNumber }.distinct()
        (fromTimetable + DynamicTimetableEngine.DEFAULT_ROOMS).distinct()
    }

    val occupancyList = remember(timetable) {
        DynamicTimetableEngine.calculateRoomOccupancy(timetable)
    }

    val dayLectures = remember(timetable, selectedDay) {
        timetable.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Day selector for room view
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(DynamicTimetableEngine.DAYS, key = { it }) { day ->
                    val isSel = selectedDay == day
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectDay(day) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) Color(0xFF0F172A) else Color.White,
                        border = BorderStroke(1.dp, if (isSel) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = day.take(3),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.White else Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Room Availability on $selectedDay",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        items(rooms, key = { it }) { room ->
            val busyInThisRoom = dayLectures.filter { it.roomNumber.equals(room, ignoreCase = true) }.sortedBy { it.startTime }
            val roomStat = occupancyList.find { it.roomNumber.equals(room, ignoreCase = true) }
            val totalAllWeek = roomStat?.totalPeriods ?: 0

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = room,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${busyInThisRoom.size} classes today • $totalAllWeek total periods/week",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (busyInThisRoom.isEmpty()) Color(0xFFF0FDF4) else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = if (busyInThisRoom.isEmpty()) "VACANT ALL DAY" else "${busyInThisRoom.size} OCCUPIED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (busyInThisRoom.isEmpty()) Color(0xFF16A34A) else Color(0xFFD97706),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (busyInThisRoom.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        busyInThisRoom.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${p.startTime} - ${p.endTime}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.width(130.dp)
                                )
                                Text(
                                    text = "${p.subject} (${p.batchName})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// SUB-VIEW 4: TEACHER WORKLOAD ANALYTICS
// =============================================================================
@Composable
fun TeacherWorkloadView(
    timetable: List<TimetablePeriod>
) {
    val workloadList = remember(timetable) {
        DynamicTimetableEngine.calculateTeacherWorkload(timetable)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Faculty Teaching Load Analysis",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tracks periods per teacher to avoid burnout and balance routine distribution.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        if (workloadList.isEmpty()) {
            item {
                Text(
                    text = "No timetable records found. Use Auto-Generate to build a routine.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        } else {
            items(workloadList, key = { it.teacherName }) { teacher ->
                val progress = (teacher.totalPeriods / 30f).coerceIn(0f, 1f)
                val statusColor = when {
                    teacher.totalPeriods > 24 -> Color(0xFFDC2626) // Heavy load
                    teacher.totalPeriods >= 12 -> Color(0xFF16A34A) // Balanced load
                    else -> Color(0xFF2563EB) // Light load
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = teacher.teacherName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = teacher.subjects.joinToString(", ").ifEmpty { "General" },
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${teacher.totalPeriods} periods",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                                Text(
                                    text = "${teacher.daysActive.size} days active",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = statusColor,
                            trackColor = Color(0xFFF1F5F9)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// DIALOG COMPONENT: SCHEDULE GENERATOR WIZARD
// =============================================================================
@Composable
fun ScheduleGeneratorWizardDialog(
    existingPeriods: List<TimetablePeriod>,
    onDismiss: () -> Unit,
    onApplyGeneratedSchedule: (GenerationResult, Boolean) -> Unit
) {
    var selectedPreset by remember { mutableStateOf("JEE Advanced (PCM)") }
    val presets = listOf("JEE Advanced (PCM)", "NEET Medical (PCB)", "Foundation Class 10", "Custom Evening")

    val initialConfig = remember(selectedPreset) {
        DynamicTimetableEngine.getPresetRequirements(selectedPreset)
    }

    var batchName by remember { mutableStateOf(initialConfig.first) }
    var subjects by remember { mutableStateOf(initialConfig.second.toMutableList()) }
    var replaceExistingBatch by remember { mutableStateOf(true) }
    var shiftType by remember { mutableStateOf("Morning (08:00 AM - 01:40 PM)") }

    // When preset changes, update batch and subjects
    LaunchedEffect(selectedPreset) {
        val config = DynamicTimetableEngine.getPresetRequirements(selectedPreset)
        batchName = config.first
        subjects = config.second.toMutableList()
    }

    // Generated preview holder
    var previewResult by remember { mutableStateOf<GenerationResult?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dynamic Schedule Generator",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Automatically creates a collision-free routine ensuring no teacher or room overlaps across batches.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B)
                )

                // 1. Preset Selector
                Text("Select Academic Stream Preset:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset },
                            label = { Text(preset, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0F172A),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // 2. Batch Name
                OutlinedTextField(
                    value = batchName,
                    onValueChange = { batchName = it },
                    label = { Text("Target Batch Name", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Subject Allocations Preview
                Text(
                    text = "Subjects, Teachers & Rooms Allocation (${subjects.size} items):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.forEach { sub ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${sub.subject}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${sub.teacherName.take(14)} (${sub.weeklyClasses}x)",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // 4. Timing / Shift
                Text("Select Class Timing / Shift:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = shiftType.startsWith("Morning"),
                        onClick = { shiftType = "Morning (08:00 AM - 01:40 PM)" },
                        label = { Text("Morning (8am)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = shiftType.startsWith("Afternoon"),
                        onClick = { shiftType = "Afternoon (02:00 PM - 06:45 PM)" },
                        label = { Text("Afternoon (2pm)", fontSize = 11.sp) }
                    )
                }

                // Replace existing toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { replaceExistingBatch = !replaceExistingBatch },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = replaceExistingBatch,
                        onCheckedChange = { replaceExistingBatch = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Replace existing timetable for this batch",
                        fontSize = 11.5.sp,
                        color = Color(0xFF334155)
                    )
                }

                // Preview Result Box (if generated)
                previewResult?.let { res ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "✅ Generation Successful!",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${res.totalSlotsFilled} periods assigned • ${res.clashesAvoided} clashes prevented",
                                fontSize = 11.sp,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (previewResult == null) {
                Button(
                    onClick = {
                        val slots = if (shiftType.startsWith("Morning")) {
                            DynamicTimetableEngine.getDefaultTimeSlots().filterNot { it.isBreak }
                        } else {
                            DynamicTimetableEngine.getAfternoonSlots().filterNot { it.isBreak }
                        }
                        val result = DynamicTimetableEngine.generateSchedule(
                            batchName = batchName.trim().ifEmpty { "General Batch" },
                            subjects = subjects,
                            days = DynamicTimetableEngine.WORKING_DAYS,
                            timeSlots = slots,
                            existingPeriods = existingPeriods,
                            replaceBatchExisting = replaceExistingBatch
                        )
                        previewResult = result
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate Routine")
                }
            } else {
                Button(
                    onClick = {
                        onApplyGeneratedSchedule(previewResult!!, replaceExistingBatch)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save & Apply Routine")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// =============================================================================
// DIALOG COMPONENT: MANUAL ADD / EDIT PERIOD DIALOG (With Live Clash Warning)
// =============================================================================
@Composable
fun ManualPeriodDialog(
    period: TimetablePeriod?,
    existingPeriods: List<TimetablePeriod>,
    defaultBatch: String,
    defaultDay: String,
    onDismiss: () -> Unit,
    onSave: (TimetablePeriod) -> Unit
) {
    var batchName by remember { mutableStateOf(period?.batchName ?: defaultBatch) }
    var dayOfWeek by remember { mutableStateOf(period?.dayOfWeek ?: defaultDay) }
    var startTime by remember { mutableStateOf(period?.startTime ?: "08:00 AM") }
    var endTime by remember { mutableStateOf(period?.endTime ?: "08:50 AM") }
    var subject by remember { mutableStateOf(period?.subject ?: "Physics") }
    var teacherName by remember { mutableStateOf(period?.teacherName ?: "Dr. R.K. Verma") }
    var roomNumber by remember { mutableStateOf(period?.roomNumber ?: "Room 101 (Smart Hall)") }

    // Other periods excluding the one being edited
    val otherPeriods = remember(existingPeriods, period) {
        if (period != null) existingPeriods.filterNot { it.id == period.id } else existingPeriods
    }

    // Live Clash Detection
    val teacherClash = remember(teacherName, dayOfWeek, startTime, otherPeriods) {
        otherPeriods.find {
            it.dayOfWeek.equals(dayOfWeek, ignoreCase = true) &&
                    it.startTime.equals(startTime, ignoreCase = true) &&
                    it.teacherName.equals(teacherName, ignoreCase = true)
        }
    }

    val roomClash = remember(roomNumber, dayOfWeek, startTime, otherPeriods) {
        otherPeriods.find {
            it.dayOfWeek.equals(dayOfWeek, ignoreCase = true) &&
                    it.startTime.equals(startTime, ignoreCase = true) &&
                    it.roomNumber.equals(roomNumber, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (period == null) "Schedule New Class" else "Edit Class Period",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Clash Warning Banners
                if (teacherClash != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Text(
                            text = "⚠️ Teacher Clash: $teacherName already has a class in '${teacherClash.batchName}' at this slot!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (roomClash != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Text(
                            text = "⚠️ Room Clash: $roomNumber is already booked by '${roomClash.batchName}' at this slot!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Batch
                OutlinedTextField(
                    value = batchName,
                    onValueChange = { batchName = it },
                    label = { Text("Batch Name", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Day Selector Chips
                Text("Day of Week:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DynamicTimetableEngine.DAYS.forEach { d ->
                        FilterChip(
                            selected = dayOfWeek.equals(d, ignoreCase = true),
                            onClick = { dayOfWeek = d },
                            label = { Text(d.take(3), fontSize = 10.5.sp) }
                        )
                    }
                }

                // Start & End Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Subject
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (e.g. Physics)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Teacher
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text("Faculty / Teacher Name", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Room
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Room / Hall / Lab Number", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newPeriod = TimetablePeriod(
                        id = period?.id ?: UUID.randomUUID().toString(),
                        batchId = period?.batchId ?: UUID.randomUUID().toString(),
                        batchName = batchName.trim().ifEmpty { "General Batch" },
                        dayOfWeek = dayOfWeek,
                        startTime = startTime.trim(),
                        endTime = endTime.trim(),
                        subject = subject.trim().ifEmpty { "General Subject" },
                        teacherName = teacherName.trim().ifEmpty { "Faculty" },
                        roomNumber = roomNumber.trim().ifEmpty { "Room 101" },
                        timestamp = period?.timestamp ?: System.currentTimeMillis()
                    )
                    onSave(newPeriod)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Class")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
