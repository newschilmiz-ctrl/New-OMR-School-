package com.example.util

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.Converters
import com.example.data.Exam
import com.example.data.ScanResult
import com.example.data.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RankedStudentResult(
    val rank: Int,
    val result: ScanResult,
    val student: Student?,
    val percentage: Float,
    val isPassed: Boolean,
    val grade: String
)

object ReportPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun getRankedResults(exam: Exam, results: List<ScanResult>, students: List<Student>): List<RankedStudentResult> {
        val totalQ = results.firstOrNull()?.totalQuestions?.takeIf { it > 0 } ?: 100
        val totalMarks = if (exam.marksPerQuestion > 0) {
            totalQ * exam.marksPerQuestion
        } else {
            100f
        }
        val sorted = results.sortedByDescending { it.score }
        return sorted.mapIndexed { index, r ->
            val st = students.find { it.rollNo.equals(r.studentId, ignoreCase = true) }
            val pct = if (totalMarks > 0) (r.score / totalMarks) * 100f else 0f
            val passed = r.score >= exam.passMarks
            val grade = when {
                pct >= 80f -> "A+ (Distinction)"
                pct >= 60f -> "A (First Div)"
                pct >= 45f -> "B (Second Div)"
                passed -> "C (Third Div)"
                else -> "F (Failed)"
            }
            RankedStudentResult(
                rank = index + 1,
                result = r,
                student = st,
                percentage = pct,
                isPassed = passed,
                grade = grade
            )
        }
    }

    // 1. RANK LIST PDF
    suspend fun generateRankListPdf(
        context: Context,
        exam: Exam,
        results: List<ScanResult>,
        students: List<Student>,
        uri: Uri
    ) = withContext(Dispatchers.IO) {
        val ranked = getRankedResults(exam, results, students)
        val pdf = PdfDocument()
        try {
            val titlePaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 15f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint().apply {
                color = AndroidColor.DKGRAY
                textSize = 10f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val headerPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 10f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 9.5f
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = AndroidColor.LTGRAY
                strokeWidth = 1f
            }

            val rowsPerPage = 32
            val chunks = if (ranked.isNotEmpty()) ranked.chunked(rowsPerPage) else listOf(emptyList())
            val totalPages = chunks.size

            chunks.forEachIndexed { pageIndex, chunk ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas

                // Header
                canvas.drawText(exam.title, PAGE_WIDTH / 2f, 40f, titlePaint)
                canvas.drawText("OFFICIAL RANK LIST & SCORE GAZETTE", PAGE_WIDTH / 2f, 58f, titlePaint)
                canvas.drawText("Exam: ${exam.name}  |  Subject: ${exam.subject}  |  Pass Cutoff: ${exam.passMarks.toInt()} Marks", PAGE_WIDTH / 2f, 76f, subPaint)

                // Summary Strip
                val passCount = ranked.count { it.isPassed }
                canvas.drawText("Total Evaluated: ${ranked.size}  |  Passed: $passCount (${if (ranked.isNotEmpty()) (passCount * 100) / ranked.size else 0}%)", PAGE_WIDTH / 2f, 92f, subPaint)

                // Table Header
                var y = 120f
                canvas.drawLine(MARGIN, y - 14f, PAGE_WIDTH - MARGIN, y - 14f, linePaint)
                canvas.drawText("Rank", MARGIN, y, headerPaint)
                canvas.drawText("Roll No", MARGIN + 40f, y, headerPaint)
                canvas.drawText("Candidate Name", MARGIN + 110f, y, headerPaint)
                canvas.drawText("Gender", MARGIN + 270f, y, headerPaint)
                canvas.drawText("Set", MARGIN + 325f, y, headerPaint)
                canvas.drawText("Score", MARGIN + 365f, y, headerPaint)
                canvas.drawText("Percent", MARGIN + 425f, y, headerPaint)
                canvas.drawText("Status", MARGIN + 475f, y, headerPaint)
                y += 6f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
                y += 16f

                for (item in chunk) {
                    canvas.drawText("#${item.rank}", MARGIN, y, textPaint)
                    canvas.drawText(item.result.studentId, MARGIN + 40f, y, textPaint)
                    val name = item.student?.name ?: "Student ${item.result.studentId}"
                    val truncatedName = if (name.length > 22) name.substring(0, 20) + ".." else name
                    canvas.drawText(truncatedName, MARGIN + 110f, y, textPaint)
                    canvas.drawText(item.student?.gender ?: "-", MARGIN + 270f, y, textPaint)
                    canvas.drawText(item.result.paperSet.ifEmpty { "-" }, MARGIN + 325f, y, textPaint)
                    canvas.drawText(String.format("%.1f", item.result.score), MARGIN + 365f, y, textPaint)
                    canvas.drawText(String.format("%.1f%%", item.percentage), MARGIN + 425f, y, textPaint)

                    val statusPaint = Paint(textPaint).apply {
                        color = if (item.isPassed) 0xFF16A34A.toInt() else 0xFFDC2626.toInt()
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.drawText(if (item.isPassed) "PASS" else "FAIL", MARGIN + 475f, y, statusPaint)

                    y += 4f
                    canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
                    y += 16f
                }

                // Footer
                val footerPaint = Paint().apply {
                    color = AndroidColor.GRAY
                    textSize = 8.5f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Page ${pageIndex + 1} of $totalPages  •  Institutional OMR Assessment Engine", PAGE_WIDTH / 2f, PAGE_HEIGHT - 25f, footerPaint)

                pdf.finishPage(page)
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdf.writeTo(out)
            }
        } finally {
            pdf.close()
        }
    }

    // 2. MERIT LIST PDF
    suspend fun generateMeritListPdf(
        context: Context,
        exam: Exam,
        results: List<ScanResult>,
        students: List<Student>,
        uri: Uri
    ) = withContext(Dispatchers.IO) {
        val ranked = getRankedResults(exam, results, students)
        val pdf = PdfDocument()
        try {
            val titlePaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 15f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint().apply {
                color = AndroidColor.DKGRAY
                textSize = 10f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val groupHeaderPaint = Paint().apply {
                color = 0xFF1E293B.toInt()
                textSize = 11f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 9.5f
                isAntiAlias = true
            }

            val grades = listOf(
                "A+ (Distinction - 80%+)",
                "A (First Division - 60% to 79%)",
                "B (Second Division - 45% to 59%)",
                "C (Third Division - Pass to 44%)",
                "Needs Improvement / Fail"
            )

            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawText(exam.title, PAGE_WIDTH / 2f, 40f, titlePaint)
            canvas.drawText("MERIT CLASSIFICATION & DIVISION REPORT", PAGE_WIDTH / 2f, 58f, titlePaint)
            canvas.drawText("Exam: ${exam.name}  |  Subject: ${exam.subject}", PAGE_WIDTH / 2f, 76f, subPaint)

            var y = 105f
            for (gradeTitle in grades) {
                val groupItems = when {
                    gradeTitle.startsWith("A+") -> ranked.filter { it.percentage >= 80f }
                    gradeTitle.startsWith("A ") -> ranked.filter { it.percentage in 60f..79.99f }
                    gradeTitle.startsWith("B ") -> ranked.filter { it.percentage in 45f..59.99f }
                    gradeTitle.startsWith("C ") -> ranked.filter { it.isPassed && it.percentage < 45f }
                    else -> ranked.filter { !it.isPassed }
                }

                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, Paint().apply { color = 0xFFF1F5F9.toInt() })
                canvas.drawText("$gradeTitle  (${groupItems.size} Candidates)", MARGIN + 8f, y + 14f, groupHeaderPaint)
                y += 26f

                if (groupItems.isEmpty()) {
                    canvas.drawText("No candidates in this grade band.", MARGIN + 12f, y + 10f, textPaint.apply { color = AndroidColor.GRAY })
                    y += 20f
                } else {
                    for (item in groupItems.take(7)) {
                        val name = item.student?.name ?: "Student ${item.result.studentId}"
                        canvas.drawText("#${item.rank}   Roll: ${item.result.studentId}   Name: $name   Score: ${item.result.score} (${String.format("%.1f%%", item.percentage)})", MARGIN + 12f, y + 10f, textPaint)
                        y += 15f
                    }
                    if (groupItems.size > 7) {
                        canvas.drawText("+ ${groupItems.size - 7} more candidates...", MARGIN + 12f, y + 10f, textPaint.apply { color = AndroidColor.DKGRAY })
                        y += 16f
                    }
                }
                y += 10f
            }

            canvas.drawText("Page 1 of 1  •  Official Institutional Merit Register", PAGE_WIDTH / 2f, PAGE_HEIGHT - 25f, Paint().apply {
                color = AndroidColor.GRAY
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
            })

            pdf.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdf.writeTo(out)
            }
        } finally {
            pdf.close()
        }
    }

    // 3. PASS / FAIL SUMMARY PDF
    suspend fun generatePassFailSummaryPdf(
        context: Context,
        exam: Exam,
        results: List<ScanResult>,
        students: List<Student>,
        uri: Uri
    ) = withContext(Dispatchers.IO) {
        val ranked = getRankedResults(exam, results, students)
        val pdf = PdfDocument()
        try {
            val titlePaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 15f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint().apply {
                color = AndroidColor.DKGRAY
                textSize = 10f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val textPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 10f
                isAntiAlias = true
            }

            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawText(exam.title, PAGE_WIDTH / 2f, 40f, titlePaint)
            canvas.drawText("EXECUTIVE PASS / FAIL GAZETTE & DEMOGRAPHIC AUDIT", PAGE_WIDTH / 2f, 58f, titlePaint)
            canvas.drawText("Exam: ${exam.name}  |  Subject: ${exam.subject}", PAGE_WIDTH / 2f, 76f, subPaint)

            val total = ranked.size
            val passed = ranked.count { it.isPassed }
            val failed = total - passed
            val passRate = if (total > 0) (passed * 100) / total else 0

            val boys = ranked.filter { it.student?.gender?.equals("Male", ignoreCase = true) == true }
            val girls = ranked.filter { it.student?.gender?.equals("Female", ignoreCase = true) == true }
            val boysPass = boys.count { it.isPassed }
            val girlsPass = girls.count { it.isPassed }

            var y = 110f
            // Draw Overview Box
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 80f, Paint().apply { color = 0xFFF8FAFC.toInt() })
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 80f, Paint().apply { color = 0xFFCBD5E1.toInt(); style = Paint.Style.STROKE; strokeWidth = 1f })

            canvas.drawText("OVERALL RESULT SUMMARY", MARGIN + 14f, y + 22f, Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 11f })
            canvas.drawText("Total Evaluated: $total Candidates", MARGIN + 14f, y + 42f, textPaint)
            canvas.drawText("Passed: $passed ($passRate%)", MARGIN + 14f, y + 60f, Paint(textPaint).apply { color = 0xFF16A34A.toInt() })
            canvas.drawText("Failed: $failed (${100 - passRate}%)", MARGIN + 220f, y + 60f, Paint(textPaint).apply { color = 0xFFDC2626.toInt() })
            canvas.drawText("Passing Cutoff: ${exam.passMarks.toInt()} Marks", MARGIN + 220f, y + 42f, textPaint)

            y += 105f
            canvas.drawText("GENDER-WISE PERFORMANCE AUDIT", MARGIN, y, Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 11f })
            y += 16f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = AndroidColor.BLACK; strokeWidth = 1.5f })
            y += 16f

            canvas.drawText("Category", MARGIN, y, Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 10f })
            canvas.drawText("Appeared", MARGIN + 120f, y, Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 10f })
            canvas.drawText("Passed", MARGIN + 220f, y, Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 10f })
            canvas.drawText("Failed", MARGIN + 320f, y, Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 10f })
            canvas.drawText("Success Rate", MARGIN + 420f, y, Paint().apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 10f })

            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = AndroidColor.LTGRAY; strokeWidth = 1f })
            y += 18f

            val rows = listOf(
                Triple("Boys Candidates", boys.size, boysPass),
                Triple("Girls Candidates", girls.size, girlsPass),
                Triple("Total Aggregate", total, passed)
            )

            for ((cat, app, pass) in rows) {
                val fail = app - pass
                val rate = if (app > 0) (pass * 100) / app else 0
                canvas.drawText(cat, MARGIN, y, textPaint)
                canvas.drawText("$app", MARGIN + 120f, y, textPaint)
                canvas.drawText("$pass", MARGIN + 220f, y, textPaint)
                canvas.drawText("$fail", MARGIN + 320f, y, textPaint)
                canvas.drawText("$rate%", MARGIN + 420f, y, textPaint)
                y += 6f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = 0xFFE2E8F0.toInt(); strokeWidth = 0.8f })
                y += 18f
            }

            // Signatures block
            y = PAGE_HEIGHT - 120f
            canvas.drawLine(MARGIN, y, MARGIN + 140f, y, Paint().apply { color = AndroidColor.BLACK; strokeWidth = 1f })
            canvas.drawText("Exam Controller / Evaluator", MARGIN, y + 16f, textPaint)

            canvas.drawLine(PAGE_WIDTH - MARGIN - 140f, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = AndroidColor.BLACK; strokeWidth = 1f })
            canvas.drawText("Principal / Center Superintendent", PAGE_WIDTH - MARGIN - 140f, y + 16f, textPaint)

            pdf.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { out -> pdf.writeTo(out) }
        } finally {
            pdf.close()
        }
    }

    // 4. TOP 10 HIGH ACHIEVERS PDF
    suspend fun generateTopAchieversPdf(
        context: Context,
        exam: Exam,
        results: List<ScanResult>,
        students: List<Student>,
        uri: Uri
    ) = withContext(Dispatchers.IO) {
        val top10 = getRankedResults(exam, results, students).take(10)
        val pdf = PdfDocument()
        try {
            val titlePaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 15f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint().apply {
                color = AndroidColor.DKGRAY
                textSize = 10f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val textPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 10f
                isAntiAlias = true
            }

            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawText(exam.title, PAGE_WIDTH / 2f, 40f, titlePaint)
            canvas.drawText("CERTIFICATE OF EXCELLENCE - TOP 10 MERIT HONORS", PAGE_WIDTH / 2f, 58f, titlePaint)
            canvas.drawText("Exam: ${exam.name}  |  Subject: ${exam.subject}", PAGE_WIDTH / 2f, 76f, subPaint)

            var y = 110f
            top10.forEachIndexed { i, item ->
                val bgColor = when (i) {
                    0 -> 0xFFFEF9C3.toInt()
                    1 -> 0xFFF1F5F9.toInt()
                    2 -> 0xFFFFEDD5.toInt()
                    else -> AndroidColor.WHITE
                }
                val borderColor = when (i) {
                    0 -> 0xFFCA8A04.toInt()
                    1 -> 0xFF94A3B8.toInt()
                    2 -> 0xFFEA580C.toInt()
                    else -> 0xFFE2E8F0.toInt()
                }
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 42f, Paint().apply { color = bgColor })
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 42f, Paint().apply { color = borderColor; style = Paint.Style.STROKE; strokeWidth = 1f })

                val rankTag = when (i) {
                    0 -> "★ 1st RANK (GOLD)"
                    1 -> "★ 2nd RANK (SILVER)"
                    2 -> "★ 3rd RANK (BRONZE)"
                    else -> "#${i + 1} TOPPER"
                }
                canvas.drawText(rankTag, MARGIN + 12f, y + 16f, Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 9.5f
                    color = borderColor
                })

                val name = item.student?.name ?: "Student ${item.result.studentId}"
                canvas.drawText("$name  (Roll: ${item.result.studentId})", MARGIN + 12f, y + 32f, Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 10.5f
                })

                canvas.drawText("Marks: ${item.result.score}", PAGE_WIDTH - MARGIN - 140f, y + 24f, textPaint)
                canvas.drawText(String.format("%.1f%%", item.percentage), PAGE_WIDTH - MARGIN - 60f, y + 24f, Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 11f
                    color = 0xFF16A34A.toInt()
                })

                y += 50f
            }

            canvas.drawText("Page 1 of 1  •  Official Hall of Fame Register", PAGE_WIDTH / 2f, PAGE_HEIGHT - 25f, Paint().apply {
                color = AndroidColor.GRAY
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
            })

            pdf.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { out -> pdf.writeTo(out) }
        } finally {
            pdf.close()
        }
    }

    // 5. ITEM DIFFICULTY ANALYSIS PDF
    suspend fun generateItemDifficultyPdf(
        context: Context,
        exam: Exam,
        results: List<ScanResult>,
        uri: Uri
    ) = withContext(Dispatchers.IO) {
        val totalQuestions = (results.firstOrNull()?.totalQuestions?.takeIf { it > 0 } ?: 100)
        val converters = Converters()
        val allStatuses = results.map { converters.toList(it.questionStatuses) }

        val pdf = PdfDocument()
        try {
            val titlePaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 15f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint().apply {
                color = AndroidColor.DKGRAY
                textSize = 10f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val headerPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 9.5f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 9f
                isAntiAlias = true
            }

            val rowsPerPage = 35
            val questionIndices = (0 until totalQuestions).toList()
            val chunks = questionIndices.chunked(rowsPerPage)
            val totalPages = chunks.size.coerceAtLeast(1)

            chunks.forEachIndexed { pageIndex, chunk ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawText(exam.title, PAGE_WIDTH / 2f, 40f, titlePaint)
                canvas.drawText("ITEM DIFFICULTY & PSYCHOMETRIC ANALYSIS", PAGE_WIDTH / 2f, 58f, titlePaint)
                canvas.drawText("Exam: ${exam.name}  |  Evaluated Scripts: ${results.size}", PAGE_WIDTH / 2f, 76f, subPaint)

                var y = 110f
                canvas.drawLine(MARGIN, y - 12f, PAGE_WIDTH - MARGIN, y - 12f, Paint().apply { color = AndroidColor.BLACK; strokeWidth = 1f })
                canvas.drawText("Q.No", MARGIN, y, headerPaint)
                canvas.drawText("Total Evaluated", MARGIN + 50f, y, headerPaint)
                canvas.drawText("Correct (Count)", MARGIN + 160f, y, headerPaint)
                canvas.drawText("Accuracy %", MARGIN + 280f, y, headerPaint)
                canvas.drawText("Difficulty Level", MARGIN + 380f, y, headerPaint)
                y += 6f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = AndroidColor.BLACK; strokeWidth = 1.2f })
                y += 15f

                for (qIdx in chunk) {
                    val qNum = qIdx + 1
                    var correctCount = 0
                    for (st in allStatuses) {
                        if (qIdx < st.size && st[qIdx] == 1) {
                            correctCount++
                        }
                    }
                    val accuracy = if (results.isNotEmpty()) (correctCount * 100f) / results.size else 0f
                    val levelText: String
                    val levelColor: Int
                    if (accuracy >= 75f) {
                        levelText = "Easy (High Score)"
                        levelColor = 0xFF16A34A.toInt()
                    } else if (accuracy >= 45f) {
                        levelText = "Moderate"
                        levelColor = 0xFF2563EB.toInt()
                    } else {
                        levelText = "Difficult / Tricky"
                        levelColor = 0xFFDC2626.toInt()
                    }

                    canvas.drawText("Q#$qNum", MARGIN, y, textPaint)
                    canvas.drawText("${results.size}", MARGIN + 50f, y, textPaint)
                    canvas.drawText("$correctCount", MARGIN + 160f, y, textPaint)
                    canvas.drawText(String.format("%.1f%%", accuracy), MARGIN + 280f, y, textPaint)
                    canvas.drawText(levelText, MARGIN + 380f, y, Paint(textPaint).apply {
                        this.color = levelColor
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    })

                    y += 4f
                    canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = 0xFFF1F5F9.toInt(); strokeWidth = 0.5f })
                    y += 15f
                }

                canvas.drawText("Page ${pageIndex + 1} of $totalPages  •  Psychometric Diagnostic Ledger", PAGE_WIDTH / 2f, PAGE_HEIGHT - 25f, Paint().apply {
                    color = AndroidColor.GRAY
                    textSize = 8.5f
                    textAlign = Paint.Align.CENTER
                })

                pdf.finishPage(page)
            }

            context.contentResolver.openOutputStream(uri)?.use { out -> pdf.writeTo(out) }
        } finally {
            pdf.close()
        }
    }

    // 6. ATTENDANCE VS SCANNED RECONCILIATION PDF
    suspend fun generateAttendanceVsScannedPdf(
        context: Context,
        exam: Exam,
        results: List<ScanResult>,
        students: List<Student>,
        attendanceMap: Map<String, Boolean>,
        uri: Uri
    ) = withContext(Dispatchers.IO) {
        val pdf = PdfDocument()
        try {
            val titlePaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 15f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint().apply {
                color = AndroidColor.DKGRAY
                textSize = 10f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val headerPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 9.5f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 9f
                isAntiAlias = true
            }

            val scannedRolls = results.map { it.studentId.trim() }.toSet()
            val sortedStudents = students.sortedBy { it.rollNo }

            val rowsPerPage = 32
            val chunks = sortedStudents.chunked(rowsPerPage)
            val totalPages = chunks.size.coerceAtLeast(1)

            chunks.forEachIndexed { pageIndex, chunk ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawText(exam.title, PAGE_WIDTH / 2f, 40f, titlePaint)
                canvas.drawText("EXAM DAY AUDIT: ATTENDANCE VS SCANNED OMR SHEETS", PAGE_WIDTH / 2f, 58f, titlePaint)
                canvas.drawText("Exam: ${exam.name}  |  Subject: ${exam.subject}", PAGE_WIDTH / 2f, 76f, subPaint)

                var y = 110f
                canvas.drawLine(MARGIN, y - 12f, PAGE_WIDTH - MARGIN, y - 12f, Paint().apply { color = AndroidColor.BLACK; strokeWidth = 1f })
                canvas.drawText("Roll No", MARGIN, y, headerPaint)
                canvas.drawText("Student Name", MARGIN + 70f, y, headerPaint)
                canvas.drawText("Attendance", MARGIN + 240f, y, headerPaint)
                canvas.drawText("OMR Scanned", MARGIN + 330f, y, headerPaint)
                canvas.drawText("Audit Finding", MARGIN + 430f, y, headerPaint)
                y += 6f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = AndroidColor.BLACK; strokeWidth = 1.2f })
                y += 16f

                for (s in chunk) {
                    val isPresent = attendanceMap[s.rollNo] == true
                    val isScanned = scannedRolls.contains(s.rollNo.trim())
                    val findingText: String
                    val findingColor: Int
                    if (isPresent && isScanned) {
                        findingText = "OK (Reconciled)"
                        findingColor = 0xFF16A34A.toInt()
                    } else if (isPresent && !isScanned) {
                        findingText = "MISSING OMR SHEET!"
                        findingColor = 0xFFDC2626.toInt()
                    } else if (!isPresent && isScanned) {
                        findingText = "EXTRA / UNREGISTERED"
                        findingColor = 0xFFEA580C.toInt()
                    } else {
                        findingText = "Absent"
                        findingColor = 0xFF64748B.toInt()
                    }

                    canvas.drawText(s.rollNo, MARGIN, y, textPaint)
                    val truncatedName = if (s.name.length > 20) s.name.substring(0, 18) + ".." else s.name
                    canvas.drawText(truncatedName, MARGIN + 70f, y, textPaint)
                    canvas.drawText(if (isPresent) "Present" else "Absent", MARGIN + 240f, y, textPaint)
                    canvas.drawText(if (isScanned) "Evaluated" else "Not Scanned", MARGIN + 330f, y, textPaint)
                    canvas.drawText(findingText, MARGIN + 430f, y, Paint(textPaint).apply {
                        this.color = findingColor
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    })

                    y += 4f
                    canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = 0xFFF1F5F9.toInt(); strokeWidth = 0.5f })
                    y += 16f
                }

                canvas.drawText("Page ${pageIndex + 1} of $totalPages  •  Discrepancy Audit Log", PAGE_WIDTH / 2f, PAGE_HEIGHT - 25f, Paint().apply {
                    color = AndroidColor.GRAY
                    textSize = 8.5f
                    textAlign = Paint.Align.CENTER
                })

                pdf.finishPage(page)
            }

            context.contentResolver.openOutputStream(uri)?.use { out -> pdf.writeTo(out) }
        } finally {
            pdf.close()
        }
    }
}
