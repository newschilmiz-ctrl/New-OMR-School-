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
                writer.write("Roll No,Registration No,Name,Gender,Paper Set,Score,Max Questions,Attempted\n")
                
                results.forEach { result ->
                    val student = students.find { it.rollNo == result.studentId }
                    val name = student?.name ?: "Unknown"
                    val regNo = student?.registrationNo ?: "Unknown"
                    val gender = student?.gender ?: "Unknown"
                    
                    val attempted = result.questionStatuses.count { it != ',' && it != '[' && it != ']' && it != '-' } // naive check for attempts
                    
                    writer.write("${result.studentId},$regNo,$name,$gender,${result.paperSet},${result.score},${result.totalQuestions},$attempted\n")
                }
            }
        }
    }
}
