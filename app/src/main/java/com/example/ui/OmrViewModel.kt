package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Exam
import com.example.data.AnswerKey
import com.example.data.Converters
import com.example.data.ScanResult
import com.example.data.Student
import com.example.data.QuestionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class OmrViewModel(application: Application) : AndroidViewModel(application) {
    val attendanceMap = kotlinx.coroutines.flow.MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val converters = Converters()

    private val _students = kotlinx.coroutines.flow.MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students

    val isLoadingExams = kotlinx.coroutines.flow.MutableStateFlow(true)
    val isLoadingStudents = kotlinx.coroutines.flow.MutableStateFlow(true)

    private val _exams = kotlinx.coroutines.flow.MutableStateFlow<List<Exam>>(emptyList())
    val exams: StateFlow<List<Exam>> = _exams

    init {
        fetchStudents()
        fetchExams()
    }

    private fun fetchStudents() {
        viewModelScope.launch {
            isLoadingStudents.value = true
            val list = com.example.util.CloudSyncManager.fetchStudents()
            _students.value = list
            isLoadingStudents.value = false
        }
    }

    private fun fetchExams() {
        viewModelScope.launch {
            isLoadingExams.value = true
            val list = com.example.util.CloudSyncManager.fetchExams()
            _exams.value = list
            isLoadingExams.value = false
        }
    }

    fun addStudent(
        name: String, 
        fatherName: String, 
        motherName: String,
        gender: String,
        dob: String,
        mobileNo: String,
        email: String,
        stream: String,
        subjects: String,
        imagePath: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val currentStudents = students.value
            
            // YY + 4 digit sequential
            val yearStr = (Calendar.getInstance().get(Calendar.YEAR) % 100).toString()
            val nextId = (currentStudents.size + 1)
            val rollNo = yearStr + String.format("%04d", nextId)
            
            val random2Digit = (10..99).random().toString()
            val registrationNo = yearStr + random2Digit + String.format("%04d", nextId)
            
            val student = Student(
                name = name,
                fatherName = fatherName,
                motherName = motherName,
                gender = gender,
                registrationNo = registrationNo,
                rollNo = rollNo,
                dob = dob,
                mobileNo = mobileNo,
                email = email,
                stream = stream,
                subjects = subjects,
                imagePath = imagePath
            )
            
            // Upload to Firebase & Cloudinary
            launch {
                com.example.util.CloudSyncManager.uploadStudent(student)
                fetchStudents() // Refresh list
            }
            
            onDone()
        }
    }

    fun deleteStudent(rollNo: String) {
        viewModelScope.launch {
            com.example.util.CloudSyncManager.deleteStudent(rollNo)
            fetchStudents()
        }
    }

    fun getScanResultsForExam(examId: Int): StateFlow<List<ScanResult>> {
        val resultsFlow = kotlinx.coroutines.flow.MutableStateFlow<List<ScanResult>>(emptyList())
        viewModelScope.launch {
            resultsFlow.value = com.example.util.CloudSyncManager.fetchScanResultsForExam(examId)
        }
        return resultsFlow
    }

    fun getQuestionsForExam(examId: Int): StateFlow<List<QuestionEntity>> {
        val flow = kotlinx.coroutines.flow.MutableStateFlow<List<QuestionEntity>>(emptyList())
        viewModelScope.launch {
            flow.value = com.example.util.CloudSyncManager.fetchQuestions(examId)
        }
        return flow
    }

    fun saveQuestions(questions: List<QuestionEntity>, onDone: () -> Unit) {
        viewModelScope.launch {
            com.example.util.CloudSyncManager.uploadQuestions(questions)
            onDone()
        }
    }

    fun saveQuestion(question: QuestionEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            com.example.util.CloudSyncManager.uploadQuestion(question)
            onDone()
        }
    }

    fun updateQuestion(question: QuestionEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            com.example.util.CloudSyncManager.uploadQuestion(question)
            onDone()
        }
    }

    fun deleteQuestion(question: QuestionEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            com.example.util.CloudSyncManager.deleteQuestion(question.examId, question.id)
            onDone()
        }
    }

    fun deleteQuestionsForExam(examId: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            com.example.util.CloudSyncManager.deleteQuestionsForExam(examId)
            onDone()
        }
    }

    suspend fun getExamById(id: Int): Exam? {
        return _exams.value.find { it.id == id } ?: com.example.util.CloudSyncManager.fetchExams().find { it.id == id }
    }

    suspend fun getAnswerKeyForExamAndSet(examId: Int, setName: String): AnswerKey? {
        return com.example.util.CloudSyncManager.fetchAnswerKey(examId, setName)
    }

    fun createExam(name: String, subject: String, date: String, title: String = "बिहार विद्यालय परीक्षा , समिति", logoUrl: String = "", logoOpacity: Float = 0.2f, logoSize: Float = 100f, logoPosition: String = "Left", marksPerQuestion: Float = 1f, negativeMarks: Float = 0f, passMarks: Float = 30f, bonusMarks: Float = 0f, templateType: String = "Standard", onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val examId = com.example.util.CloudSyncManager.uploadExam(Exam(name = name, subject = subject, date = date, title = title, logoUrl = logoUrl, logoOpacity = logoOpacity, logoSize = logoSize, logoPosition = logoPosition, marksPerQuestion = marksPerQuestion, negativeMarks = negativeMarks, passMarks = passMarks, bonusMarks = bonusMarks, templateType = templateType))
            fetchExams()
            onDone(examId)
        }
    }

    fun updateExam(exam: Exam, onDone: () -> Unit) {
        viewModelScope.launch {
            com.example.util.CloudSyncManager.uploadExam(exam)
            fetchExams()
            onDone()
        }
    }

    fun deleteExam(id: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            com.example.util.CloudSyncManager.deleteExam(id)
            fetchExams()
            onDone()
        }
    }

    fun saveAnswerKey(examId: Int, setName: String, numQuestions: Int, numOptions: Int, correctAnswers: List<Int>, onDone: () -> Unit) {
        viewModelScope.launch {
            val existing = getAnswerKeyForExamAndSet(examId, setName)
            val key = AnswerKey(
                id = existing?.id ?: 0,
                examId = examId,
                setName = setName,
                numQuestions = numQuestions,
                numOptions = numOptions,
                correctAnswers = converters.fromList(correctAnswers)
            )
            com.example.util.CloudSyncManager.uploadAnswerKey(key)
            onDone()
        }
    }

    fun saveScanResult(examId: Int, studentId: String, paperSet: String, score: Float, totalQuestions: Int, studentAnswers: List<Int>, questionStatuses: List<Int>, onDone: () -> Unit) {
        viewModelScope.launch {
            val result = ScanResult(
                examId = examId,
                studentId = studentId,
                paperSet = paperSet,
                score = score,
                totalQuestions = totalQuestions,
                studentAnswers = converters.fromList(studentAnswers),
                questionStatuses = converters.fromList(questionStatuses)
            )
            
            // Upload to Firebase
            launch {
                com.example.util.CloudSyncManager.uploadScanResult(result)
            }
            
            onDone()
        }
    }
}
