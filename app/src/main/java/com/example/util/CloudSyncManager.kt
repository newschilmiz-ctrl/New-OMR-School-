package com.example.util

import android.util.Log
import com.example.data.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files

object CloudSyncManager {
    private const val FIREBASE_DB_URL = "https://new-ba-f9d96-default-rtdb.firebaseio.com"
    private const val CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/dckmi2k1j/image/upload"
    private const val CLOUDINARY_PRESET = "rs arts"

    suspend fun uploadStudent(student: Student) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Upload Image to Cloudinary
                var uploadedImageUrl = ""
                if (student.imagePath.isNotEmpty()) {
                    val file = File(student.imagePath)
                    if (file.exists()) {
                        uploadedImageUrl = uploadToCloudinary(file)
                    }
                }

                // 2. Upload Student to Firebase Realtime DB
                val json = JSONObject().apply {
                    put("name", student.name)
                    put("fatherName", student.fatherName)
                    put("motherName", student.motherName)
                    put("gender", student.gender)
                    put("registrationNo", student.registrationNo)
                    put("rollNo", student.rollNo)
                    put("dob", student.dob)
                    put("mobileNo", student.mobileNo)
                    put("email", student.email)
                    put("stream", student.stream)
                    put("subjects", student.subjects)
                    put("imageUrl", uploadedImageUrl)
                }

