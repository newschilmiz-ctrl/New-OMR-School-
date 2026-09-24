package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.FacultyMember
import com.example.data.NoticeRecord
import com.example.data.StudyMaterial
import com.example.data.TimetablePeriod
import com.example.util.CoachingFeaturesManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachingHubScreen(
    navController: NavController,
    viewModel: OmrViewModel,
    initialTab: Int = 0
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        CoachingFeaturesManager.init(context)
    }

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabTitles = remember { listOf("Notices", "Timetable", "Faculty", "DPP & Notes") }

    // Reactive State Holders
    var notices by remember { mutableStateOf(CoachingFeaturesManager.getNotices()) }
    var timetable by remember { mutableStateOf(CoachingFeaturesManager.getTimetable()) }
    var facultyList by remember { mutableStateOf(CoachingFeaturesManager.getFaculty()) }
    var studyMaterials by remember { mutableStateOf(CoachingFeaturesManager.getStudyMaterials()) }

    // Dialog Visibility
    var showAddNoticeDialog by remember { mutableStateOf(false) }
    var showAddPeriodDialog by remember { mutableStateOf(false) }
    var showAddFacultyDialog by remember { mutableStateOf(false) }
    var showAddMaterialDialog by remember { mutableStateOf(false) }

    fun refreshAll() {
        notices = CoachingFeaturesManager.getNotices()
        timetable = CoachingFeaturesManager.getTimetable()
        facultyList = CoachingFeaturesManager.getFaculty()
        studyMaterials = CoachingFeaturesManager.getStudyMaterials()
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Institute Management Hub",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        "PRO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE11D48),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                "Circulars, Schedule, Faculty & Materials",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        IconButton(
                            onClick = {
                                when (selectedTab) {
                                    0 -> showAddNoticeDialog = true
                                    1 -> showAddPeriodDialog = true
                                    2 -> showAddFacultyDialog = true
                                    3 -> showAddMaterialDialog = true
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48))
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add New",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Navigation Tabs
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = Color(0xFFE11D48),
                        divider = {}
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (selectedTab == index) Color(0xFFE11D48) else Color(0xFF64748B)
                                    )
                                }
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            when (selectedTab) {
                0 -> NoticeBoardTab(
                    notices = notices,
                    onAddNotice = { showAddNoticeDialog = true },
                    onDeleteNotice = {
                        CoachingFeaturesManager.deleteNotice(it)
                        refreshAll()
                        Toast.makeText(context, "Notice deleted", Toast.LENGTH_SHORT).show()
                    },
                    onTogglePin = {
                        CoachingFeaturesManager.togglePinNotice(it)
                        refreshAll()
                    }
                )
                1 -> TimetableTab(
                    periods = timetable,
                    onAddPeriod = { showAddPeriodDialog = true },
                    onDeletePeriod = {
                        CoachingFeaturesManager.deleteTimetablePeriod(it)
                        refreshAll()
                        Toast.makeText(context, "Period removed", Toast.LENGTH_SHORT).show()
                    },
                    onOpenDynamicStudio = {
                        navController.navigate(Screen.DynamicTimetable.createRoute("ALL"))
                    }
                )
                2 -> FacultyDirectoryTab(
                    facultyList = facultyList,
                    onAddFaculty = { showAddFacultyDialog = true },
                    onDeleteFaculty = {
                        CoachingFeaturesManager.deleteFaculty(it)
                        refreshAll()
                        Toast.makeText(context, "Faculty record removed", Toast.LENGTH_SHORT).show()
                    }
                )
                3 -> StudyMaterialTab(
                    materials = studyMaterials,
                    onAddMaterial = { showAddMaterialDialog = true },
                    onDeleteMaterial = {
                        CoachingFeaturesManager.deleteStudyMaterial(it)
                        refreshAll()
                        Toast.makeText(context, "Material deleted", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Dialogs
    if (showAddNoticeDialog) {
        AddNoticeDialog(
            onDismiss = { showAddNoticeDialog = false },
            onConfirm = { notice ->
                CoachingFeaturesManager.addNotice(notice)
                refreshAll()
                showAddNoticeDialog = false
                Toast.makeText(context, "Notice published successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddPeriodDialog) {
        AddPeriodDialog(
            onDismiss = { showAddPeriodDialog = false },
            onConfirm = { period ->
                CoachingFeaturesManager.addTimetablePeriod(period)
                refreshAll()
                showAddPeriodDialog = false
                Toast.makeText(context, "Timetable period added!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddFacultyDialog) {
        AddFacultyDialog(
            onDismiss = { showAddFacultyDialog = false },
            onConfirm = { faculty ->
                CoachingFeaturesManager.addFaculty(faculty)
                refreshAll()
                showAddFacultyDialog = false
                Toast.makeText(context, "Faculty profile added!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddMaterialDialog) {
        AddMaterialDialog(
            onDismiss = { showAddMaterialDialog = false },
            onConfirm = { material ->
                CoachingFeaturesManager.addStudyMaterial(material)
                refreshAll()
                showAddMaterialDialog = false
                Toast.makeText(context, "Study material added!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// =========================================================================
// TAB 1: NOTICES & CIRCULARS
// =========================================================================
@Composable
fun NoticeBoardTab(
    notices: List<NoticeRecord>,
    onAddNotice: () -> Unit,
    onDeleteNotice: (String) -> Unit,
    onTogglePin: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("ALL") }
    val categories = listOf("ALL", "EXAM_ALERT", "HOLIDAY", "FEE_ALERT", "URGENT", "GENERAL")

    val filtered = remember(notices, selectedCategory) {
        if (selectedCategory == "ALL") notices
        else notices.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Category Filter Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(categories, key = { it }) { cat ->
                    val isSelected = selectedCategory == cat
                    val displayLabel = when (cat) {
                        "ALL" -> "All Circulars"
                        "EXAM_ALERT" -> "Exams"
                        "HOLIDAY" -> "Holidays"
                        "FEE_ALERT" -> "Fees"
                        "URGENT" -> "Urgent"
                        else -> "General"
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedCategory = cat },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFFE11D48) else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFE11D48) else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = displayLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Notices Found", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Publish an announcement for your coaching batches.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddNotice,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Notice", fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { notice ->
                NoticeItemCard(
                    notice = notice,
                    onDelete = { onDeleteNotice(notice.id) },
                    onTogglePin = { onTogglePin(notice.id) },
                    onShare = {
                        val shareText = "📢 *${notice.title}*\n\n${notice.content}\n\n🎯 Target: ${notice.targetBatch}\n🗓️ Date: ${notice.date}\n🏛️ ${notice.postedBy}"
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Broadcast Notice"))
                    }
                )
            }
        }
    }
}

@Composable
fun NoticeItemCard(
    notice: NoticeRecord,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onShare: () -> Unit
) {
    val (badgeBg, badgeText, badgeIcon) = when (notice.category) {
        "EXAM_ALERT" -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), Icons.Default.Quiz)
        "HOLIDAY" -> Triple(Color(0xFFECFDF5), Color(0xFF059669), Icons.Default.WbSunny)
        "FEE_ALERT" -> Triple(Color(0xFFFFFBEB), Color(0xFFD97706), Icons.Default.Payments)
        "URGENT" -> Triple(Color(0xFFFFF1F2), Color(0xFFE11D48), Icons.Default.Warning)
        else -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), Icons.Default.Campaign)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            if (notice.isPinned) Color(0xFFFECDD3) else Color(0xFFF1F5F9)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(badgeIcon, contentDescription = null, tint = badgeText, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = notice.category.replace("_", " "),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeText
                            )
                        }
                    }

                    if (notice.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFF1F2)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PushPin, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("PINNED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (notice.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = if (notice.isPinned) Color(0xFFE11D48) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = notice.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = notice.content,
                fontSize = 12.5.sp,
                color = Color(0xFF475569),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(notice.targetBatch, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(notice.date, fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

// =========================================================================
// TAB 2: TIMETABLE & ROUTINE
// =========================================================================
@Composable
fun TimetableTab(
    periods: List<TimetablePeriod>,
    onAddPeriod: () -> Unit,
    onDeletePeriod: (String) -> Unit,
    onOpenDynamicStudio: () -> Unit = {}
) {
    val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val todayName = remember {
        SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date()).uppercase(Locale.ENGLISH)
    }
    var selectedDay by remember { mutableStateOf(if (todayName in days) todayName else "MONDAY") }

    val batches = remember(periods) {
        listOf("ALL") + periods.map { it.batchName }.distinct()
    }
    var selectedBatch by remember { mutableStateOf("ALL") }

    val dayPeriods = remember(periods, selectedDay, selectedBatch) {
        periods.filter { p ->
            p.dayOfWeek.equals(selectedDay, ignoreCase = true) &&
                    (selectedBatch == "ALL" || p.batchName == selectedBatch)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Dynamic Timetable Studio Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE11D48)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dynamic Timetable Studio",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Auto-generate clash-free routine based on subjects, rooms & teachers",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onOpenDynamicStudio,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // Day selector
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(days, key = { it }) { day ->
                    val isSelected = selectedDay == day
                    val shortDay = day.take(3)
                    val isToday = day == todayName
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedDay = day },
                        shape = RoundedCornerShape(12.dp),
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
                                text = shortDay,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF1E293B)
                            )
                            if (isToday) {
                                Text(
                                    text = "TODAY",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFFFDA4AF) else Color(0xFFE11D48)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (batches.size > 2) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(batches, key = { it }) { b ->
                        val isSelected = selectedBatch == b
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedBatch = b },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFFE11D48) else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFE11D48) else Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = if (b == "ALL") "All Batches" else b,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (dayPeriods.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Classes Scheduled", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("No lecture period has been registered for $selectedDay.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onAddPeriod,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Schedule Period", fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            items(dayPeriods, key = { it.id }) { period ->
                PeriodCard(period = period, onDelete = { onDeletePeriod(period.id) })
            }
        }
    }
}

@Composable
fun PeriodCard(
    period: TimetablePeriod,
    onDelete: () -> Unit
) {
    val subjectColor = when {
        period.subject.contains("Physics", ignoreCase = true) -> Color(0xFF2563EB)
        period.subject.contains("Chemistry", ignoreCase = true) -> Color(0xFF059669)
        period.subject.contains("Math", ignoreCase = true) -> Color(0xFFD97706)
        period.subject.contains("Bio", ignoreCase = true) -> Color(0xFF0D9488)
        period.subject.contains("Account", ignoreCase = true) -> Color(0xFF7C3AED)
        else -> Color(0xFFE11D48)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Time Column
            Column(
                modifier = Modifier.width(82.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = period.startTime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "to ${period.endTime}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = period.roomNumber,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Colored Divider Pillar
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(subjectColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = period.subject,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = period.teacherName,
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = period.batchName,
                    fontSize = 10.5.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// =========================================================================
// TAB 3: FACULTY & TEACHERS DIRECTORY
// =========================================================================
@Composable
fun FacultyDirectoryTab(
    facultyList: List<FacultyMember>,
    onAddFaculty: () -> Unit,
    onDeleteFaculty: (String) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SupervisedUserCircle, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Faculty & Instructor Roster", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                        Text("Manage subject teachers, qualifications, and direct communication channels.", fontSize = 11.sp, color = Color(0xFF3B82F6))
                    }
                }
            }
        }

        if (facultyList.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Faculty Added", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add teaching staff and instructors to this coaching roster.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onAddFaculty,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Faculty", fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            items(facultyList, key = { it.id }) { faculty ->
                FacultyCard(
                    faculty = faculty,
                    onDelete = { onDeleteFaculty(faculty.id) },
                    onCall = {
                        if (faculty.phone.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${faculty.phone}"))
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onWhatsApp = {
                        val cleanPhone = faculty.phone.replace(Regex("[^0-9]"), "")
                        if (cleanPhone.isNotBlank()) {
                            val uri = Uri.parse("https://wa.me/$cleanPhone?text=Hello%20${Uri.encode(faculty.name)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FacultyCard(
    faculty: FacultyMember,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Teacher Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFE11D48), Color(0xFFBE123C))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = faculty.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = faculty.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = faculty.subject,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE11D48)
                    )
                    Text(
                        text = faculty.qualification,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = faculty.salaryType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = faculty.assignedBatches,
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onCall() },
                        shape = CircleShape,
                        color = Color(0xFFEFF6FF)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFF2563EB), modifier = Modifier.padding(6.dp).size(15.dp))
                    }

                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onWhatsApp() },
                        shape = CircleShape,
                        color = Color(0xFFECFDF5)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF059669), modifier = Modifier.padding(6.dp).size(15.dp))
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 4: STUDY MATERIAL & DPP TRACKER
// =========================================================================
@Composable
fun StudyMaterialTab(
    materials: List<StudyMaterial>,
    onAddMaterial: () -> Unit,
    onDeleteMaterial: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf("ALL") }
    val types = listOf("ALL", "DPP", "NOTES", "FORMULA_BOOK", "ASSIGNMENT", "SOLUTION")

    val filtered = remember(materials, selectedType) {
        if (selectedType == "ALL") materials
        else materials.filter { it.type.equals(selectedType, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(types, key = { it }) { t ->
                    val isSelected = selectedType == t
                    val label = when (t) {
                        "ALL" -> "All Materials"
                        "DPP" -> "DPP Sheets"
                        "NOTES" -> "Chapter Notes"
                        "FORMULA_BOOK" -> "Formulae"
                        "ASSIGNMENT" -> "Assignments"
                        else -> "Solutions"
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedType = t },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFFE11D48) else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFE11D48) else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Materials Found", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Upload daily practice problems or chapter lecture notes.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onAddMaterial,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Material", fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { mat ->
                MaterialCard(
                    material = mat,
                    onDelete = { onDeleteMaterial(mat.id) },
                    onShare = {
                        val text = "📚 *${mat.title}*\nSubject: ${mat.subject} (${mat.chapter})\nBatch: ${mat.batchName}\nDue: ${mat.dueDate}\nTotal Problems: ${mat.totalProblems}"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Study Material"))
                    }
                )
            }
        }
    }
}

@Composable
fun MaterialCard(
    material: StudyMaterial,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val (typeBg, typeText) = when (material.type) {
        "DPP" -> Pair(Color(0xFFFEF2F2), Color(0xFFDC2626))
        "NOTES" -> Pair(Color(0xFFEFF6FF), Color(0xFF2563EB))
        "FORMULA_BOOK" -> Pair(Color(0xFFF5F3FF), Color(0xFF7C3AED))
        "ASSIGNMENT" -> Pair(Color(0xFFECFDF5), Color(0xFF059669))
        else -> Pair(Color(0xFFFFFBEB), Color(0xFFD97706))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = typeBg
                ) {
                    Text(
                        text = material.type.replace("_", " "),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = material.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = material.subject,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE11D48)
                )
                Text(" • ", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text(
                    text = material.chapter,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(material.dueDate, fontSize = 11.sp, color = Color(0xFF64748B))
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "${material.totalProblems} Questions",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// MODAL DIALOGS FOR ADDING DATA
// =========================================================================

@Composable
fun AddNoticeDialog(
    onDismiss: () -> Unit,
    onConfirm: (NoticeRecord) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("GENERAL") }
    var targetBatch by remember { mutableStateOf("All Batches") }
    var postedBy by remember { mutableStateOf("Director Office") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Publish Notice / Circular", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notice Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Announcement Body *") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it.uppercase() },
                        label = { Text("Category (e.g. EXAM_ALERT)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = targetBatch,
                        onValueChange = { targetBatch = it },
                        label = { Text("Batch") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                OutlinedTextField(
                    value = postedBy,
                    onValueChange = { postedBy = it },
                    label = { Text("Authority / Author") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        val today = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                        onConfirm(
                            NoticeRecord(
                                title = title.trim(),
                                content = content.trim(),
                                category = category.trim(),
                                targetBatch = targetBatch.trim(),
                                postedBy = postedBy.trim(),
                                date = today
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Publish Notice")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddPeriodDialog(
    onDismiss: () -> Unit,
    onConfirm: (TimetablePeriod) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var batchName by remember { mutableStateOf("Morning Science Batch") }
    var dayOfWeek by remember { mutableStateOf("MONDAY") }
    var startTime by remember { mutableStateOf("07:00 AM") }
    var endTime by remember { mutableStateOf("08:15 AM") }
    var roomNumber by remember { mutableStateOf("Hall 101") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Schedule Timetable Period", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (e.g. Physics)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text("Faculty / Teacher Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dayOfWeek,
                        onValueChange = { dayOfWeek = it.uppercase() },
                        label = { Text("Day (e.g. MONDAY)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = roomNumber,
                        onValueChange = { roomNumber = it },
                        label = { Text("Room / Hall") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                OutlinedTextField(
                    value = batchName,
                    onValueChange = { batchName = it },
                    label = { Text("Batch Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank() && teacherName.isNotBlank()) {
                        onConfirm(
                            TimetablePeriod(
                                subject = subject.trim(),
                                teacherName = teacherName.trim(),
                                batchName = batchName.trim(),
                                dayOfWeek = dayOfWeek.trim(),
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                roomNumber = roomNumber.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Period")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddFacultyDialog(
    onDismiss: () -> Unit,
    onConfirm: (FacultyMember) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var qualification by remember { mutableStateOf("M.Sc, B.Ed") }
    var phone by remember { mutableStateOf("+91 ") }
    var assignedBatches by remember { mutableStateOf("All Batches") }
    var salaryType by remember { mutableStateOf("Monthly") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Faculty Member", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Faculty Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Specialization / Subject *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("Educational Qualification") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone / WhatsApp Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = assignedBatches,
                    onValueChange = { assignedBatches = it },
                    label = { Text("Assigned Batches") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && subject.isNotBlank()) {
                        onConfirm(
                            FacultyMember(
                                name = name.trim(),
                                subject = subject.trim(),
                                qualification = qualification.trim(),
                                phone = phone.trim(),
                                assignedBatches = assignedBatches.trim(),
                                salaryType = salaryType.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Add Teacher")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddMaterialDialog(
    onDismiss: () -> Unit,
    onConfirm: (StudyMaterial) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("DPP") }
    var batchName by remember { mutableStateOf("All Batches") }
    var dueDate by remember { mutableStateOf("Next Class") }
    var totalProblems by remember { mutableStateOf("20") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add DPP / Study Material", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / DPP Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it.uppercase() },
                        label = { Text("Type (DPP/NOTES)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it },
                    label = { Text("Chapter / Unit Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date / Deadline") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = totalProblems,
                        onValueChange = { totalProblems = it },
                        label = { Text("Total Qs") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && subject.isNotBlank()) {
                        onConfirm(
                            StudyMaterial(
                                title = title.trim(),
                                subject = subject.trim(),
                                chapter = chapter.trim(),
                                type = type.trim(),
                                batchName = batchName.trim(),
                                dueDate = dueDate.trim(),
                                totalProblems = totalProblems.toIntOrNull() ?: 15
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Material")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
