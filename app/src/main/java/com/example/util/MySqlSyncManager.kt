package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object MySqlSyncManager {
    private const val TAG = "MySqlSyncManager"
    private const val TIMEOUT_MS = 15000

    fun normalizeUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (url.isEmpty()) return ""

        // Fix accidental dot before folder name (e.g. rsartsclassess.whf.bz.omr_api -> rsartsclassess.whf.bz/omr_api)
        if (url.contains(".omr_api")) {
            url = url.replace(".omr_api", "/omr_api")
        }

        // Prepend protocol if missing. Default to http:// for free hosting/custom servers
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "http://$url"
        }

        // Ensure endpoint points to a PHP script (default to api.php)
        if (!url.endsWith(".php", ignoreCase = true)) {
            if (!url.endsWith("/")) {
                url = "$url/"
            }
            url = "${url}api.php"
        }

        return url
    }

    private fun executeHttpRequest(targetUrl: String, apiKey: String, payload: JSONObject): Pair<Int, String> {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(targetUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; OMR Grader)")
            if (apiKey.isNotBlank()) {
                conn.setRequestProperty("X-API-KEY", apiKey)
            }
            conn.doOutput = true
            conn.doInput = true

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val inputStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = inputStream?.let { stream ->
                BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
            } ?: ""

            return Pair(responseCode, responseText)
        } finally {
            conn?.disconnect()
        }
    }

    private fun postJson(serverUrl: String, apiKey: String, payload: JSONObject): Pair<Int, String> {
        val normalized = normalizeUrl(serverUrl)
        if (normalized.isBlank()) {
            return Pair(-1, "Server URL is empty")
        }

        try {
            return executeHttpRequest(normalized, apiKey, payload)
        } catch (e: java.net.UnknownHostException) {
            val host = e.message ?: ""
            Log.e(TAG, "UnknownHostException: $host", e)
            val hint = if (host.contains("omr_api") || host.contains("api")) {
                "Host '$host' not found! Make sure you use a slash '/' instead of a dot '.' before omr_api (e.g. rsartsclassess.whf.bz/omr_api/api.php)."
            } else {
                "Unable to resolve domain '$host'. Please verify your domain name or internet connection."
            }
            return Pair(-1, hint)
        } catch (e: javax.net.ssl.SSLException) {
            // Free subdomains like *.whf.bz often don't have valid SSL; attempt HTTP fallback
            if (normalized.startsWith("https://", ignoreCase = true)) {
                val httpUrl = "http://" + normalized.substring("https://".length)
                Log.w(TAG, "SSL failed ($e), attempting fallback to: $httpUrl")
                try {
                    return executeHttpRequest(httpUrl, apiKey, payload)
                } catch (fallbackEx: Exception) {
                    Log.e(TAG, "HTTP fallback failed: ${fallbackEx.message}", fallbackEx)
                    return Pair(-1, "Connection error: ${fallbackEx.message}")
                }
            }
            Log.e(TAG, "SSL Error: ${e.message}", e)
            return Pair(-1, "SSL Error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "HTTP POST failed: ${e.message}", e)
            return Pair(-1, e.message ?: "Connection failed")
        }
    }

    suspend fun testConnection(serverUrl: String, apiKey: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("action", "test_connection")
                    put("timestamp", System.currentTimeMillis())
                }
                val (code, response) = postJson(serverUrl, apiKey, payload)
                if (code in 200..299) {
                    try {
                        val json = JSONObject(response)
                        val status = json.optString("status", "")
                        val message = json.optString("message", "Connected successfully")
                        if (status == "success" || status == "ok") {
                            Pair(true, message)
                        } else {
                            Pair(false, message.ifEmpty { response })
                        }
                    } catch (e: Exception) {
                        Pair(true, "Server responded (HTTP $code)")
                    }
                } else {
                    Pair(false, "Server Error ($code): $response")
                }
            } catch (e: Exception) {
                Pair(false, "Error: ${e.message}")
            }
        }
    }

    suspend fun uploadStudentPhoto(context: Context, rollNo: String, imageBytes: ByteArray): String? {
        if (!SyncPreferences.isMySqlSyncEnabled(context)) return null
        val serverUrl = SyncPreferences.getMySqlServerUrl(context)
        val apiKey = SyncPreferences.getMySqlApiKey(context)
        if (serverUrl.isBlank() || imageBytes.isEmpty()) return null

        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("action", "upload_student_photo")
                    put("roll_no", rollNo)
                    put("image_base64", PhotoCompressor.toBase64(imageBytes))
                }
                val (code, response) = postJson(serverUrl, apiKey, payload)
                if (code in 200..299) {
                    val json = JSONObject(response)
                    json.optString("image_url", null)
                } else {
                    Log.e(TAG, "uploadStudentPhoto failed ($code): $response")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "uploadStudentPhoto error: ${e.message}", e)
                null
            }
        }
    }

    suspend fun syncStudent(context: Context, student: Student): Boolean {
        if (!SyncPreferences.isMySqlSyncEnabled(context)) return false
        val serverUrl = SyncPreferences.getMySqlServerUrl(context)
        val apiKey = SyncPreferences.getMySqlApiKey(context)
        if (serverUrl.isBlank()) return false

        return withContext(Dispatchers.IO) {
            try {
                // If local photo exists, auto-compress to 20KB and encode base64
                var photoBase64 = ""
                if (student.imagePath.isNotBlank() && !student.imagePath.startsWith("http")) {
                    val compressedBytes = PhotoCompressor.compressFileTo20Kb(student.imagePath)
                    if (compressedBytes.isNotEmpty()) {
                        photoBase64 = PhotoCompressor.toBase64(compressedBytes)
                    }
                }

                val dataObj = JSONObject().apply {
                    put("roll_no", student.rollNo)
                    put("name", student.name)
                    put("father_name", student.fatherName)
                    put("mother_name", student.motherName)
                    put("gender", student.gender)
                    put("registration_no", student.registrationNo)
                    put("dob", student.dob)
                    put("mobile_no", student.mobileNo)
                    put("email", student.email)
                    put("stream", student.stream)
                    put("subjects", student.subjects)
                    put("image_url", student.imagePath)
                    if (photoBase64.isNotEmpty()) {
                        put("image_base64", photoBase64)
                    }
                    put("timestamp", student.timestamp)
                }
                val payload = JSONObject().apply {
                    put("action", "save_student")
                    put("data", dataObj)
                }
                val (code, _) = postJson(serverUrl, apiKey, payload)
                code in 200..299
            } catch (e: Exception) {
                Log.e(TAG, "syncStudent failed: ${e.message}", e)
                false
            }
        }
    }

    suspend fun deleteStudent(context: Context, rollNo: String): Boolean {
        if (!SyncPreferences.isMySqlSyncEnabled(context)) return false
        val serverUrl = SyncPreferences.getMySqlServerUrl(context)
        val apiKey = SyncPreferences.getMySqlApiKey(context)
        if (serverUrl.isBlank()) return false

        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("action", "delete_student")
                    put("roll_no", rollNo)
                }
                val (code, _) = postJson(serverUrl, apiKey, payload)
                code in 200..299
            } catch (e: Exception) {
                Log.e(TAG, "deleteStudent failed: ${e.message}", e)
                false
            }
        }
    }

    suspend fun syncExam(context: Context, exam: Exam): Boolean {
        if (!SyncPreferences.isMySqlSyncEnabled(context)) return false
        val serverUrl = SyncPreferences.getMySqlServerUrl(context)
        val apiKey = SyncPreferences.getMySqlApiKey(context)
        if (serverUrl.isBlank()) return false

        return withContext(Dispatchers.IO) {
            try {
                val dataObj = JSONObject().apply {
                    put("id", exam.id)
                    put("name", exam.name)
                    put("subject", exam.subject)
                    put("date", exam.date)
                    put("title", exam.title)
                    put("logo_url", exam.logoUrl)
                    put("logo_opacity", exam.logoOpacity.toDouble())
                    put("logo_size", exam.logoSize.toDouble())
                    put("logo_position", exam.logoPosition)
                    put("marks_per_question", exam.marksPerQuestion.toDouble())
                    put("negative_marks", exam.negativeMarks.toDouble())
                    put("pass_marks", exam.passMarks.toDouble())
                    put("bonus_marks", exam.bonusMarks.toDouble())
                    put("template_type", exam.templateType)
                    put("timestamp", exam.timestamp)
                }
                val payload = JSONObject().apply {
                    put("action", "save_exam")
                    put("data", dataObj)
                }
                val (code, _) = postJson(serverUrl, apiKey, payload)
                code in 200..299
            } catch (e: Exception) {
                Log.e(TAG, "syncExam failed: ${e.message}", e)
                false
            }
        }
    }

    suspend fun deleteExam(context: Context, examId: Int): Boolean {
        if (!SyncPreferences.isMySqlSyncEnabled(context)) return false
        val serverUrl = SyncPreferences.getMySqlServerUrl(context)
        val apiKey = SyncPreferences.getMySqlApiKey(context)
        if (serverUrl.isBlank()) return false

        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("action", "delete_exam")
                    put("id", examId)
                }
                val (code, _) = postJson(serverUrl, apiKey, payload)
                code in 200..299
            } catch (e: Exception) {
                Log.e(TAG, "deleteExam failed: ${e.message}", e)
                false
            }
        }
    }

    suspend fun syncAnswerKey(context: Context, key: AnswerKey): Boolean {
        if (!SyncPreferences.isMySqlSyncEnabled(context)) return false
        val serverUrl = SyncPreferences.getMySqlServerUrl(context)
        val apiKey = SyncPreferences.getMySqlApiKey(context)
        if (serverUrl.isBlank()) return false

        return withContext(Dispatchers.IO) {
            try {
                val dataObj = JSONObject().apply {
                    put("id", key.id)
                    put("exam_id", key.examId)
                    put("set_name", key.setName)
                    put("num_questions", key.numQuestions)
                    put("num_options", key.numOptions)
                    put("correct_answers", key.correctAnswers)
                    put("timestamp", key.timestamp)
                }
                val payload = JSONObject().apply {
                    put("action", "save_answer_key")
                    put("data", dataObj)
                }
                val (code, _) = postJson(serverUrl, apiKey, payload)
                code in 200..299
            } catch (e: Exception) {
                Log.e(TAG, "syncAnswerKey failed: ${e.message}", e)
                false
            }
        }
    }

    suspend fun syncQuestion(context: Context, question: QuestionEntity): Boolean {
        if (!SyncPreferences.isMySqlSyncEnabled(context)) return false
        val serverUrl = SyncPreferences.getMySqlServerUrl(context)
        val apiKey = SyncPreferences.getMySqlApiKey(context)
        if (serverUrl.isBlank()) return false

        return withContext(Dispatchers.IO) {
            try {
                val dataObj = JSONObject().apply {
                    put("id", question.id)
                    put("exam_id", question.examId)
                    put("text", question.text)
                    put("option_a", question.optionA)
                    put("option_b", question.optionB)
                    put("option_c", question.optionC)
                    put("option_d", question.optionD)
                    put("correct_index", question.correctIndex)
                }
                val payload = JSONObject().apply {
                    put("action", "save_question")
                    put("data", dataObj)
                }
                val (code, _) = postJson(serverUrl, apiKey, payload)
                code in 200..299
            } catch (e: Exception) {
                Log.e(TAG, "syncQuestion failed: ${e.message}", e)
                false
            }
        }
    }

    suspend fun deleteQuestion(context: Context, examId: Int, id: Int): Boolean {
        if (!SyncPreferences.isMySqlSyncEnabled(context)) return false
        val serverUrl = SyncPreferences.getMySqlServerUrl(context)
        val apiKey = SyncPreferences.getMySqlApiKey(context)
        if (serverUrl.isBlank()) return false

        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("action", "delete_question")
                    put("exam_id", examId)
                    put("id", id)
                }
                val (code, _) = postJson(serverUrl, apiKey, payload)
                code in 200..299
            } catch (e: Exception) {
                Log.e(TAG, "deleteQuestion failed: ${e.message}", e)
                false
            }
        }
    }

    suspend fun syncScanResult(context: Context, result: ScanResult): Boolean {
        if (!SyncPreferences.isMySqlSyncEnabled(context)) return false
        val serverUrl = SyncPreferences.getMySqlServerUrl(context)
        val apiKey = SyncPreferences.getMySqlApiKey(context)
        if (serverUrl.isBlank()) return false

        return withContext(Dispatchers.IO) {
            try {
                val dataObj = JSONObject().apply {
                    put("id", result.id)
                    put("exam_id", result.examId)
                    put("student_id", result.studentId)
                    put("paper_set", result.paperSet)
                    put("score", result.score.toDouble())
                    put("total_questions", result.totalQuestions)
                    put("student_answers", result.studentAnswers)
                    put("question_statuses", result.questionStatuses)
                    put("timestamp", result.timestamp)
                }
                val payload = JSONObject().apply {
                    put("action", "save_scan_result")
                    put("data", dataObj)
                }
                val (code, _) = postJson(serverUrl, apiKey, payload)
                code in 200..299
            } catch (e: Exception) {
                Log.e(TAG, "syncScanResult failed: ${e.message}", e)
                false
            }
        }
    }

    suspend fun bulkSync(
        context: Context,
        exams: List<Exam>,
        students: List<Student>,
        results: List<ScanResult>,
        answerKeys: List<com.example.data.AnswerKey> = emptyList(),
        questions: List<com.example.data.QuestionEntity> = emptyList()
    ): Pair<Boolean, String> {
        val serverUrl = SyncPreferences.getMySqlServerUrl(context)
        val apiKey = SyncPreferences.getMySqlApiKey(context)
        if (serverUrl.isBlank()) {
            return Pair(false, "Server URL is not configured")
        }

        return withContext(Dispatchers.IO) {
            try {
                val examsArray = JSONArray()
                exams.forEach { e ->
                    examsArray.put(JSONObject().apply {
                        put("id", e.id)
                        put("name", e.name)
                        put("subject", e.subject)
                        put("date", e.date)
                        put("title", e.title)
                        put("logo_url", e.logoUrl)
                        put("logo_opacity", e.logoOpacity.toDouble())
                        put("logo_size", e.logoSize.toDouble())
                        put("logo_position", e.logoPosition)
                        put("marks_per_question", e.marksPerQuestion.toDouble())
                        put("negative_marks", e.negativeMarks.toDouble())
                        put("pass_marks", e.passMarks.toDouble())
                        put("bonus_marks", e.bonusMarks.toDouble())
                        put("template_type", e.templateType)
                        put("timestamp", e.timestamp)
                    })
                }

                val studentsArray = JSONArray()
                students.forEach { s ->
                    studentsArray.put(JSONObject().apply {
                        put("roll_no", s.rollNo)
                        put("name", s.name)
                        put("father_name", s.fatherName)
                        put("mother_name", s.motherName)
                        put("gender", s.gender)
                        put("registration_no", s.registrationNo)
                        put("dob", s.dob)
                        put("mobile_no", s.mobileNo)
                        put("email", s.email)
                        put("stream", s.stream)
                        put("subjects", s.subjects)
                        put("image_url", s.imagePath)
                        put("timestamp", s.timestamp)
                    })
                }

                val keysArray = JSONArray()
                answerKeys.forEach { k ->
                    keysArray.put(JSONObject().apply {
                        put("id", k.id)
                        put("exam_id", k.examId)
                        put("set_name", k.setName)
                        put("num_questions", k.numQuestions)
                        put("num_options", k.numOptions)
                        put("correct_answers", k.correctAnswers)
                        put("timestamp", k.timestamp)
                    })
                }

                val questionsArray = JSONArray()
                questions.forEach { q ->
                    questionsArray.put(JSONObject().apply {
                        put("id", q.id)
                        put("exam_id", q.examId)
                        put("text", q.text)
                        put("option_a", q.optionA)
                        put("option_b", q.optionB)
                        put("option_c", q.optionC)
                        put("option_d", q.optionD)
                        put("correct_index", q.correctIndex)
                    })
                }

                val resultsArray = JSONArray()
                results.forEach { r ->
                    resultsArray.put(JSONObject().apply {
                        put("exam_id", r.examId)
                        put("student_id", r.studentId)
                        put("paper_set", r.paperSet)
                        put("score", r.score.toDouble())
                        put("total_questions", r.totalQuestions)
                        put("student_answers", r.studentAnswers)
                        put("question_statuses", r.questionStatuses)
                        put("timestamp", r.timestamp)
                    })
                }

                val payload = JSONObject().apply {
                    put("action", "bulk_sync")
                    put("data", JSONObject().apply {
                        put("exams", examsArray)
                        put("students", studentsArray)
                        put("answer_keys", keysArray)
                        put("questions", questionsArray)
                        put("scan_results", resultsArray)
                    })
                }

                val (code, response) = postJson(serverUrl, apiKey, payload)
                if (code in 200..299) {
                    SyncPreferences.setLastSyncTime(context, System.currentTimeMillis())
                    Pair(true, "Bulk sync successful! (${exams.size} Exams, ${answerKeys.size} Answer Keys, ${questions.size} Questions, ${students.size} Students, ${results.size} Results)")
                } else {
                    Pair(false, "Server Error ($code): $response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "bulkSync failed: ${e.message}", e)
                Pair(false, "Sync Error: ${e.message}")
            }
        }
    }
}
