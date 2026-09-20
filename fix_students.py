import re

with open("app/src/main/java/com/example/ui/StudentsScreen.kt", "r") as f:
    content = f.read()

content = content.replace("    val students by viewModel.students.collectAsStateWithLifecycle()", "    val students by viewModel.students.collectAsStateWithLifecycle()\n    val isLoadingStudents by viewModel.isLoadingStudents.collectAsStateWithLifecycle()")

list_logic = """            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(students) { student ->"""

list_logic_new = """            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                if (isLoadingStudents) {
                    items(6) {
                        SkeletonStudentCard()
                    }
                } else if (students.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No students added yet.", color = Color.Gray)
                        }
                    }
                } else {
                items(students) { student ->"""

content = content.replace(list_logic, list_logic_new)
content = content.replace("                            }                        }                    }                }            }        }    }    // Delete Dialog", "                            }                        }                    }                }                }            }        }    }    // Delete Dialog")

skeleton_card = """@Composable
fun SkeletonStudentCard() {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Box(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.3f).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
            }
        }
    }
}
"""

content = content + "\n" + skeleton_card

with open("app/src/main/java/com/example/ui/StudentsScreen.kt", "w") as f:
    f.write(content)
print("done")
