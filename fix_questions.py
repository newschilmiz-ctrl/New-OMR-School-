import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

old_header = """        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Questions (${questions.size}/100)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                Button(onClick = { pendingAction = "downloadAnswerKey"; createDocumentLauncher.launch("${exam.subject}_AnswerKey.pdf") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp)) {
                    Text(if (isGenerating && pendingAction == "downloadAnswerKey") "Key" else "Answer Key")
                }
                Button(onClick = { showJsonDialog = true }, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp)) {
                    Text("Import JSON")
                }
                Button(onClick = {
                    if (questions.size < 100) {
                        viewModel.saveQuestion(QuestionEntity(examId = exam.id, text = "", optionA = "", optionB = "", optionC = "", optionD = "", correctIndex = 0)) {}
                    }
                }, shape = RoundedCornerShape(12.dp)) {
                    Text("Add")
                }
            }
        }"""

new_header = """        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Questions (${questions.size}/100)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showSettingsDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pendingAction = "downloadAnswerKey"; createDocumentLauncher.launch("${exam.subject}_AnswerKey.pdf") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                Text(if (isGenerating && pendingAction == "downloadAnswerKey") "Key" else "Ans Key", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(onClick = { showJsonDialog = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                Text("Import JSON", style = MaterialTheme.typography.labelSmall)
            }
            Button(onClick = {
                if (questions.size < 100) {
                    viewModel.saveQuestion(QuestionEntity(examId = exam.id, text = "", optionA = "", optionB = "", optionC = "", optionD = "", correctIndex = 0)) {}
                }
            }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                Text("Add Question", style = MaterialTheme.typography.labelSmall)
            }
        }"""

content = content.replace(old_header, new_header)

old_card = """                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Question ${index + 1}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.deleteQuestion(q) {} }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        OutlinedTextField(
                            value = q.text,
                            onValueChange = { 
                                 questions[index] = q.copy(text = it)
                                viewModel.updateQuestion(questions[index]) {} 
                             },
                            label = { Text("Question Text") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = q.optionA,
                            onValueChange = { 
                                 questions[index] = q.copy(optionA = it)
                                viewModel.updateQuestion(questions[index]) {} 
                             },
                            label = { Text("Option A") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = q.optionB,
                            onValueChange = { 
                                 questions[index] = q.copy(optionB = it)
                                viewModel.updateQuestion(questions[index]) {} 
                             },
                            label = { Text("Option B") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = q.optionC,
                            onValueChange = { 
                                 questions[index] = q.copy(optionC = it)
                                viewModel.updateQuestion(questions[index]) {} 
                             },
                            label = { Text("Option C") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = q.optionD,
                            onValueChange = { 
                                 questions[index] = q.copy(optionD = it)
                                viewModel.updateQuestion(questions[index]) {} 
                             },
                            label = { Text("Option D") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Correct Answer:", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val labels = listOf("A", "B", "C", "D")
                            for (i in 0..3) {
                                FilterChip(
                                    selected = q.correctIndex == i,
                                    onClick = { 
                                         questions[index] = q.copy(correctIndex = i)
                                        viewModel.updateQuestion(questions[index]) {} 
                                     },
                                    label = { Text(labels[i]) }
                                )
                            }
                        }
                    }
                }"""

new_card = """                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Q${index + 1}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { viewModel.deleteQuestion(q) {} }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                        OutlinedTextField(
                            value = q.text,
                            onValueChange = { 
                                questions[index] = q.copy(text = it)
                                viewModel.updateQuestion(questions[index]) {} 
                            },
                            placeholder = { Text("Question text...", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(8.dp),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = q.optionA,
                                onValueChange = { 
                                    questions[index] = q.copy(optionA = it)
                                    viewModel.updateQuestion(questions[index]) {} 
                                },
                                placeholder = { Text("Option A", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall,
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = q.optionB,
                                onValueChange = { 
                                    questions[index] = q.copy(optionB = it)
                                    viewModel.updateQuestion(questions[index]) {} 
                                },
                                placeholder = { Text("Option B", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall,
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = q.optionC,
                                onValueChange = { 
                                    questions[index] = q.copy(optionC = it)
                                    viewModel.updateQuestion(questions[index]) {} 
                                },
                                placeholder = { Text("Option C", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall,
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = q.optionD,
                                onValueChange = { 
                                    questions[index] = q.copy(optionD = it)
                                    viewModel.updateQuestion(questions[index]) {} 
                                },
                                placeholder = { Text("Option D", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall,
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Ans:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val labels = listOf("A", "B", "C", "D")
                                for (i in 0..3) {
                                    FilterChip(
                                        selected = q.correctIndex == i,
                                        onClick = { 
                                            questions[index] = q.copy(correctIndex = i)
                                            viewModel.updateQuestion(questions[index]) {} 
                                        },
                                        label = { Text(labels[i], style = MaterialTheme.typography.bodySmall) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }"""

content = content.replace(old_card, new_card)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
