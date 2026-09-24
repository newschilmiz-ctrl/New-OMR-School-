package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.FacultyMember
import com.example.data.NoticeRecord
import com.example.data.StudyMaterial
import com.example.data.TimetablePeriod
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CoachingFeaturesManager {
    private const val PREFS_NAME = "coaching_features_prefs"
    private const val KEY_NOTICES = "coaching_notices_json"
    private const val KEY_TIMETABLE = "coaching_timetable_json"
    private const val KEY_FACULTY = "coaching_faculty_json"
    private const val KEY_MATERIALS = "coaching_materials_json"
    private const val KEY_INIT = "coaching_features_init_v1"

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var cachedNotices: List<NoticeRecord>? = null

    @Volatile
    private var cachedTimetable: List<TimetablePeriod>? = null

    @Volatile
    private var cachedFaculty: List<FacultyMember>? = null

    @Volatile
    private var cachedMaterials: List<StudyMaterial>? = null

    private const val KEY_DEMO_CLEANED_V2 = "coaching_demo_cleaned_v2"

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            cleanDemoDataIfPresent()
        }
    }

    private fun cleanDemoDataIfPresent() {
        val p = prefs ?: return
        if (!p.getBoolean(KEY_DEMO_CLEANED_V2, false)) {
            // Purge demo seeded records
            p.edit()
                .putString(KEY_NOTICES, "[]")
                .putString(KEY_TIMETABLE, "[]")
                .putString(KEY_FACULTY, "[]")
                .putString(KEY_MATERIALS, "[]")
                .putBoolean(KEY_INIT, true)
                .putBoolean(KEY_DEMO_CLEANED_V2, true)
                .apply()
            cachedNotices = emptyList()
            cachedTimetable = emptyList()
            cachedFaculty = emptyList()
            cachedMaterials = emptyList()
        }
    }

    fun clearAllNotices() {
        saveNotices(emptyList())
    }

    fun clearAllTimetable() {
        saveTimetable(emptyList())
    }

    fun clearAllFaculty() {
        saveFaculty(emptyList())
    }

    fun clearAllMaterials() {
        saveMaterials(emptyList())
    }

    // ==================== NOTICES ====================
    fun getNotices(): List<NoticeRecord> {
        cachedNotices?.let { return it }
        val p = prefs ?: return emptyList()
        val jsonStr = p.getString(KEY_NOTICES, "[]") ?: "[]"
        val list = mutableListOf<NoticeRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    NoticeRecord(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        content = obj.optString("content"),
                        category = obj.optString("category", "GENERAL"),
                        targetBatch = obj.optString("targetBatch", "All Batches"),
                        postedBy = obj.optString("postedBy", "Director Office"),
                        date = obj.optString("date"),
                        isPinned = obj.optBoolean("isPinned", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val sorted = list.sortedWith(compareByDescending<NoticeRecord> { it.isPinned }.thenByDescending { it.timestamp })
        cachedNotices = sorted
        return sorted
    }

    fun addNotice(notice: NoticeRecord) {
        val current = getNotices().toMutableList()
        current.add(0, notice)
        saveNotices(current)
    }

    fun deleteNotice(id: String) {
        val current = getNotices().filterNot { it.id == id }
        saveNotices(current)
    }

    fun togglePinNotice(id: String) {
        val current = getNotices().map {
            if (it.id == id) it.copy(isPinned = !it.isPinned) else it
        }
        saveNotices(current)
    }

    private fun saveNotices(list: List<NoticeRecord>) {
        val sorted = list.sortedWith(compareByDescending<NoticeRecord> { it.isPinned }.thenByDescending { it.timestamp })
        cachedNotices = sorted
        val p = prefs ?: return
        val arr = JSONArray()
        for (n in sorted) {
            val obj = JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("category", n.category)
                put("targetBatch", n.targetBatch)
                put("postedBy", n.postedBy)
                put("date", n.date)
                put("isPinned", n.isPinned)
                put("timestamp", n.timestamp)
            }
            arr.put(obj)
        }
        p.edit().putString(KEY_NOTICES, arr.toString()).apply()
    }

    // ==================== TIMETABLE ====================
    fun getTimetable(): List<TimetablePeriod> {
        cachedTimetable?.let { return it }
        val p = prefs ?: return emptyList()
        val jsonStr = p.getString(KEY_TIMETABLE, "[]") ?: "[]"
        val list = mutableListOf<TimetablePeriod>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    TimetablePeriod(
                        id = obj.optString("id"),
                        batchId = obj.optString("batchId"),
                        batchName = obj.optString("batchName"),
                        dayOfWeek = obj.optString("dayOfWeek", "MONDAY"),
                        startTime = obj.optString("startTime"),
                        endTime = obj.optString("endTime"),
                        subject = obj.optString("subject"),
                        teacherName = obj.optString("teacherName"),
                        roomNumber = obj.optString("roomNumber", "Hall A"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedTimetable = list
        return list
    }

    fun addTimetablePeriod(period: TimetablePeriod) {
        val current = getTimetable().toMutableList()
        current.add(period)
        saveTimetable(current)
    }

    fun addTimetablePeriods(periods: List<TimetablePeriod>) {
        val current = getTimetable().toMutableList()
        current.addAll(periods)
        saveTimetable(current)
    }

    fun updateTimetablePeriod(period: TimetablePeriod) {
        val current = getTimetable().toMutableList()
        val index = current.indexOfFirst { it.id == period.id }
        if (index != -1) {
            current[index] = period
            saveTimetable(current)
        }
    }

    fun deleteTimetablePeriod(id: String) {
        val current = getTimetable().filterNot { it.id == id }
        saveTimetable(current)
    }

    fun clearTimetableForBatch(batchName: String) {
        val current = getTimetable().filterNot { it.batchName.equals(batchName, ignoreCase = true) }
        saveTimetable(current)
    }

    fun replaceTimetable(list: List<TimetablePeriod>) {
        saveTimetable(list)
    }

    private fun saveTimetable(list: List<TimetablePeriod>) {
        cachedTimetable = list
        val p = prefs ?: return
        val arr = JSONArray()
        for (t in list) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("batchId", t.batchId)
                put("batchName", t.batchName)
                put("dayOfWeek", t.dayOfWeek)
                put("startTime", t.startTime)
                put("endTime", t.endTime)
                put("subject", t.subject)
                put("teacherName", t.teacherName)
                put("roomNumber", t.roomNumber)
                put("timestamp", t.timestamp)
            }
            arr.put(obj)
        }
        p.edit().putString(KEY_TIMETABLE, arr.toString()).apply()
    }

    // ==================== FACULTY ====================
    fun getFaculty(): List<FacultyMember> {
        cachedFaculty?.let { return it }
        val p = prefs ?: return emptyList()
        val jsonStr = p.getString(KEY_FACULTY, "[]") ?: "[]"
        val list = mutableListOf<FacultyMember>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FacultyMember(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        subject = obj.optString("subject"),
                        qualification = obj.optString("qualification"),
                        phone = obj.optString("phone"),
                        email = obj.optString("email"),
                        salaryType = obj.optString("salaryType", "Monthly"),
                        assignedBatches = obj.optString("assignedBatches"),
                        status = obj.optString("status", "Active"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedFaculty = list
        return list
    }

    fun addFaculty(faculty: FacultyMember) {
        val current = getFaculty().toMutableList()
        current.add(faculty)
        saveFaculty(current)
    }

    fun deleteFaculty(id: String) {
        val current = getFaculty().filterNot { it.id == id }
        saveFaculty(current)
    }

    private fun saveFaculty(list: List<FacultyMember>) {
        cachedFaculty = list
        val p = prefs ?: return
        val arr = JSONArray()
        for (f in list) {
            val obj = JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("subject", f.subject)
                put("qualification", f.qualification)
                put("phone", f.phone)
                put("email", f.email)
                put("salaryType", f.salaryType)
                put("assignedBatches", f.assignedBatches)
                put("status", f.status)
                put("timestamp", f.timestamp)
            }
            arr.put(obj)
        }
        p.edit().putString(KEY_FACULTY, arr.toString()).apply()
    }

    // ==================== STUDY MATERIALS ====================
    fun getStudyMaterials(): List<StudyMaterial> {
        cachedMaterials?.let { return it }
        val p = prefs ?: return emptyList()
        val jsonStr = p.getString(KEY_MATERIALS, "[]") ?: "[]"
        val list = mutableListOf<StudyMaterial>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    StudyMaterial(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        type = obj.optString("type", "DPP"),
                        subject = obj.optString("subject"),
                        batchName = obj.optString("batchName", "All Batches"),
                        chapter = obj.optString("chapter"),
                        fileUrlOrInfo = obj.optString("fileUrlOrInfo"),
                        dueDate = obj.optString("dueDate"),
                        totalProblems = obj.optInt("totalProblems", 15),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val sorted = list.sortedByDescending { it.timestamp }
        cachedMaterials = sorted
        return sorted
    }

    fun addStudyMaterial(material: StudyMaterial) {
        val current = getStudyMaterials().toMutableList()
        current.add(0, material)
        saveMaterials(current)
    }

    fun deleteStudyMaterial(id: String) {
        val current = getStudyMaterials().filterNot { it.id == id }
        saveMaterials(current)
    }

    private fun saveMaterials(list: List<StudyMaterial>) {
        val sorted = list.sortedByDescending { it.timestamp }
        cachedMaterials = sorted
        val p = prefs ?: return
        val arr = JSONArray()
        for (m in sorted) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("title", m.title)
                put("type", m.type)
                put("subject", m.subject)
                put("batchName", m.batchName)
                put("chapter", m.chapter)
                put("fileUrlOrInfo", m.fileUrlOrInfo)
                put("dueDate", m.dueDate)
                put("totalProblems", m.totalProblems)
                put("timestamp", m.timestamp)
            }
            arr.put(obj)
        }
        p.edit().putString(KEY_MATERIALS, arr.toString()).apply()
    }
}
