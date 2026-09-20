import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

old_tab = """    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Exam Day Operations", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF1B5E20))
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("1. Attendance (उपस्थिति)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Mark attendance for students. Check the box if present.", style = MaterialTheme.typography.bodyMedium)
        
        Card(modifier = Modifier.fillMaxWidth().height(300.dp).padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.padding(8.dp)) {
                items(enrolledStudents.size) { i ->
                    val student = enrolledStudents[i]
                    val isPresent = attendanceMap[student.rollNo] ?: false
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp).clickable {
                        val newMap = attendanceMap.toMutableMap()
                        newMap[student.rollNo] = !isPresent
                        viewModel.attendanceMap.value = newMap
                    }, verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = isPresent,
                            onCheckedChange = { checked ->
                                val newMap = attendanceMap.toMutableMap()
                                newMap[student.rollNo] = checked
                                viewModel.attendanceMap.value = newMap
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(student.name, fontWeight = FontWeight.Bold)
                            Text("Roll No: ${student.rollNo}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
        
        OutlinedButton(
            onClick = {
                pendingAction = "AttendanceReport"
                createDocumentLauncher.launch("${exam.name}_Attendance.pdf")
            },
            enabled = enrolledStudents.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Download Attendance Report PDF")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("2. Seating Arrangement (सीटिंग प्लान)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = {
                pendingAction = "SeatingPlan"
                createDocumentLauncher.launch("${exam.name}_SeatingPlan.pdf")
            },
            enabled = enrolledStudents.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Generate Seating Plan PDF")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("3. OMR Errors / Pending List", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("List of scanned OMRs with invalid roll numbers, multiple selected options, or missing sets.", style = MaterialTheme.typography.bodyMedium)
        
        val results by viewModel.getScanResultsForExam(examId).collectAsStateWithLifecycle()
        // Simple mock of pending/error checking:
        val errorResults = results.filter { it.paperSet.isEmpty() || it.studentAnswers.contains("[-1,-1]") || it.studentId.isEmpty() }
        
        if (errorResults.isEmpty()) {
            Text("No pending or error OMRs found.", color = androidx.compose.ui.graphics.Color(0xFF1B5E20), modifier = Modifier.padding(vertical = 16.dp))
        } else {
            errorResults.forEach { res ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Unknown Roll / Set: ${res.paperSet.ifEmpty { "MISSING" }}", fontWeight = FontWeight.Bold)
                        Text("Score: ${res.score}", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { /* Handle manual resolve */ }, modifier = Modifier.align(Alignment.End)) {
                            Text("Resolve Manually")
                        }
                    }
                }
            }
        }
    }"""

new_tab = """    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Exam Day Operations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF1B5E20))
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("1. Attendance (उपस्थिति)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Mark attendance for students. Check the box if present.", style = MaterialTheme.typography.bodySmall)
        
        Card(modifier = Modifier.fillMaxWidth().height(250.dp).padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.padding(4.dp)) {
                items(enrolledStudents.size) { i ->
                    val student = enrolledStudents[i]
                    val isPresent = attendanceMap[student.rollNo] ?: false
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        val newMap = attendanceMap.toMutableMap()
                        newMap[student.rollNo] = !isPresent
                        viewModel.attendanceMap.value = newMap
                    }.padding(vertical = 4.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = isPresent,
                            onCheckedChange = { checked ->
                                val newMap = attendanceMap.toMutableMap()
                                newMap[student.rollNo] = checked
                                viewModel.attendanceMap.value = newMap
                            },
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("Roll No: ${student.rollNo}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
        
        OutlinedButton(
            onClick = {
                pendingAction = "AttendanceReport"
                createDocumentLauncher.launch("${exam.name}_Attendance.pdf")
            },
            enabled = enrolledStudents.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Download Attendance PDF", style = MaterialTheme.typography.labelMedium)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text("2. Seating Arrangement", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        
        OutlinedButton(
            onClick = {
                pendingAction = "SeatingPlan"
                createDocumentLauncher.launch("${exam.name}_SeatingPlan.pdf")
            },
            enabled = enrolledStudents.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Generate Seating Plan PDF", style = MaterialTheme.typography.labelMedium)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text("3. OMR Errors", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("List of scanned OMRs with invalid roll numbers, multiple selected options, or missing sets.", style = MaterialTheme.typography.bodySmall)
        
        val results by viewModel.getScanResultsForExam(examId).collectAsStateWithLifecycle()
        // Simple mock of pending/error checking:
        val errorResults = results.filter { it.paperSet.isEmpty() || it.studentAnswers.contains("[-1,-1]") || it.studentId.isEmpty() }
        
        if (errorResults.isEmpty()) {
            Text("No pending or error OMRs found.", color = androidx.compose.ui.graphics.Color(0xFF1B5E20), modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
        } else {
            errorResults.forEach { res ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Unknown Roll / Set: ${res.paperSet.ifEmpty { "MISSING" }}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text("Score: ${res.score}", style = MaterialTheme.typography.labelSmall)
                        Button(onClick = { /* Handle manual resolve */ }, modifier = Modifier.align(Alignment.End).height(32.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Resolve Manually", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }"""

content = content.replace(old_tab, new_tab)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
