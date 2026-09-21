package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.util.SyncPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(navController: NavController, viewModel: OmrViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isMySqlEnabled by remember { mutableStateOf(SyncPreferences.isMySqlSyncEnabled(context)) }
    var serverUrl by remember {
        val saved = SyncPreferences.getMySqlServerUrl(context)
        val fixed = com.example.util.MySqlSyncManager.normalizeUrl(saved)
        if (fixed != saved && saved.isNotBlank()) {
            SyncPreferences.setMySqlServerUrl(context, fixed)
        }
        mutableStateOf(fixed)
    }
    var apiKey by remember { mutableStateOf(SyncPreferences.getMySqlApiKey(context)) }
    var lastSyncTime by remember { mutableLongStateOf(SyncPreferences.getLastSyncTime(context)) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var isSyncingAll by remember { mutableStateOf(false) }
    var syncAllResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var showSqlSchemaDialog by remember { mutableStateOf(false) }

    val sqlSchemaText = """
-- ========================================================
-- OMR GRADER DATABASE SCHEMA FOR MYSQL / MARIADB
-- UTF8MB4 for Hindi/English Unicode Character Support
-- ========================================================

CREATE DATABASE IF NOT EXISTS `gceedakt_rsarts` 
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `gceedakt_rsarts`;

-- 1. EXAMS TABLE
CREATE TABLE IF NOT EXISTS `exams` (
    `id` INT NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `subject` VARCHAR(255) NOT NULL,
    `date` VARCHAR(100) DEFAULT '',
    `title` VARCHAR(255) DEFAULT 'बिहार विद्यालय परीक्षा , समिति',
    `logo_url` TEXT,
    `logo_opacity` FLOAT DEFAULT 0.2,
    `logo_size` FLOAT DEFAULT 100,
    `logo_position` VARCHAR(50) DEFAULT 'Left',
    `marks_per_question` FLOAT DEFAULT 1.0,
    `negative_marks` FLOAT DEFAULT 0.0,
    `pass_marks` FLOAT DEFAULT 30.0,
    `bonus_marks` FLOAT DEFAULT 0.0,
    `template_type` VARCHAR(100) DEFAULT 'Standard',
    `timestamp` BIGINT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. STUDENTS TABLE
CREATE TABLE IF NOT EXISTS `students` (
    `id` INT AUTO_INCREMENT,
    `roll_no` VARCHAR(100) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `father_name` VARCHAR(255) DEFAULT '',
    `mother_name` VARCHAR(255) DEFAULT '',
    `gender` VARCHAR(20) DEFAULT 'Male',
    `registration_no` VARCHAR(100) DEFAULT '',
    `dob` VARCHAR(50) DEFAULT '',
    `mobile_no` VARCHAR(50) DEFAULT '',
    `email` VARCHAR(255) DEFAULT '',
    `stream` VARCHAR(100) DEFAULT 'ARTS',
    `subjects` VARCHAR(255) DEFAULT '',
    `image_url` TEXT,
    `timestamp` BIGINT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_roll_no` (`roll_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. ANSWER KEYS TABLE
CREATE TABLE IF NOT EXISTS `answer_keys` (
    `id` INT NOT NULL,
    `exam_id` INT NOT NULL,
    `set_name` VARCHAR(50) NOT NULL,
    `num_questions` INT NOT NULL,
    `num_options` INT NOT NULL,
    `correct_answers` LONGTEXT NOT NULL,
    `timestamp` BIGINT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`exam_id`, `set_name`),
    INDEX (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. QUESTIONS TABLE
CREATE TABLE IF NOT EXISTS `questions` (
    `id` INT NOT NULL,
    `exam_id` INT NOT NULL,
    `text` TEXT NOT NULL,
    `option_a` TEXT,
    `option_b` TEXT,
    `option_c` TEXT,
    `option_d` TEXT,
    `correct_index` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. SCAN RESULTS TABLE
CREATE TABLE IF NOT EXISTS `scan_results` (
    `id` INT AUTO_INCREMENT,
    `exam_id` INT NOT NULL,
    `student_id` VARCHAR(100) NOT NULL,
    `paper_set` VARCHAR(50) DEFAULT '',
    `score` FLOAT NOT NULL,
    `total_questions` INT NOT NULL,
    `student_answers` LONGTEXT,
    `question_statuses` LONGTEXT,
    `timestamp` BIGINT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exam_student` (`exam_id`, `student_id`),
    INDEX (`exam_id`),
    INDEX (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
""".trimIndent()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Database & Cloud Sync",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFFAFBFD)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. FIREBASE REALTIME DB STATUS CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF7ED)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudQueue,
                                    contentDescription = "Firebase",
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Firebase Realtime DB",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    "Google Cloud Sync Active",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF16A34A))
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "Connected",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "All exams, students, OMR keys, and scanned results automatically sync with Firebase Realtime Database in real-time.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 15.sp
                    )
                }
            }

            // 2. MYSQL WEB SERVER DUAL-SYNC CONFIGURATION CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = "MySQL",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Web Server MySQL Sync",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    "Dual-Write to Your Hosting / VPS",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Switch(
                            checked = isMySqlEnabled,
                            onCheckedChange = {
                                isMySqlEnabled = it
                                SyncPreferences.setMySqlSyncEnabled(context, it)
                                Toast.makeText(
                                    context,
                                    if (it) "MySQL Web Server Sync Enabled" else "MySQL Web Server Sync Disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2563EB)
                            ),
                            modifier = Modifier.testTag("mysql_sync_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Server URL Input
                    Text(
                        "Web Server API URL",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        placeholder = { Text("http://rsartsclassess.whf.bz/omr_api/api.php", fontSize = 11.5.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_url_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    if (serverUrl.contains(".omr_api")) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    serverUrl = com.example.util.MySqlSyncManager.normalizeUrl(serverUrl)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    "Typo detected: '.omr_api' found! Tap here to fix to '/omr_api/api.php'",
                                    fontSize = 10.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Text(
                        "Example: http://rsartsclassess.whf.bz/omr_api/api.php",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 3.dp, start = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // API Key / Secret Token Input
                    Text(
                        "API Secret Key (Optional / X-API-KEY)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        placeholder = { Text("Leave blank if not configured in db_config.php", fontSize = 11.5.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons Row: Save & Test Connection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val cleanUrl = com.example.util.MySqlSyncManager.normalizeUrl(serverUrl)
                                serverUrl = cleanUrl
                                SyncPreferences.setMySqlServerUrl(context, cleanUrl)
                                SyncPreferences.setMySqlApiKey(context, apiKey.trim())
                                if (cleanUrl.isNotBlank() && !isMySqlEnabled) {
                                    isMySqlEnabled = true
                                    SyncPreferences.setMySqlSyncEnabled(context, true)
                                }
                                Toast.makeText(context, "URL normalized & saved", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("save_sync_settings_btn"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Save URL", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (serverUrl.isBlank()) {
                                    Toast.makeText(context, "Please enter Server URL first", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                val cleanUrl = com.example.util.MySqlSyncManager.normalizeUrl(serverUrl)
                                serverUrl = cleanUrl
                                SyncPreferences.setMySqlServerUrl(context, cleanUrl)
                                SyncPreferences.setMySqlApiKey(context, apiKey.trim())
                                
                                isTestingConnection = true
                                testResult = null
                                viewModel.testMySqlConnection(cleanUrl, apiKey.trim()) { success, msg ->
                                    isTestingConnection = false
                                    testResult = Pair(success, msg)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("test_mysql_conn_btn"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            border = BorderStroke(1.dp, Color(0xFF2563EB)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Test Connection", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Test Connection Result Feedback Banner
                    testResult?.let { result ->
                        Spacer(modifier = Modifier.height(10.dp))
                        val (isSuccess, msg) = result
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, if (isSuccess) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isSuccess) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = msg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSuccess) Color(0xFF14532D) else Color(0xFF7F1D1D)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bulk Sync Button
                    Text(
                        "Force Bulk Sync",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        "Upload all existing exams, answer keys, questions, students (with photos), and scan results to your MySQL server right now.",
                        fontSize = 10.5.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            if (serverUrl.isBlank()) {
                                Toast.makeText(context, "Enter Server URL first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSyncingAll = true
                            syncAllResult = null
                            viewModel.bulkSyncToMySql { success, msg ->
                                isSyncingAll = false
                                syncAllResult = Pair(success, msg)
                                if (success) {
                                    lastSyncTime = System.currentTimeMillis()
                                }
                            }
                        },
                        enabled = !isSyncingAll,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("bulk_sync_btn"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        if (isSyncingAll) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Syncing to MySQL...", fontSize = 11.5.sp)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync All Data to MySQL Now", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (lastSyncTime > 0) {
                        val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastSyncTime))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Last Full Sync: $formattedDate",
                            fontSize = 10.sp,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Bulk Sync Result Feedback Banner
                    syncAllResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        val (isSuccess, msg) = result
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, if (isSuccess) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isSuccess) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = msg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSuccess) Color(0xFF14532D) else Color(0xFF7F1D1D)
                                )
                            }
                        }
                    }
                }
            }

            // 3. DATABASE SCHEMA & BACKEND SCRIPT VIEWER CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF3E8FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = "SQL",
                                    tint = Color(0xFF9333EA),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "MySQL .sql Table Structure",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    "Ready to import in phpMyAdmin",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Includes tables for: exams, students, answer_keys, questions, and scan_results with UTF8MB4 Hindi support.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSqlSchemaDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View SQL", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("OMR Database SQL", sqlSchemaText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "SQL Schema copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy SQL", fontSize = 11.sp)
                        }
                    }
                }
            }

            // 4. STEP-BY-STEP SETUP GUIDE CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Setup Steps for Your Web Server:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "1. Open cPanel / phpMyAdmin and create a database named `omr_system`.\n" +
                        "2. Click 'Import' and run the SQL table structure above.\n" +
                        "3. Upload the provided `api.php` and `db_config.php` to your web server (e.g. `public_html/omr_api/`).\n" +
                        "4. Put your DB username and password in `db_config.php`.\n" +
                        "5. Enter your URL above (e.g. `https://yourdomain.com/omr_api/api.php`) and click 'Test Connection'.",
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // SQL Schema Dialog
    if (showSqlSchemaDialog) {
        AlertDialog(
            onDismissRequest = { showSqlSchemaDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("MySQL Table Structure (.sql)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("OMR Database SQL", sqlSchemaText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "SQL copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = sqlSchemaText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSqlSchemaDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
