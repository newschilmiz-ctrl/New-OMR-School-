import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

old_report_tab = """    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Evaluation & Reports Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF1B5E20))
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Scanned", style = MaterialTheme.typography.titleSmall)
                    Text("${results.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val passCount = results.count { it.score >= exam.passMarks }
                    Text("Passed Students", style = MaterialTheme.typography.titleSmall)
                    Text("$passCount", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        val reports = listOf(
            "Rank List" to "Top scoring students",
            "Merit List" to "Students grouped by grades",
            "Pass / Fail Report" to "Status based on Pass Marks",
            "Topper Report" to "Top 10 performing students",
            "Subject Analysis" to "Average marks for the subject",
            "Question-wise Analysis" to "Difficulty and accuracy per question",
            "Attendance Report" to "Scanned vs Total Enrolled",
            "CSV Exporter" to "Export all data as CSV format"
        )
        
        Text("Available Reports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        reports.forEach { (title, desc) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { 
                        if (title == "CSV Exporter") {
                            pendingReport = title
                            csvLauncher.launch("results.csv")
                        }
                    }) {
                        Text("Generate")
                    }
                }
            }
        }
    }"""

new_report_tab = """    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Evaluation & Reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF1B5E20))
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Scanned", style = MaterialTheme.typography.labelMedium)
                    Text("${results.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val passCount = results.count { it.score >= exam.passMarks }
                    Text("Passed", style = MaterialTheme.typography.labelMedium)
                    Text("$passCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        val reports = listOf(
            "Rank List" to "Top scoring students",
            "Merit List" to "Students grouped by grades",
            "Pass / Fail Report" to "Status based on Pass Marks",
            "Topper Report" to "Top 10 performing students",
            "Subject Analysis" to "Average marks for the subject",
            "Question-wise Analysis" to "Difficulty and accuracy per question",
            "Attendance Report" to "Scanned vs Total Enrolled",
            "CSV Exporter" to "Export all data as CSV format"
        )
        
        Text("Available Reports", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        reports.forEach { (title, desc) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { 
                        if (title == "CSV Exporter") {
                            pendingReport = title
                            csvLauncher.launch("results.csv")
                        }
                    }, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text("Generate", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }"""

content = content.replace(old_report_tab, new_report_tab)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
