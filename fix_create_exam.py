import re

with open("app/src/main/java/com/example/ui/CreateExamScreen.kt", "r") as f:
    text = f.read()

text = text.replace('var bonusMarks by remember { mutableStateOf("0.0") }',
'''var bonusMarks by remember { mutableStateOf("0.0") }
    var templateType by remember { mutableStateOf("Standard") }
    var templateDropdownExpanded by remember { mutableStateOf(false) }''')

text = text.replace('''                    val bns = bonusMarks.toFloatOrNull() ?: 0f
                    
                    viewModel.createExam(
                        examName, selectedSubject, examDate, examTitle, 
                        logoPath, logoOpacity, logoSize, logoPosition,
                        mk, neg, ps, bns
                    ) { examId ->''',
'''                    val bns = bonusMarks.toFloatOrNull() ?: 0f
                    
                    viewModel.createExam(
                        examName, selectedSubject, examDate, examTitle, 
                        logoPath, logoOpacity, logoSize, logoPosition,
                        mk, neg, ps, bns, templateType
                    ) { examId ->''')

# Add UI for templateType right before the submit button or after bonusMarks.
# Let's find:
#                 OutlinedTextField(
#                     value = bonusMarks,

ui_block = '''
            ExposedDropdownMenuBox(
                expanded = templateDropdownExpanded,
                onExpandedChange = { templateDropdownExpanded = !templateDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = if (templateType == "Standard") "Standard (All Details + QR)" else "Simple (Roll No Only)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("OMR Template") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateDropdownExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = templateDropdownExpanded,
                    onDismissRequest = { templateDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Standard (All Details + QR)") },
                        onClick = {
                            templateType = "Standard"
                            templateDropdownExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Simple (Roll No Only)") },
                        onClick = {
                            templateType = "RollNoOnly"
                            templateDropdownExpanded = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
'''

text = re.sub(r'Button\(\s*onClick = \{\s*val mk =', ui_block.strip() + r'\n            onClick = {\n                    val mk =', text)

with open("app/src/main/java/com/example/ui/CreateExamScreen.kt", "w") as f:
    f.write(text)

