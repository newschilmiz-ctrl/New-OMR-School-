import re

with open("app/src/main/java/com/example/ui/OmrViewModel.kt", "r") as f:
    content = f.read()

content = content.replace(
    "    private fun fetchStudents() {\n        viewModelScope.launch {\n            val list = com.example.util.CloudSyncManager.fetchStudents()\n            _students.value = list\n        }\n    }",
    "    private fun fetchStudents() {\n        viewModelScope.launch {\n            isLoadingStudents.value = true\n            val list = com.example.util.CloudSyncManager.fetchStudents()\n            _students.value = list\n            isLoadingStudents.value = false\n        }\n    }"
)

content = content.replace(
    "    private fun fetchExams() {\n        viewModelScope.launch {\n            val list = com.example.util.CloudSyncManager.fetchExams()\n            _exams.value = list\n        }\n    }",
    "    private fun fetchExams() {\n        viewModelScope.launch {\n            isLoadingExams.value = true\n            val list = com.example.util.CloudSyncManager.fetchExams()\n            _exams.value = list\n            isLoadingExams.value = false\n        }\n    }"
)

with open("app/src/main/java/com/example/ui/OmrViewModel.kt", "w") as f:
    f.write(content)
print("done")
