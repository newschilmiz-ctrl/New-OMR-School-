package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.Exam
import com.example.data.ScanResult
import com.example.data.Student
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CsvExporter {
    suspend fun exportResults(
        context: Context,
        uri: Uri,
        exam: Exam,
        results: List<ScanResult>,
        students: List<Student>
    ) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                // Header
                writer.write("Rank,Roll No,Registration No,Candidate Name,Father Name,Gender,Subject,Paper Set,Score,Max Marks,Percentage,Status\n")
                
                val sorted = results.sortedByDescending { it.score }
                val totalQ = results.firstOrNull()?.totalQuestions?.takeIf { it > 0 } ?: 100
                val totalMarks = if (exam.marksPerQuestion > 0) totalQ * exam.marksPerQuestion else 100f

                sorted.forEachIndexed { index, result ->
                    val rank = index + 1
                    val student = students.find { it.rollNo.equals(result.studentId, ignoreCase = true) }
                    val name = student?.name ?: "Unknown"
                    val regNo = student?.registrationNo ?: ""
                    val fatherName = student?.fatherName ?: ""
                    val gender = student?.gender ?: ""
                    val pct = if (totalMarks > 0) (result.score / totalMarks) * 100f else 0f
                    val status = if (result.score >= exam.passMarks) "PASS" else "FAIL"
                    
                    writer.write("$rank,${result.studentId},\"$regNo\",\"$name\",\"$fatherName\",\"$gender\",\"${exam.subject}\",\"${result.paperSet}\",${result.score},$totalMarks,${String.format("%.1f", pct)}%,$status\n")
                }
            }
        }
    }
}
