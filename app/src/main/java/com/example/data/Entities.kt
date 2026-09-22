package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class Exam(
    val id: Int = 0,
    val name: String,
    val subject: String,
    val date: String = "",
    val title: String = "बिहार विद्यालय परीक्षा , समिति",
    val logoUrl: String = "",
    val logoOpacity: Float = 0.2f,
    val logoSize: Float = 100f,
    val logoPosition: String = "Left",
    val marksPerQuestion: Float = 1f,
    val negativeMarks: Float = 0f,
    val passMarks: Float = 30f,
    val bonusMarks: Float = 0f,
    val templateType: String = "Standard",
    val timestamp: Long = System.currentTimeMillis()
)

data class AnswerKey(
    val id: Int = 0,
    val examId: Int,
    val setName: String,
    val numQuestions: Int,
    val numOptions: Int,
    val correctAnswers: String, // JSON list of Int (0-based index of correct option)
    val timestamp: Long = System.currentTimeMillis()
)

data class Student(
    val id: Int = 0,
    val name: String,
    val fatherName: String,
    val motherName: String = "",
    val gender: String = "Male",
    val registrationNo: String = "",
    val rollNo: String,
    val dob: String = "",
    val mobileNo: String = "",
    val email: String = "",
    val stream: String = "ARTS",
    val subjects: String = "", // Comma-separated list of selected subjects
    val imagePath: String = "",
    val sessionId: String = "",
    val sessionName: String = "",
    val className: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class QuestionEntity(
    val id: Int = 0,
    val examId: Int,
    val text: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctIndex: Int
)

data class ScanResult(
    val id: Int = 0,
    val examId: Int,
    val studentId: String,
    val paperSet: String = "",
    val score: Float,
    val totalQuestions: Int,
    val studentAnswers: String, // JSON list of Int (0-based index of selected option, -1 if none/invalid)
    val questionStatuses: String = "", // JSON list of Int (1 = correct, 0 = wrong, -1 = empty)
    val timestamp: Long = System.currentTimeMillis()
)

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, Integer::class.java)
    private val adapter = moshi.adapter<List<Int>>(listType)

    @TypeConverter
    fun fromList(list: List<Int>): String {
        return adapter.toJson(list)
    }

    @TypeConverter
    fun toList(json: String): List<Int> {
        return adapter.fromJson(json) ?: emptyList()
    }
}

data class CoachingClass(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val section: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class CoachingSubject(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val classId: String = "",
    val className: String = "",
    val stream: String = "SCIENCE", // "ARTS", "SCIENCE", "COMMERCE", "GENERAL"
    val subjectCode: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class CoachingSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val classId: String = "",
    val className: String = "",
    val startTime: String = "07:00 AM",
    val endTime: String = "08:30 AM",
    val days: String = "Mon - Sat",
    val academicYear: String = "2024-2025",
    val timestamp: Long = System.currentTimeMillis()
)

data class FeeRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val studentRollNo: String,
    val studentName: String,
    val amountPaid: Double,
    val totalFee: Double,
    val paymentDate: String,
    val paymentMode: String = "Cash", // Cash, UPI, Cheque, Online
    val receiptNo: String = "",
    val monthOrInstallment: String = "Monthly Installment",
    val remarks: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class AttendanceRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val studentRollNo: String,
    val studentName: String,
    val date: String, // YYYY-MM-DD
    val status: String = "PRESENT", // PRESENT, ABSENT, LATE, EXCUSED
    val sessionId: String = "",
    val sessionName: String = "",
    val inTime: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class NoticeRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: String = "GENERAL", // GENERAL, EXAM_ALERT, HOLIDAY, FEE_ALERT, URGENT
    val targetBatch: String = "All Batches",
    val postedBy: String = "Director Office",
    val date: String,
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class TimetablePeriod(
    val id: String = java.util.UUID.randomUUID().toString(),
    val batchId: String = "",
    val batchName: String,
    val dayOfWeek: String, // MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    val startTime: String, // e.g. "07:00 AM"
    val endTime: String,   // e.g. "08:15 AM"
    val subject: String,
    val teacherName: String,
    val roomNumber: String = "Hall A",
    val timestamp: Long = System.currentTimeMillis()
)

data class FacultyMember(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val subject: String,
    val qualification: String = "M.Sc, B.Ed",
    val phone: String = "",
    val email: String = "",
    val salaryType: String = "Monthly", // Monthly, Per Lecture, Contract
    val assignedBatches: String = "All Batches",
    val status: String = "Active", // Active, On Leave
    val timestamp: Long = System.currentTimeMillis()
)

data class StudyMaterial(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val type: String = "DPP", // DPP, NOTES, FORMULA_BOOK, ASSIGNMENT, SOLUTION
    val subject: String,
    val batchName: String = "All Batches",
    val chapter: String = "",
    val fileUrlOrInfo: String = "",
    val dueDate: String = "",
    val totalProblems: Int = 15,
    val timestamp: Long = System.currentTimeMillis()
)

