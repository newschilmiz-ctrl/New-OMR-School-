package com.example.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.Exam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun generateAnswerKeyPdf(context: Context, exam: Exam, uri: Uri, viewModel: OmrViewModel) = withContext(Dispatchers.IO) {
    val sets = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
    val answerKeys = mutableMapOf<String, List<Int>>()
    var maxQuestions = 0

    // Fetch answer keys for all sets
    for (setName in sets) {
        val ak = viewModel.getAnswerKeyForExamAndSet(exam.id, setName)
        if (ak != null) {
            val correctList = try {
                val jsonArr = org.json.JSONArray(ak.correctAnswers)
                List(jsonArr.length()) { i -> jsonArr.getInt(i) }
            } catch (e: Exception) {
                emptyList()
            }
            answerKeys[setName] = correctList
            if (correctList.size > maxQuestions) {
                maxQuestions = correctList.size
            }
        }
    }

    if (maxQuestions == 0) return@withContext

    val pdfDocument = PdfDocument()
    val pageW = 793f
    val pageH = 1122f

    val titlePaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 24f
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.BLACK
    }
    
    val headerPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12f
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.BLACK
    }

    val cellPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12f
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.BLACK
    }

    val borderPaint = Paint().apply {
        strokeWidth = 1f
        style = Paint.Style.STROKE
        color = android.graphics.Color.BLACK
    }

    val optionsMap = mapOf(0 to "A", 1 to "B", 2 to "C", 3 to "D")

    val rowsPerPage = 50
    val totalPages = Math.ceil(maxQuestions.toDouble() / rowsPerPage).toInt().coerceAtLeast(1)
    
    val margin = 30f
    val tableTop = 90f
    val rowHeight = 19f
    // Col 1 is Q.No, then 10 columns for A-J
    val colWidth = (pageW - 2 * margin) / 11f

    for (p in 0 until totalPages) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), p + 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawText("Answer Key - ${exam.title ?: exam.name}", pageW / 2, 50f, titlePaint)
        canvas.drawText("Subject: ${exam.subject}", pageW / 2, 80f, headerPaint)

        // Draw Table Header
        var currentY = tableTop
        for (i in 0..10) {
            val left = margin + i * colWidth
            canvas.drawRect(left, currentY, left + colWidth, currentY + rowHeight, borderPaint)
            val text = if (i == 0) "Q.No" else sets[i - 1]
            canvas.drawText(text, left + colWidth / 2, currentY + 13.5f, headerPaint)
        }
        currentY += rowHeight

        // Draw Rows
        val startQ = p * rowsPerPage
        val endQ = Math.min(startQ + rowsPerPage, maxQuestions)
        
        for (q in startQ until endQ) {
            for (i in 0..10) {
                val left = margin + i * colWidth
                canvas.drawRect(left, currentY, left + colWidth, currentY + rowHeight, borderPaint)
                
                if (i == 0) {
                    canvas.drawText("${q + 1}", left + colWidth / 2, currentY + 13.5f, cellPaint)
                } else {
                    val setName = sets[i - 1]
                    val list = answerKeys[setName]
                    if (list != null && q < list.size) {
                        val ansIdx = list[q]
                        val ansChar = optionsMap[ansIdx] ?: "?"
                        canvas.drawText(ansChar, left + colWidth / 2, currentY + 13.5f, cellPaint)
                    }
                }
            }
            currentY += rowHeight
        }

        pdfDocument.finishPage(page)
    }

    try {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            pdfDocument.writeTo(out)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    pdfDocument.close()
}
