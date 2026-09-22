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

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            seedDefaultsIfEmpty()
        }
    }

    private fun seedDefaultsIfEmpty() {
        val p = prefs ?: return
        if (!p.getBoolean(KEY_INIT, false)) {
            val today = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

            // Seed Notices
            val notices = listOf(
                NoticeRecord(
                    title = "Grand Monthly Mock Test (OMR Sheet Based)",
                    content = "All students of Class 11th & 12th (Science & Commerce) must be present in full uniform with admit card on Sunday at 08:30 AM for the Full Syllabus OMR Simulation test.",
                    category = "EXAM_ALERT",
                    targetBatch = "All Batches",
                    postedBy = "Director Office",
                    date = today,
                    isPinned = true
                ),
                NoticeRecord(
                    title = "Diwali & Chhath Puja Holiday Schedule",
                    content = "Coaching classes will remain closed from Oct 28th to Nov 4th. Daily Practice Problem (DPP) assignments have been distributed and must be submitted on reopening day.",
                    category = "HOLIDAY",
                    targetBatch = "All Batches",
                    postedBy = "Administration",
                    date = today,
                    isPinned = false
                ),
                NoticeRecord(
                    title = "2nd Installment Tuition Fee Reminder",
                    content = "Kindly clear pending 2nd installment fees by the 10th of this month to avoid late administrative fee penalty. Collect payment receipt from the accounts counter.",
                    category = "FEE_ALERT",
                    targetBatch = "Morning Science Batch",
                    postedBy = "Accounts Dept",
                    date = today,
                    isPinned = false
                ),
                NoticeRecord(
                    title = "Special Doubt Clearing Session with Er. Verma",
                    content = "Extra 2-hour doubt clearing class on Rotational Motion & Calculus will be conducted this Saturday from 04:00 PM to 06:00 PM in Lecture Hall 2.",
                    category = "URGENT",
                    targetBatch = "Morning Science Batch",
                    postedBy = "Prof. R.K. Verma",
                    date = today,
                    isPinned = true
                )
            )
            saveNotices(notices)

            // Seed Timetable
            val timetable = listOf(
                TimetablePeriod(batchName = "Morning Science Batch", dayOfWeek = "MONDAY", startTime = "07:00 AM", endTime = "08:15 AM", subject = "Physics", teacherName = "Er. R.K. Verma", roomNumber = "Hall 101"),
                TimetablePeriod(batchName = "Morning Science Batch", dayOfWeek = "MONDAY", startTime = "08:20 AM", endTime = "09:35 AM", subject = "Mathematics", teacherName = "Prof. S.N. Mishra", roomNumber = "Hall 101"),
                TimetablePeriod(batchName = "Morning Science Batch", dayOfWeek = "MONDAY", startTime = "09:45 AM", endTime = "11:00 AM", subject = "Chemistry", teacherName = "Dr. Anita Gupta", roomNumber = "Lab 2"),
                TimetablePeriod(batchName = "Morning Science Batch", dayOfWeek = "TUESDAY", startTime = "07:00 AM", endTime = "08:15 AM", subject = "Biology", teacherName = "Dr. Neha Sharma", roomNumber = "Bio Lab"),
                TimetablePeriod(batchName = "Morning Science Batch", dayOfWeek = "TUESDAY", startTime = "08:20 AM", endTime = "09:35 AM", subject = "Physics", teacherName = "Er. R.K. Verma", roomNumber = "Hall 101"),
                TimetablePeriod(batchName = "Morning Science Batch", dayOfWeek = "WEDNESDAY", startTime = "07:00 AM", endTime = "08:15 AM", subject = "Chemistry", teacherName = "Dr. Anita Gupta", roomNumber = "Lab 2"),
                TimetablePeriod(batchName = "Morning Science Batch", dayOfWeek = "WEDNESDAY", startTime = "08:20 AM", endTime = "09:35 AM", subject = "Mathematics", teacherName = "Prof. S.N. Mishra", roomNumber = "Hall 101"),
                TimetablePeriod(batchName = "Morning Science Batch", dayOfWeek = "THURSDAY", startTime = "07:00 AM", endTime = "08:15 AM", subject = "Physics", teacherName = "Er. R.K. Verma", roomNumber = "Hall 101"),
                TimetablePeriod(batchName = "Morning Science Batch", dayOfWeek = "FRIDAY", startTime = "07:00 AM", endTime = "08:15 AM", subject = "Chemistry", teacherName = "Dr. Anita Gupta", roomNumber = "Lab 2"),
                TimetablePeriod(batchName = "Morning Science Batch", dayOfWeek = "SATURDAY", startTime = "07:00 AM", endTime = "09:00 AM", subject = "Full Test OMR Simulation", teacherName = "Exam Invigilator", roomNumber = "Auditorium"),

                TimetablePeriod(batchName = "Commerce Prime Batch", dayOfWeek = "MONDAY", startTime = "09:00 AM", endTime = "10:15 AM", subject = "Accountancy", teacherName = "CA Manish Aggarwal", roomNumber = "Room 201"),
                TimetablePeriod(batchName = "Commerce Prime Batch", dayOfWeek = "MONDAY", startTime = "10:20 AM", endTime = "11:35 AM", subject = "Economics", teacherName = "Prof. Priya Sinha", roomNumber = "Room 201"),
                TimetablePeriod(batchName = "Commerce Prime Batch", dayOfWeek = "TUESDAY", startTime = "09:00 AM", endTime = "10:15 AM", subject = "Business Studies", teacherName = "Dr. K.P. Thakur", roomNumber = "Room 201")
            )
            saveTimetable(timetable)

            // Seed Faculty
            val faculties = listOf(
                FacultyMember(name = "Er. R.K. Verma", subject = "Physics", qualification = "B.Tech IIT Kanpur (12 yrs exp)", phone = "+91 98765 43210", email = "rkverma@coaching.com", salaryType = "Monthly", assignedBatches = "Morning Science Batch, JEE Target", status = "Active"),
                FacultyMember(name = "Dr. Anita Gupta", subject = "Chemistry", qualification = "Ph.D Organic Chemistry (9 yrs exp)", phone = "+91 98765 43211", email = "anita.gupta@coaching.com", salaryType = "Monthly", assignedBatches = "Morning Science Batch, NEET Focus", status = "Active"),
                FacultyMember(name = "Prof. S.N. Mishra", subject = "Mathematics", qualification = "M.Sc Mathematics, Gold Medalist", phone = "+91 98765 43212", email = "snmishra@coaching.com", salaryType = "Monthly", assignedBatches = "Morning Science Batch, 10th Foundation", status = "Active"),
                FacultyMember(name = "Dr. Neha Sharma", subject = "Biology (Botany & Zoology)", qualification = "MBBS, M.S (AIIMS)", phone = "+91 98765 43213", email = "neha.bio@coaching.com", salaryType = "Per Lecture", assignedBatches = "NEET Target Batch", status = "Active"),
                FacultyMember(name = "CA Manish Aggarwal", subject = "Accountancy & Taxation", qualification = "FCA, B.Com (Hons)", phone = "+91 98765 43214", email = "manish.ca@coaching.com", salaryType = "Monthly", assignedBatches = "Commerce Prime Batch", status = "Active"),
                FacultyMember(name = "Prof. Priya Sinha", subject = "Economics & Statistics", qualification = "M.A Economics (DSE)", phone = "+91 98765 43215", email = "priya.eco@coaching.com", salaryType = "Per Lecture", assignedBatches = "Commerce Prime Batch, Arts Evening", status = "Active")
            )
            saveFaculty(faculties)

            // Seed Study Material / DPP
            val materials = listOf(
                StudyMaterial(title = "DPP-04: Laws of Motion & Friction", type = "DPP", subject = "Physics", batchName = "Morning Science Batch", chapter = "Newton's Laws of Motion", fileUrlOrInfo = "physics_dpp_04_friction.pdf", dueDate = "Tomorrow 08:00 AM", totalProblems = 20),
                StudyMaterial(title = "Formula Handbook: Integration & Differential Calculus", type = "FORMULA_BOOK", subject = "Mathematics", batchName = "Morning Science Batch", chapter = "Calculus & Limits", fileUrlOrInfo = "math_formula_sheet_v2.pdf", dueDate = "Permanent Reference", totalProblems = 65),
                StudyMaterial(title = "Comprehensive Notes: Chemical Bonding & Molecular Structure", type = "NOTES", subject = "Chemistry", batchName = "Morning Science Batch", chapter = "Inorganic Chemistry", fileUrlOrInfo = "chem_bonding_notes_2024.pdf", dueDate = "Exam Preparation", totalProblems = 35),
                StudyMaterial(title = "DPP-02: Partnership Deeds & GoodWill Valuation", type = "DPP", subject = "Accountancy", batchName = "Commerce Prime Batch", chapter = "Partnership Accounts", fileUrlOrInfo = "acc_partnership_dpp2.pdf", dueDate = "Friday 09:00 AM", totalProblems = 15),
                StudyMaterial(title = "OMR Mock Test 1 Answer Key & Solutions", type = "SOLUTION", subject = "Physics", batchName = "All Batches", chapter = "Full Term 1 Review", fileUrlOrInfo = "omr_mock_1_solutions.pdf", dueDate = "Self Review", totalProblems = 50)
            )
            saveMaterials(materials)

            p.edit().putBoolean(KEY_INIT, true).apply()
        }
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

    fun deleteTimetablePeriod(id: String) {
        val current = getTimetable().filterNot { it.id == id }
        saveTimetable(current)
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
