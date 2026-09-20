import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

old_tabs = """            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = androidx.compose.ui.graphics.Color.White,
                contentColor = androidx.compose.ui.graphics.Color(0xFF1B5E20),
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = androidx.compose.ui.graphics.Color(0xFF1B5E20)
                        )
                    }
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Generate", fontWeight = FontWeight.SemiBold) }, icon = { Icon(Icons.Default.Download, null) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Questions", fontWeight = FontWeight.SemiBold) }, icon = { Icon(Icons.Default.Assignment, null) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Scanner", fontWeight = FontWeight.SemiBold) }, icon = { Icon(Icons.Default.CameraAlt, null) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Exam Day", fontWeight = FontWeight.SemiBold) }, icon = { Icon(Icons.Default.Event, null) })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Reports", fontWeight = FontWeight.SemiBold) }, icon = { Icon(Icons.Default.Assessment, null) })
            }"""

new_tabs = """            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = androidx.compose.ui.graphics.Color.White,
                contentColor = androidx.compose.ui.graphics.Color(0xFF1B5E20),
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = androidx.compose.ui.graphics.Color(0xFF1B5E20)
                        )
                    }
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Generate", style = MaterialTheme.typography.labelSmall) }, icon = { Icon(Icons.Default.Download, null, modifier = Modifier.size(20.dp)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Questions", style = MaterialTheme.typography.labelSmall) }, icon = { Icon(Icons.Default.Assignment, null, modifier = Modifier.size(20.dp)) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Scanner", style = MaterialTheme.typography.labelSmall) }, icon = { Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp)) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Exam Day", style = MaterialTheme.typography.labelSmall) }, icon = { Icon(Icons.Default.Event, null, modifier = Modifier.size(20.dp)) })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Reports", style = MaterialTheme.typography.labelSmall) }, icon = { Icon(Icons.Default.Assessment, null, modifier = Modifier.size(20.dp)) })
            }"""

content = content.replace(old_tabs, new_tabs)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