                val url = URL("$FIREBASE_DB_URL/students/${student.rollNo}.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                val responseCode = conn.responseCode
                Log.d("CloudSync", "Firebase response code: $responseCode")
                conn.disconnect()

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("CloudSync", "Sync failed", e)
            }
        }
    }

    suspend fun uploadScanResult(result: com.example.data.ScanResult) {
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("examId", result.examId)
                    put("studentId", result.studentId)
                    put("paperSet", result.paperSet)
                    put("score", result.score)
                    put("totalQuestions", result.totalQuestions)
                    put("timestamp", result.timestamp)
                }

                // Push to results node (unique ID using timestamp)
                val url = URL("$FIREBASE_DB_URL/scan_results/${result.examId}/${result.studentId}.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                val responseCode = conn.responseCode
                Log.d("CloudSync", "Scan Result Firebase response: $responseCode")
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun uploadExam(exam: com.example.data.Exam): Int {
        return withContext(Dispatchers.IO) {
            try {
                // If ID is 0, generate a new one using timestamp or random
                val idToUse = if (exam.id == 0) (System.currentTimeMillis() % 1000000).toInt() else exam.id
                var logoUrlToUse = exam.logoUrl
                
                // If logoUrl is a local file path (not starting with http), upload it
                if (logoUrlToUse.isNotEmpty() && !logoUrlToUse.startsWith("http")) {
                    val file = File(logoUrlToUse)
                    if (file.exists()) {
                        logoUrlToUse = uploadToCloudinary(file)
                    }
                }
                
                val json = JSONObject().apply {
                    put("id", idToUse)
                    put("name", exam.name)
                    put("subject", exam.subject)
                    put("date", exam.date)
                    put("title", exam.title)
                    put("logoUrl", logoUrlToUse)
                    put("logoOpacity", exam.logoOpacity.toDouble())
                    put("logoSize", exam.logoSize.toDouble())
                    put("logoPosition", exam.logoPosition)
                    put("marksPerQuestion", exam.marksPerQuestion.toDouble())
                    put("negativeMarks", exam.negativeMarks.toDouble())
                    put("passMarks", exam.passMarks.toDouble())
                    put("bonusMarks", exam.bonusMarks.toDouble())
                    put("timestamp", exam.timestamp)
                }

                val url = URL("$FIREBASE_DB_URL/exams/$idToUse.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                val responseCode = conn.responseCode
                Log.d("CloudSync", "Exam upload response: $responseCode")
                conn.disconnect()
                idToUse
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }

    suspend fun fetchExams(): List<com.example.data.Exam> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<com.example.data.Exam>()
            try {
                val url = URL("$FIREBASE_DB_URL/exams.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().readText()
                    if (response != "null") {
                        val parsed = org.json.JSONTokener(response).nextValue()
                        if (parsed is JSONObject) {
                            val keys = parsed.keys()
                            while (keys.hasNext()) {
                                val examJson = parsed.getJSONObject(keys.next())
                                list.add(
                                    com.example.data.Exam(
                                        id = examJson.optInt("id", 0),
                                        name = examJson.optString("name", ""),
                                        subject = examJson.optString("subject", ""),
                                        date = examJson.optString("date", ""),
                                        title = examJson.optString("title", "बिहार विद्यालय परीक्षा , समिति"),
                                        logoUrl = examJson.optString("logoUrl", ""),
                                        logoOpacity = examJson.optDouble("logoOpacity", 0.2).toFloat(),
                                        logoSize = examJson.optDouble("logoSize", 100.0).toFloat(),
                                        logoPosition = examJson.optString("logoPosition", "Left"),
                                        marksPerQuestion = examJson.optDouble("marksPerQuestion", 1.0).toFloat(),
                                        negativeMarks = examJson.optDouble("negativeMarks", 0.0).toFloat(),
                                        passMarks = examJson.optDouble("passMarks", 30.0).toFloat(),
                                        bonusMarks = examJson.optDouble("bonusMarks", 0.0).toFloat(),
                                        timestamp = examJson.optLong("timestamp", 0)
                                    )
                                )
                            }
                        } else if (parsed is org.json.JSONArray) {
                            for (i in 0 until parsed.length()) {
                                if (parsed.isNull(i)) continue
                                val examJson = parsed.getJSONObject(i)
                                list.add(
                                    com.example.data.Exam(
                                        id = examJson.optInt("id", 0),
                                        name = examJson.optString("name", ""),
                                        subject = examJson.optString("subject", ""),
                                        date = examJson.optString("date", ""),
                                        title = examJson.optString("title", "बिहार विद्यालय परीक्षा , समिति"),
                                        logoUrl = examJson.optString("logoUrl", ""),
                                        logoOpacity = examJson.optDouble("logoOpacity", 0.2).toFloat(),
                                        logoSize = examJson.optDouble("logoSize", 100.0).toFloat(),
                                        logoPosition = examJson.optString("logoPosition", "Left"),
                                        marksPerQuestion = examJson.optDouble("marksPerQuestion", 1.0).toFloat(),
                                        negativeMarks = examJson.optDouble("negativeMarks", 0.0).toFloat(),
                                        passMarks = examJson.optDouble("passMarks", 30.0).toFloat(),
                                        bonusMarks = examJson.optDouble("bonusMarks", 0.0).toFloat(),
                                        timestamp = examJson.optLong("timestamp", 0)
                                    )
                                )
                            }
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list.sortedByDescending { it.timestamp }
        }
    }

    suspend fun deleteExam(id: Int) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$FIREBASE_DB_URL/exams/$id.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    suspend fun uploadAnswerKey(key: com.example.data.AnswerKey) {
        withContext(Dispatchers.IO) {
            try {
                val idToUse = if (key.id == 0) (System.currentTimeMillis() % 1000000).toInt() else key.id
                val json = JSONObject().apply {
                    put("id", idToUse)
                    put("examId", key.examId)
                    put("setName", key.setName)
                    put("numQuestions", key.numQuestions)
                    put("numOptions", key.numOptions)
                    put("correctAnswers", key.correctAnswers)
                    put("timestamp", key.timestamp)
                }

                val url = URL("$FIREBASE_DB_URL/answer_keys/${key.examId}/${key.setName}.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    suspend fun fetchAnswerKey(examId: Int, setName: String): com.example.data.AnswerKey? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$FIREBASE_DB_URL/answer_keys/$examId/$setName.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().readText()
                    if (response != "null") {
                        val jsonObject = JSONObject(response)
                        return@withContext com.example.data.AnswerKey(
                            id = jsonObject.optInt("id", 0),
                            examId = jsonObject.optInt("examId", examId),
                            setName = jsonObject.optString("setName", setName),
                            numQuestions = jsonObject.optInt("numQuestions", 0),
                            numOptions = jsonObject.optInt("numOptions", 0),
                            correctAnswers = jsonObject.optString("correctAnswers", "[]"),
                            timestamp = jsonObject.optLong("timestamp", 0)
                        )
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            null
        }
    }

    suspend fun fetchStudents(): List<Student> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<Student>()
            try {
                val url = URL("$FIREBASE_DB_URL/students.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().readText()
                    if (response != "null") {
                        val parsed = org.json.JSONTokener(response).nextValue()
                        var idCounter = 1
                        if (parsed is JSONObject) {
                            val keys = parsed.keys()
                            while (keys.hasNext()) {
                                val studentJson = parsed.getJSONObject(keys.next())
                                list.add(
                                    Student(
                                        id = idCounter++,
                                        name = studentJson.optString("name", ""),
                                        fatherName = studentJson.optString("fatherName", ""),
                                        motherName = studentJson.optString("motherName", ""),
                                        gender = studentJson.optString("gender", "Male"),
                                        registrationNo = studentJson.optString("registrationNo", ""),
                                        rollNo = studentJson.optString("rollNo", ""),
                                        dob = studentJson.optString("dob", ""),
                                        mobileNo = studentJson.optString("mobileNo", ""),
                                        email = studentJson.optString("email", ""),
                                        stream = studentJson.optString("stream", "ARTS"),
                                        subjects = studentJson.optString("subjects", ""),
                                        imagePath = studentJson.optString("imageUrl", "")
                                    )
                                )
                            }
                        } else if (parsed is org.json.JSONArray) {
                            for (i in 0 until parsed.length()) {
                                if (parsed.isNull(i)) continue
                                val studentJson = parsed.getJSONObject(i)
                                list.add(
                                    Student(
                                        id = idCounter++,
                                        name = studentJson.optString("name", ""),
                                        fatherName = studentJson.optString("fatherName", ""),
                                        motherName = studentJson.optString("motherName", ""),
                                        gender = studentJson.optString("gender", "Male"),
                                        registrationNo = studentJson.optString("registrationNo", ""),
                                        rollNo = studentJson.optString("rollNo", ""),
                                        dob = studentJson.optString("dob", ""),
                                        mobileNo = studentJson.optString("mobileNo", ""),
                                        email = studentJson.optString("email", ""),
                                        stream = studentJson.optString("stream", "ARTS"),
                                        subjects = studentJson.optString("subjects", ""),
                                        imagePath = studentJson.optString("imageUrl", "")
                                    )
                                )
                            }
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list.sortedBy { it.name }
        }
    }

    suspend fun fetchScanResultsForExam(examId: Int): List<com.example.data.ScanResult> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<com.example.data.ScanResult>()
            try {
                val url = URL("$FIREBASE_DB_URL/scan_results/$examId.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().readText()
                    if (response != "null") {
                        val parsed = org.json.JSONTokener(response).nextValue()
                        var idCounter = 1
                        if (parsed is JSONObject) {
                            val keys = parsed.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val resultJson = parsed.getJSONObject(key)
                                list.add(
                                    com.example.data.ScanResult(
                                        id = idCounter++,
                                        examId = resultJson.optInt("examId", examId),
                                        studentId = resultJson.optString("studentId", key),
                                        paperSet = resultJson.optString("paperSet", ""),
                                        score = resultJson.optDouble("score", 0.0).toFloat(),
                                        totalQuestions = resultJson.optInt("totalQuestions", 0),
                                        studentAnswers = "[]",
                                        timestamp = resultJson.optLong("timestamp", System.currentTimeMillis())
                                    )
                                )
                            }
                        } else if (parsed is org.json.JSONArray) {
                            for (i in 0 until parsed.length()) {
                                if (parsed.isNull(i)) continue
                                val resultJson = parsed.getJSONObject(i)
                                list.add(
                                    com.example.data.ScanResult(
                                        id = idCounter++,
                                        examId = resultJson.optInt("examId", examId),
                                        studentId = resultJson.optString("studentId", i.toString()),
                                        paperSet = resultJson.optString("paperSet", ""),
                                        score = resultJson.optDouble("score", 0.0).toFloat(),
                                        totalQuestions = resultJson.optInt("totalQuestions", 0),
                                        studentAnswers = "[]",
                                        timestamp = resultJson.optLong("timestamp", System.currentTimeMillis())
                                    )
                                )
                            }
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list.sortedByDescending { it.timestamp }
        }
    }

    suspend fun deleteStudent(rollNo: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$FIREBASE_DB_URL/students/$rollNo.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun uploadQuestions(questions: List<com.example.data.QuestionEntity>) {
        withContext(Dispatchers.IO) {
            questions.forEach { q -> uploadQuestion(q) }
        }
    }

    suspend fun uploadQuestion(q: com.example.data.QuestionEntity) {
        withContext(Dispatchers.IO) {
            try {
                val idToUse = if (q.id == 0) (System.currentTimeMillis() % 1000000).toInt() + (Math.random() * 1000).toInt() else q.id
                val json = JSONObject().apply {
                    put("id", idToUse)
                    put("examId", q.examId)
                    put("text", q.text)
                    put("optionA", q.optionA)
                    put("optionB", q.optionB)
                    put("optionC", q.optionC)
                    put("optionD", q.optionD)
                    put("correctIndex", q.correctIndex)
                }

                val url = URL("$FIREBASE_DB_URL/questions/${q.examId}/$idToUse.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchQuestions(examId: Int): List<com.example.data.QuestionEntity> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<com.example.data.QuestionEntity>()
            try {
                val url = URL("$FIREBASE_DB_URL/questions/$examId.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().readText()
                    if (response != "null") {
                        val parsed = org.json.JSONTokener(response).nextValue()
                        if (parsed is JSONObject) {
                            val keys = parsed.keys()
                            while (keys.hasNext()) {
                                val qJson = parsed.getJSONObject(keys.next())
                                list.add(
                                    com.example.data.QuestionEntity(
                                        id = qJson.optInt("id", 0),
                                        examId = qJson.optInt("examId", examId),
                                        text = qJson.optString("text", ""),
                                        optionA = qJson.optString("optionA", ""),
                                        optionB = qJson.optString("optionB", ""),
                                        optionC = qJson.optString("optionC", ""),
                                        optionD = qJson.optString("optionD", ""),
                                        correctIndex = qJson.optInt("correctIndex", 0)
                                    )
                                )
                            }
                        } else if (parsed is org.json.JSONArray) {
                            for (i in 0 until parsed.length()) {
                                if (parsed.isNull(i)) continue
                                val qJson = parsed.getJSONObject(i)
                                list.add(
                                    com.example.data.QuestionEntity(
                                        id = qJson.optInt("id", 0),
                                        examId = qJson.optInt("examId", examId),
                                        text = qJson.optString("text", ""),
                                        optionA = qJson.optString("optionA", ""),
                                        optionB = qJson.optString("optionB", ""),
                                        optionC = qJson.optString("optionC", ""),
                                        optionD = qJson.optString("optionD", ""),
                                        correctIndex = qJson.optInt("correctIndex", 0)
                                    )
                                )
                            }
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list
        }
    }

    suspend fun deleteQuestion(examId: Int, id: Int) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$FIREBASE_DB_URL/questions/$examId/$id.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    suspend fun deleteQuestionsForExam(examId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$FIREBASE_DB_URL/questions/$examId.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun uploadToCloudinary(file: File): String {
        val boundary = "---Boundary" + System.currentTimeMillis()
        val url = URL(CLOUDINARY_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        conn.doOutput = true

        conn.outputStream.use { outputStream ->
            // Upload preset
            outputStream.write(("--$boundary\r\n").toByteArray())
            outputStream.write(("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n").toByteArray())
            outputStream.write(("$CLOUDINARY_PRESET\r\n").toByteArray())

            // File
            outputStream.write(("--$boundary\r\n").toByteArray())
            outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n").toByteArray())
            outputStream.write(("Content-Type: image/jpeg\r\n\r\n").toByteArray())

            file.inputStream().use { it.copyTo(outputStream) }
            outputStream.write(("\r\n--$boundary--\r\n").toByteArray())
        }

        val responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            return json.getString("secure_url")
        }
        return ""
    }
}
