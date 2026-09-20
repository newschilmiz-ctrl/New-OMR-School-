import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

anchor = """            if (pendingAction == "generatePapers") {
                isGenerating = true
                generateQuestionPapersInternal(context, exam, questions, uri, viewModel) {
                    isGenerating = false
                }"""

new = """            if (pendingAction == "generatePapers") {
                isGenerating = true
                generateQuestionPapersInternal(context, exam, questions, uri, viewModel, headerText, runByText, directorText, addressText, false, null) {
                    isGenerating = false
                }"""

content = content.replace(anchor, new)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("Replaced in launcher")
