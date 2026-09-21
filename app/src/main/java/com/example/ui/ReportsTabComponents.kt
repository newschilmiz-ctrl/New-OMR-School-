package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Converters
import com.example.data.Exam
import com.example.data.ScanResult
import com.example.data.Student
import com.example.util.RankedStudentResult
import com.example.util.ReportPdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportViewerDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFFAFBFD),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color(0xFF0F172A))
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A), maxLines = 1)
                            Text(subtitle, fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1)
                        }
                        Button(
                            onClick = onExportPdf,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    content()
                }
            }
        }
    }
}

// 1. RANK LIST CONTENT
@Composable
fun RankListContent(ranked: List<RankedStudentResult>) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(ranked, searchQuery) {
        ranked.filter {
            val name = it.student?.name ?: ""
            it.result.studentId.contains(searchQuery, ignoreCase = true) || name.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Roll Number or Name...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFFE11D48),
                unfocusedBorderColor = Color(0xFFE2E8F0)
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Showing ${filtered.size} of ${ranked.size} students", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
            val passCount = ranked.count { it.isPassed }
            Text("Pass Rate: ${if (ranked.isNotEmpty()) (passCount * 100) / ranked.size else 0}%", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No candidate records found.", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (item.rank) {
                                                1 -> Color(0xFFFEF9C3)
                                                2 -> Color(0xFFF1F5F9)
                                                3 -> Color(0xFFFFEDD5)
                                                else -> Color(0xFFF8FAFC)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "#${item.rank}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (item.rank) {
                                            1 -> Color(0xFFCA8A04)
                                            2 -> Color(0xFF64748B)
                                            3 -> Color(0xFFEA580C)
                                            else -> Color(0xFF0F172A)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        item.student?.name ?: "Student ${item.result.studentId}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        "Roll: ${item.result.studentId}  •  Set: ${item.result.paperSet.ifEmpty { "A" }}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${String.format("%.1f", item.result.score)} Marks",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${String.format("%.1f", item.percentage)}%",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (item.isPassed) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            if (item.isPassed) "PASS" else "FAIL",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isPassed) Color(0xFF16A34A) else Color(0xFFDC2626)
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
}

