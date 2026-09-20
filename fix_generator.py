import re

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "r") as f:
    content = f.read()

sig_anchor = """private fun generateQuestionPapersInternal(
    context: Context,
    exam: Exam,
    baseQuestions: List<QuestionEntity>,
    uri: android.net.Uri,
    viewModel: OmrViewModel,
    onDone: () -> Unit
) {"""

sig_new = """private fun generateQuestionPapersInternal(
    context: Context,
    exam: Exam,
    baseQuestions: List<QuestionEntity>,
    uri: android.net.Uri?,
    viewModel: OmrViewModel,
    headerText: String,
    runByText: String,
    directorText: String,
    addressText: String,
    isPreview: Boolean,
    providedPdfDoc: android.graphics.pdf.PdfDocument?,
    onDone: () -> Unit
) {"""

content = content.replace(sig_anchor, sig_new)

with open("app/src/main/java/com/example/ui/ExamDashboardScreen.kt", "w") as f:
    f.write(content)
print("Replaced sig")
