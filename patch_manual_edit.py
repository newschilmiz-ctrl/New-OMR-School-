import re

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    content = f.read()

# Replace the loop calculation
target_calc = """    val students by viewModel.students.collectAsStateWithLifecycle()
    val correctAnswersList = viewModel.converters.toList(key!!.correctAnswers)
    
    var correct = 0
    var wrong = 0
    var empty = 0
    val statuses = mutableListOf<Int>() // 1 = correct, 0 = wrong, -1 = empty

    for (i in 0 until key!!.numQuestions) {
        val studentAns = if (i < result.answers.size) result.answers[i] else -1
        val correctAns = if (i < correctAnswersList.size) correctAnswersList[i] else -1

        if (studentAns == -1) {
            empty++
            statuses.add(-1)
        } else if (studentAns == correctAns) {
            correct++
            statuses.add(1)
        } else {
            wrong++
            statuses.add(0)
        }
    }
    
    val attempted = key!!.numQuestions - empty
    val rawScore = correct * (exam?.marksPerQuestion ?: 1f) - (attempted - correct) * (exam?.negativeMarks ?: 0f) + (exam?.bonusMarks ?: 0f)
    val score = Math.max(0f, rawScore)"""

replacement_calc = """    val students by viewModel.students.collectAsStateWithLifecycle()
    val correctAnswersList = viewModel.converters.toList(key!!.correctAnswers)
    
    var editedAnswers by remember { mutableStateOf(result.answers.toList()) }
    var showEditDialogForQ by remember { mutableStateOf<Int?>(null) }
    
    var correct = 0
    var wrong = 0
    var empty = 0
    val statuses = mutableListOf<Int>() // 1 = correct, 0 = wrong, -1 = empty

    for (i in 0 until key!!.numQuestions) {
        val studentAns = if (i < editedAnswers.size) editedAnswers[i] else -1
        val correctAns = if (i < correctAnswersList.size) correctAnswersList[i] else -1

        if (studentAns == -1) {
            empty++
            statuses.add(-1)
        } else if (studentAns == correctAns) {
            correct++
            statuses.add(1)
        } else {
            wrong++
            statuses.add(0)
        }
    }
    
    val attempted = key!!.numQuestions - empty
    val rawScore = correct * (exam?.marksPerQuestion ?: 1f) - (attempted - correct) * (exam?.negativeMarks ?: 0f) + (exam?.bonusMarks ?: 0f)
    val score = Math.max(0f, rawScore)"""

content = content.replace(target_calc, replacement_calc)

# Replace the autoSaved launch effect to use editedAnswers and avoid auto saving when editing
target_auto = """    var autoSaved by remember { mutableStateOf(false) }

    LaunchedEffect(result, key) {
        if (!autoSaved && key != null && isRollMatch) {
            viewModel.saveScanResult(exam.id, result.studentId, result.paperSet, score, key!!.numQuestions, result.answers, statuses) {
                autoSaved = true
            }
        }
    }"""

replacement_auto = """    var autoSaved by remember { mutableStateOf(false) }

    LaunchedEffect(result, key) {
        if (!autoSaved && key != null && isRollMatch) {
            viewModel.saveScanResult(exam.id, result.studentId, result.paperSet, score, key!!.numQuestions, editedAnswers, statuses) {
                autoSaved = true
            }
        }
    }
    
    if (showEditDialogForQ != null) {
        val qIndex = showEditDialogForQ!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEditDialogForQ = null },
            title = { Text("Edit Answer for Q${qIndex + 1}") },
            text = {
                Column {
                    Text("Select the correct answer bubbled by student:")
                    Spacer(modifier = Modifier.height(8.dp))
                    val options = listOf("A", "B", "C", "D", "Empty")
                    val currentAns = if (qIndex < editedAnswers.size) editedAnswers[qIndex] else -1
                    options.forEachIndexed { optIndex, optText ->
                        val value = if (optText == "Empty") -1 else optIndex
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().androidx.compose.foundation.clickable {
                                val newAnswers = editedAnswers.toMutableList()
                                if (qIndex >= newAnswers.size) {
                                    while (newAnswers.size <= qIndex) newAnswers.add(-1)
                                }
                                newAnswers[qIndex] = value
                                editedAnswers = newAnswers
                                autoSaved = false // allow saving again
                                showEditDialogForQ = null
                            }.padding(8.dp)
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = currentAns == value,
                                onClick = {
                                    val newAnswers = editedAnswers.toMutableList()
                                    if (qIndex >= newAnswers.size) {
                                        while (newAnswers.size <= qIndex) newAnswers.add(-1)
                                    }
                                    newAnswers[qIndex] = value
                                    editedAnswers = newAnswers
                                    autoSaved = false // allow saving again
                                    showEditDialogForQ = null
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(optText)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showEditDialogForQ = null }) { Text("Cancel") }
            }
        )
    }"""

content = content.replace(target_auto, replacement_auto)

# Replace the grid UI to make it clickable
target_grid = """                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .background(color, shape = androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {"""

replacement_grid = """                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
                                .androidx.compose.foundation.clickable { showEditDialogForQ = i },
                            contentAlignment = Alignment.Center
                        ) {"""

content = content.replace(target_grid, replacement_grid)


# In the save button, use editedAnswers
content = content.replace(
    "viewModel.saveScanResult(exam.id, result.studentId, result.paperSet, score, key!!.numQuestions, result.answers, statuses)",
    "viewModel.saveScanResult(exam.id, result.studentId, result.paperSet, score, key!!.numQuestions, editedAnswers, statuses)"
)

# And add hint above the grid
content = content.replace(
    "Text(\"Question Analysis\", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)",
    "Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(\"Question Analysis\", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(\"Tap a question circle to manually edit the answer\", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f)) }"
)


with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(content)

print("Patch applied to ScanOmrScreen.kt")