// 2. MERIT LIST CONTENT
@Composable
fun MeritListContent(ranked: List<RankedStudentResult>) {
    var selectedBand by remember { mutableStateOf("All") }
    val bands = listOf("All", "A+ (80%+)", "A (60-79%)", "B (45-59%)", "C (Pass-44%)", "Fail")

    val filtered = remember(ranked, selectedBand) {
        when (selectedBand) {
            "A+ (80%+)" -> ranked.filter { it.percentage >= 80f }
            "A (60-79%)" -> ranked.filter { it.percentage in 60f..79.99f }
            "B (45-59%)" -> ranked.filter { it.percentage in 45f..59.99f }
            "C (Pass-44%)" -> ranked.filter { it.isPassed && it.percentage < 45f }
            "Fail" -> ranked.filter { !it.isPassed }
            else -> ranked
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Filter row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bands.take(3).forEach { band ->
                val isSelected = selectedBand == band
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedBand = band },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFFE11D48) else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFE11D48) else Color(0xFFE2E8F0))
                ) {
                    Text(
                        band,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF0F172A)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bands.drop(3).forEach { band ->
                val isSelected = selectedBand == band
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedBand = band },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFFE11D48) else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFE11D48) else Color(0xFFE2E8F0))
                ) {
                    Text(
                        band,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF0F172A)
                    )
                }
            }
        }

        Text(
            "Found ${filtered.size} candidates in category '$selectedBand'",
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.student?.name ?: "Student ${item.result.studentId}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Roll: ${item.result.studentId}  •  Grade: ${item.grade}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${String.format("%.1f", item.result.score)} Marks",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "${String.format("%.1f", item.percentage)}%",
                                fontSize = 11.sp,
                                color = if (item.isPassed) Color(0xFF16A34A) else Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// 3. PASS / FAIL SUMMARY CONTENT
@Composable
fun PassFailSummaryContent(exam: Exam, ranked: List<RankedStudentResult>) {
    val total = ranked.size
    val passed = ranked.count { it.isPassed }
    val failed = total - passed
    val passRate = if (total > 0) (passed * 100) / total else 0

    val boys = ranked.filter { it.student?.gender?.equals("Male", ignoreCase = true) == true }
    val girls = ranked.filter { it.student?.gender?.equals("Female", ignoreCase = true) == true }
    val boysPass = boys.count { it.isPassed }
    val girlsPass = girls.count { it.isPassed }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // High-level Gauge Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Overall Evaluation Result", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Passing Rate", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text("$passRate%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Cutoff: ${exam.passMarks.toInt()} Marks", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text("$passed Passed / $failed Failed", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) passed.toFloat() / total else 0f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF16A34A),
                    trackColor = Color(0xFFFEE2E2)
                )
            }
        }

        // Gender Demographics
        Text("Gender-Wise Demographic Audit", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Boys
            val boysRate = if (boys.isNotEmpty()) (boysPass * 100) / boys.size else 0
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Male, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Boys", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("$boysPass / ${boys.size} Passed", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                    Text("Pass Rate: $boysRate%", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }

            // Girls
            val girlsRate = if (girls.isNotEmpty()) (girlsPass * 100) / girls.size else 0
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Female, contentDescription = null, tint = Color(0xFFDB2777), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Girls", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("$girlsPass / ${girls.size} Passed", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDB2777))
                    Text("Pass Rate: $girlsRate%", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }
        }

        // Score Distribution Stat
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Performance Distribution", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                val maxScore = ranked.maxOfOrNull { it.result.score } ?: 0f
                val minScore = ranked.minOfOrNull { it.result.score } ?: 0f
                val avgScore = if (ranked.isNotEmpty()) ranked.map { it.result.score }.average() else 0.0

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Highest Score: ${String.format("%.1f", maxScore)}", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    Text("Average: ${String.format("%.1f", avgScore)}", fontSize = 12.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                    Text("Lowest: ${String.format("%.1f", minScore)}", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 4. TOP 10 HIGH ACHIEVERS CONTENT
@Composable
fun TopAchieversContent(ranked: List<RankedStudentResult>) {
    val top10 = ranked.take(10)

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Honor Roll & Merit Certificate List", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))

        if (top10.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No candidate records available.", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(top10) { item ->
                    val isGold = item.rank == 1
                    val isSilver = item.rank == 2
                    val isBronze = item.rank == 3

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isGold -> Color(0xFFFEFCE8)
                                isSilver -> Color(0xFFF8FAFC)
                                isBronze -> Color(0xFFFFF7ED)
                                else -> Color.White
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            when {
                                isGold -> Color(0xFFFACC15)
                                isSilver -> Color(0xFFCBD5E1)
                                isBronze -> Color(0xFFFDBA74)
                                else -> Color(0xFFE2E8F0)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = when {
                                        isGold -> Color(0xFFCA8A04)
                                        isSilver -> Color(0xFF64748B)
                                        isBronze -> Color(0xFFEA580C)
                                        else -> Color(0xFF94A3B8)
                                    },
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Rank #${item.rank} - ${item.student?.name ?: "Student ${item.result.studentId}"}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        "Roll: ${item.result.studentId}  •  Set: ${item.result.paperSet.ifEmpty { "A" }}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${String.format("%.1f", item.result.score)} Marks",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    "${String.format("%.1f", item.percentage)}%",
                                    fontSize = 12.sp,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5. ITEM DIFFICULTY ANALYSIS CONTENT
@Composable
fun ItemDifficultyContent(results: List<ScanResult>) {
    val totalQ = results.firstOrNull()?.totalQuestions?.takeIf { it > 0 } ?: 100
    val converters = remember { Converters() }
    val allStatuses = remember(results) { results.map { converters.toList(it.questionStatuses) } }

    var filterLevel by remember { mutableStateOf("All") }

    data class QStat(val qNum: Int, val correctCount: Int, val accuracy: Float, val level: String)
    val qStats = remember(allStatuses, totalQ) {
        (0 until totalQ).map { qIdx ->
            var cCount = 0
            for (st in allStatuses) {
                if (qIdx < st.size && st[qIdx] == 1) cCount++
            }
            val acc = if (results.isNotEmpty()) (cCount * 100f) / results.size else 0f
            val lvl = when {
                acc >= 75f -> "Easy"
                acc >= 45f -> "Moderate"
                else -> "Difficult"
            }
            QStat(qIdx + 1, cCount, acc, lvl)
        }
    }

    val filtered = when (filterLevel) {
        "Easy" -> qStats.filter { it.level == "Easy" }
        "Moderate" -> qStats.filter { it.level == "Moderate" }
        "Difficult" -> qStats.filter { it.level == "Difficult" }
        else -> qStats
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Summary Chips
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All", "Easy", "Moderate", "Difficult").forEach { lvl ->
                val isSelected = filterLevel == lvl
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { filterLevel = lvl },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFFE11D48) else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFE11D48) else Color(0xFFE2E8F0))
                ) {
                    Text(
                        lvl,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF0F172A)
                    )
                }
            }
        }

        Text(
            "Showing ${filtered.size} questions ($filterLevel)",
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Question #${item.qNum}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text(
                                "${item.correctCount} of ${results.size} students answered correctly",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${String.format("%.1f", item.accuracy)}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (item.level) {
                                            "Easy" -> Color(0xFFDCFCE7)
                                            "Moderate" -> Color(0xFFDBEAFE)
                                            else -> Color(0xFFFEE2E2)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    item.level,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (item.level) {
                                        "Easy" -> Color(0xFF16A34A)
                                        "Moderate" -> Color(0xFF2563EB)
                                        else -> Color(0xFFDC2626)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 6. ATTENDANCE VS SCANNED CONTENT
@Composable
fun AttendanceAuditContent(
    students: List<Student>,
    results: List<ScanResult>,
    attendanceMap: Map<String, Boolean>
) {
    val scannedRolls = remember(results) { results.map { it.studentId.trim() }.toSet() }
    val sortedStudents = remember(students) { students.sortedBy { it.rollNo } }

    val missingSheets = remember(sortedStudents, attendanceMap, scannedRolls) {
        sortedStudents.filter { attendanceMap[it.rollNo] == true && !scannedRolls.contains(it.rollNo.trim()) }
    }
    val reconciledCount = remember(sortedStudents, attendanceMap, scannedRolls) {
        sortedStudents.count { attendanceMap[it.rollNo] == true && scannedRolls.contains(it.rollNo.trim()) }
    }

    var filterType by remember { mutableStateOf("All") }

    val filtered = remember(filterType, sortedStudents, missingSheets, attendanceMap, scannedRolls) {
        when (filterType) {
            "Missing OMR" -> missingSheets
            "Reconciled" -> sortedStudents.filter { attendanceMap[it.rollNo] == true && scannedRolls.contains(it.rollNo.trim()) }
            "Absent" -> sortedStudents.filter { attendanceMap[it.rollNo] != true }
            else -> sortedStudents
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (missingSheets.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, Color(0xFFFECDD3))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${missingSheets.size} Candidate(s) were present but their OMR sheet is NOT scanned!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }

        // Summary row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All", "Missing OMR", "Reconciled", "Absent").forEach { opt ->
                val isSelected = filterType == opt
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { filterType = opt },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFFE11D48) else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFE11D48) else Color(0xFFE2E8F0))
                ) {
                    Text(
                        opt,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF0F172A)
                    )
                }
            }
        }

        Text(
            "Reconciled: $reconciledCount  •  Missing Sheets: ${missingSheets.size}  •  Showing ${filtered.size}",
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { s ->
                val isPresent = attendanceMap[s.rollNo] == true
                val isScanned = scannedRolls.contains(s.rollNo.trim())
                val isMissing = isPresent && !isScanned

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, if (isMissing) Color(0xFFFCA5A5) else Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text("Roll: ${s.rollNo}  •  Attendance: ${if (isPresent) "Present" else "Absent"}", fontSize = 11.sp, color = Color(0xFF64748B))
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        isMissing -> Color(0xFFFEE2E2)
                                        isPresent && isScanned -> Color(0xFFDCFCE7)
                                        else -> Color(0xFFF1F5F9)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                when {
                                    isMissing -> "MISSING OMR!"
                                    isPresent && isScanned -> "OK (Evaluated)"
                                    else -> "Absent"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isMissing -> Color(0xFFDC2626)
                                    isPresent && isScanned -> Color(0xFF16A34A)
                                    else -> Color(0xFF64748B)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
