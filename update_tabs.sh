sed -i '157,163c\
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Exam Day", fontWeight = FontWeight.SemiBold) }, icon = { Icon(Icons.Default.Event, null) })\
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Reports", fontWeight = FontWeight.SemiBold) }, icon = { Icon(Icons.Default.Assessment, null) })\
            }\
            when (selectedTab) {\
                0 -> GenerateTab(navController, viewModel, examId, exam)\
                1 -> if (exam != null) CreateQuestionPaperTabInternal(viewModel, exam!!)\
                2 -> ScannerTab(navController, viewModel, examId)\
                3 -> if (exam != null) ExamDayTab(navController, viewModel, examId, exam)\
                4 -> if (exam != null) ReportsTab(viewModel, examId, exam)\
            }' app/src/main/java/com/example/ui/ExamDashboardScreen.kt
