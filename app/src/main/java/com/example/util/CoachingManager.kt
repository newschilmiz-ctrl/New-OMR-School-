package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.CoachingClass
import com.example.data.CoachingSession
import com.example.data.CoachingSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object CoachingManager {
    private const val PREFS_NAME = "coaching_control_prefs"
    private const val KEY_CLASSES = "coaching_classes_json"
    private const val KEY_SUBJECTS = "coaching_subjects_json"
    private const val KEY_SESSIONS = "coaching_sessions_json"
    private const val KEY_INITIALIZED = "coaching_initialized_v2"

    private const val FIREBASE_DB_URL = "https://new-ba-f9d96-default-rtdb.firebaseio.com"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            checkAndInitializeDefaults()
        }
    }

    private fun checkAndInitializeDefaults() {
        val p = prefs ?: return
        if (!p.getBoolean(KEY_INITIALIZED, false)) {
            // Seed clean starter classes
            val class10 = CoachingClass(id = "class_10", name = "Class 10th", section = "Batch A", description = "Foundation Board Preparation")
            val class11 = CoachingClass(id = "class_11", name = "Class 11th", section = "Main", description = "Senior Secondary Core")
            val class12 = CoachingClass(id = "class_12", name = "Class 12th", section = "Board Target", description = "Board & Competitive Target")
            val defaultClasses = listOf(class10, class11, class12)

            // Clean starter subjects organized by Streams (ARTS, SCIENCE, COMMERCE)
            val defaultSubjects = listOf(
                // SCIENCE
                CoachingSubject(name = "Physics", classId = class12.id, className = class12.name, stream = "SCIENCE"),
                CoachingSubject(name = "Chemistry", classId = class12.id, className = class12.name, stream = "SCIENCE"),
                CoachingSubject(name = "Mathematics", classId = class12.id, className = class12.name, stream = "SCIENCE"),
                CoachingSubject(name = "Biology", classId = class12.id, className = class12.name, stream = "SCIENCE"),
                // ARTS
                CoachingSubject(name = "History", classId = class12.id, className = class12.name, stream = "ARTS"),
                CoachingSubject(name = "Geography", classId = class12.id, className = class12.name, stream = "ARTS"),
                CoachingSubject(name = "Political Science", classId = class12.id, className = class12.name, stream = "ARTS"),
                CoachingSubject(name = "Economics (Arts)", classId = class12.id, className = class12.name, stream = "ARTS"),
                CoachingSubject(name = "Hindi Literature", classId = class12.id, className = class12.name, stream = "ARTS"),
                // COMMERCE
                CoachingSubject(name = "Accountancy", classId = class12.id, className = class12.name, stream = "COMMERCE"),
                CoachingSubject(name = "Business Studies", classId = class12.id, className = class12.name, stream = "COMMERCE"),
                CoachingSubject(name = "Economics (Commerce)", classId = class12.id, className = class12.name, stream = "COMMERCE"),
                CoachingSubject(name = "Entrepreneurship", classId = class12.id, className = class12.name, stream = "COMMERCE")
            )

            // Starter sessions with class & time session
            val defaultSessions = listOf(
                CoachingSession(title = "Morning Science Batch", classId = class12.id, className = class12.name, startTime = "07:00 AM", endTime = "08:30 AM", days = "Mon - Sat", academicYear = "2024-2025"),
                CoachingSession(title = "Commerce Prime Batch", classId = class12.id, className = class12.name, startTime = "09:00 AM", endTime = "10:30 AM", days = "Mon - Sat", academicYear = "2024-2025"),
                CoachingSession(title = "Arts Evening Session", classId = class12.id, className = class12.name, startTime = "04:00 PM", endTime = "05:30 PM", days = "Mon - Sat", academicYear = "2024-2025")
            )

            saveClassesLocal(defaultClasses)
            saveSubjectsLocal(defaultSubjects)
            saveSessionsLocal(defaultSessions)
            p.edit().putBoolean(KEY_INITIALIZED, true).apply()
        }
    }

    // ==================== CLASSES ====================
    fun getClasses(): List<CoachingClass> {
        val p = prefs ?: return emptyList()
        val jsonStr = p.getString(KEY_CLASSES, "[]") ?: "[]"
        val list = mutableListOf<CoachingClass>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CoachingClass(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        section = obj.optString("section", ""),
                        description = obj.optString("description", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveClassesLocal(classes: List<CoachingClass>) {
        val p = prefs ?: return
        val arr = JSONArray()
        for (c in classes) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("section", c.section)
                put("description", c.description)
                put("timestamp", c.timestamp)
            }
            arr.put(obj)
        }
        p.edit().putString(KEY_CLASSES, arr.toString()).apply()
    }

    suspend fun saveClass(coachingClass: CoachingClass) {
        val current = getClasses().toMutableList()
        val index = current.indexOfFirst { it.id == coachingClass.id }
        if (index >= 0) {
            current[index] = coachingClass
        } else {
            current.add(coachingClass)
        }
        saveClassesLocal(current)
        syncClassesToCloud(current)
    }

    suspend fun deleteClass(classId: String) {
        val current = getClasses().filter { it.id != classId }
        saveClassesLocal(current)
        syncClassesToCloud(current)
    }

    // ==================== SUBJECTS ====================
    fun getSubjects(): List<CoachingSubject> {
        val p = prefs ?: return emptyList()
        val jsonStr = p.getString(KEY_SUBJECTS, "[]") ?: "[]"
        val list = mutableListOf<CoachingSubject>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CoachingSubject(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        classId = obj.optString("classId", ""),
                        className = obj.optString("className", ""),
                        stream = obj.optString("stream", "SCIENCE"),
                        subjectCode = obj.optString("subjectCode", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveSubjectsLocal(subjects: List<CoachingSubject>) {
        val p = prefs ?: return
        val arr = JSONArray()
        for (s in subjects) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("classId", s.classId)
                put("className", s.className)
                put("stream", s.stream)
                put("subjectCode", s.subjectCode)
                put("timestamp", s.timestamp)
            }
            arr.put(obj)
        }
        p.edit().putString(KEY_SUBJECTS, arr.toString()).apply()
    }

    suspend fun addMultipleSubjects(
        classId: String,
        className: String,
        stream: String,
        subjectNames: List<String>
    ) {
        val current = getSubjects().toMutableList()
        for (rawName in subjectNames) {
            val trimmed = rawName.trim()
            if (trimmed.isEmpty()) continue
            // Check if already exists in this class and stream
            val exists = current.any { it.name.equals(trimmed, ignoreCase = true) && it.classId == classId && it.stream.equals(stream, ignoreCase = true) }
            if (!exists) {
                current.add(
                    CoachingSubject(
                        name = trimmed,
                        classId = classId,
                        className = className,
                        stream = stream.uppercase()
                    )
                )
            }
        }
        saveSubjectsLocal(current)
        syncSubjectsToCloud(current)
    }

    suspend fun saveSubject(subject: CoachingSubject) {
        val current = getSubjects().toMutableList()
        val index = current.indexOfFirst { it.id == subject.id }
        if (index >= 0) {
            current[index] = subject
        } else {
            current.add(subject)
        }
        saveSubjectsLocal(current)
        syncSubjectsToCloud(current)
    }

    suspend fun deleteSubject(subjectId: String) {
        val current = getSubjects().filter { it.id != subjectId }
        saveSubjectsLocal(current)
        syncSubjectsToCloud(current)
    }

    suspend fun clearDemoSubjectsAndReset() {
        val current = getSubjects().filter { 
            !it.name.equals("Demo Subject", ignoreCase = true) 
        }
        saveSubjectsLocal(current)
        syncSubjectsToCloud(current)
    }

    // ==================== SESSIONS ====================
    fun getSessions(): List<CoachingSession> {
        val p = prefs ?: return emptyList()
        val jsonStr = p.getString(KEY_SESSIONS, "[]") ?: "[]"
        val list = mutableListOf<CoachingSession>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CoachingSession(
                        id = obj.optString("id"),
                        title = obj.optString("title", ""),
                        classId = obj.optString("classId", ""),
                        className = obj.optString("className", ""),
                        startTime = obj.optString("startTime", "07:00 AM"),
                        endTime = obj.optString("endTime", "08:30 AM"),
                        days = obj.optString("days", "Mon - Sat"),
                        academicYear = obj.optString("academicYear", "2024-2025"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveSessionsLocal(sessions: List<CoachingSession>) {
        val p = prefs ?: return
        val arr = JSONArray()
        for (s in sessions) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("classId", s.classId)
                put("className", s.className)
                put("startTime", s.startTime)
                put("endTime", s.endTime)
                put("days", s.days)
                put("academicYear", s.academicYear)
                put("timestamp", s.timestamp)
            }
            arr.put(obj)
        }
        p.edit().putString(KEY_SESSIONS, arr.toString()).apply()
    }

    suspend fun saveSession(session: CoachingSession) {
        val current = getSessions().toMutableList()
        val index = current.indexOfFirst { it.id == session.id }
        if (index >= 0) {
            current[index] = session
        } else {
            current.add(session)
        }
        saveSessionsLocal(current)
        syncSessionsToCloud(current)
    }

    suspend fun deleteSession(sessionId: String) {
        val current = getSessions().filter { it.id != sessionId }
        saveSessionsLocal(current)
        syncSessionsToCloud(current)
    }

    // ==================== CLOUD SYNC (FIREBASE) ====================
    private suspend fun syncClassesToCloud(classes: List<CoachingClass>) {
        withContext(Dispatchers.IO) {
            try {
                val arr = JSONArray()
                for (c in classes) {
                    arr.put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("section", c.section)
                        put("description", c.description)
                        put("timestamp", c.timestamp)
                    })
                }
                sendToFirebase("coaching_classes", arr.toString())
            } catch (e: Exception) {
                Log.w("CoachingManager", "Cloud sync classes failed: ${e.message}")
            }
        }
    }

    private suspend fun syncSubjectsToCloud(subjects: List<CoachingSubject>) {
        withContext(Dispatchers.IO) {
            try {
                val arr = JSONArray()
                for (s in subjects) {
                    arr.put(JSONObject().apply {
                        put("id", s.id)
                        put("name", s.name)
                        put("classId", s.classId)
                        put("className", s.className)
                        put("stream", s.stream)
                        put("subjectCode", s.subjectCode)
                        put("timestamp", s.timestamp)
                    })
                }
                sendToFirebase("coaching_subjects", arr.toString())
            } catch (e: Exception) {
                Log.w("CoachingManager", "Cloud sync subjects failed: ${e.message}")
            }
        }
    }

    private suspend fun syncSessionsToCloud(sessions: List<CoachingSession>) {
        withContext(Dispatchers.IO) {
            try {
                val arr = JSONArray()
                for (s in sessions) {
                    arr.put(JSONObject().apply {
                        put("id", s.id)
                        put("title", s.title)
                        put("classId", s.classId)
                        put("className", s.className)
                        put("startTime", s.startTime)
                        put("endTime", s.endTime)
                        put("days", s.days)
                        put("academicYear", s.academicYear)
                        put("timestamp", s.timestamp)
                    })
                }
                sendToFirebase("coaching_sessions", arr.toString())
            } catch (e: Exception) {
                Log.w("CoachingManager", "Cloud sync sessions failed: ${e.message}")
            }
        }
    }

    private fun sendToFirebase(node: String, jsonContent: String) {
        val url = URL("$FIREBASE_DB_URL/$node.json")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        OutputStreamWriter(conn.outputStream).use { it.write(jsonContent) }
        val code = conn.responseCode
        conn.disconnect()
        Log.d("CoachingManager", "Firebase $node sync response: $code")
    }
}
