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
    val subjects: String = "", // Used as single subject now, or comma separated
    val imagePath: String = "",
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

