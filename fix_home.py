import re
with open("app/src/main/java/com/example/ui/HomeScreen.kt", "r") as f:
    content = f.read()
content = content.replace("val exams by viewModel.exams.collectAsStateWithLifecycle()", "val exams by viewModel.exams.collectAsStateWithLifecycle()\n    val isLoadingExams by viewModel.isLoadingExams.collectAsStateWithLifecycle()")
with open("app/src/main/java/com/example/ui/HomeScreen.kt", "w") as f:
    f.write(content)
