package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.AttendanceRecord
import com.example.data.FeeRecord
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FeeAndAttendanceManager {
    private const val PREFS_NAME = "coaching_fees_attendance_prefs"
    private const val KEY_FEES = "fees_json"
    private const val KEY_ATTENDANCE = "attendance_json"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            seedDefaultDataIfEmpty()
        }
    }

    private fun seedDefaultDataIfEmpty() {
        val p = prefs ?: return
        if (!p.contains(KEY_FEES)) {
            // Seed sample fee records if needed
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val sampleFees = listOf(
                FeeRecord(
                    studentRollNo = "240001",
                    studentName = "Rahul Sharma",
                    amountPaid = 3500.0,
                    totalFee = 12000.0,
                    paymentDate = today,
                    paymentMode = "UPI",
                    receiptNo = "REC-2401",
                    monthOrInstallment = "1st Installment",
                    remarks = "Enrolled in Science Batch"
                )
            )
            saveFees(sampleFees)
        }
    }

    // ==================== FEE RECORDS ====================
    fun getFees(): List<FeeRecord> {
        val p = prefs ?: return emptyList()
        val jsonStr = p.getString(KEY_FEES, "[]") ?: "[]"
        val list = mutableListOf<FeeRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FeeRecord(
                        id = obj.optString("id"),
                        studentRollNo = obj.optString("studentRollNo"),
                        studentName = obj.optString("studentName"),
                        amountPaid = obj.optDouble("amountPaid", 0.0),
                        totalFee = obj.optDouble("totalFee", 0.0),
                        paymentDate = obj.optString("paymentDate"),
                        paymentMode = obj.optString("paymentMode", "Cash"),
                        receiptNo = obj.optString("receiptNo"),
                        monthOrInstallment = obj.optString("monthOrInstallment"),
                        remarks = obj.optString("remarks"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun addFeeRecord(fee: FeeRecord) {
        val current = getFees().toMutableList()
        current.add(0, fee)
        saveFees(current)
    }

    fun deleteFeeRecord(id: String) {
        val current = getFees().filter { it.id != id }
        saveFees(current)
    }

    fun getFeesForStudent(rollNo: String): List<FeeRecord> {
        return getFees().filter { it.studentRollNo.equals(rollNo, ignoreCase = true) }
    }

    private fun saveFees(list: List<FeeRecord>) {
        val p = prefs ?: return
        val arr = JSONArray()
        for (f in list) {
            val obj = JSONObject().apply {
                put("id", f.id)
                put("studentRollNo", f.studentRollNo)
                put("studentName", f.studentName)
                put("amountPaid", f.amountPaid)
                put("totalFee", f.totalFee)
                put("paymentDate", f.paymentDate)
                put("paymentMode", f.paymentMode)
                put("receiptNo", f.receiptNo)
                put("monthOrInstallment", f.monthOrInstallment)
                put("remarks", f.remarks)
                put("timestamp", f.timestamp)
            }
            arr.put(obj)
        }
        p.edit().putString(KEY_FEES, arr.toString()).apply()
    }

    // ==================== ATTENDANCE RECORDS ====================
    fun getAttendance(): List<AttendanceRecord> {
        val p = prefs ?: return emptyList()
        val jsonStr = p.getString(KEY_ATTENDANCE, "[]") ?: "[]"
        val list = mutableListOf<AttendanceRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AttendanceRecord(
                        id = obj.optString("id"),
                        studentRollNo = obj.optString("studentRollNo"),
                        studentName = obj.optString("studentName"),
                        date = obj.optString("date"),
                        status = obj.optString("status", "PRESENT"),
                        sessionId = obj.optString("sessionId"),
                        sessionName = obj.optString("sessionName"),
                        inTime = obj.optString("inTime"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun markAttendance(
        studentRollNo: String,
        studentName: String,
        date: String,
        status: String,
        sessionId: String = "",
        sessionName: String = "",
        inTime: String = ""
    ) {
        val current = getAttendance().toMutableList()
        // Remove existing attendance for this student on this date & session if any
        current.removeAll {
            it.studentRollNo.equals(studentRollNo, ignoreCase = true) &&
                    it.date == date &&
                    (sessionId.isEmpty() || it.sessionId == sessionId)
        }

        val record = AttendanceRecord(
            studentRollNo = studentRollNo,
            studentName = studentName,
            date = date,
            status = status,
            sessionId = sessionId,
            sessionName = sessionName,
            inTime = inTime
        )
        current.add(0, record)
        saveAttendance(current)
    }

    fun markBatchAttendance(records: List<AttendanceRecord>) {
        val current = getAttendance().toMutableList()
        for (r in records) {
            current.removeAll {
                it.studentRollNo.equals(r.studentRollNo, ignoreCase = true) &&
                        it.date == r.date &&
                        (r.sessionId.isEmpty() || it.sessionId == r.sessionId)
            }
        }
        current.addAll(0, records)
        saveAttendance(current)
    }

    fun getAttendanceForDate(date: String): List<AttendanceRecord> {
        return getAttendance().filter { it.date == date }
    }

    fun getAttendanceForStudent(rollNo: String): List<AttendanceRecord> {
        return getAttendance().filter { it.studentRollNo.equals(rollNo, ignoreCase = true) }
    }

    private fun saveAttendance(list: List<AttendanceRecord>) {
        val p = prefs ?: return
        val arr = JSONArray()
        for (a in list) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("studentRollNo", a.studentRollNo)
                put("studentName", a.studentName)
                put("date", a.date)
                put("status", a.status)
                put("sessionId", a.sessionId)
                put("sessionName", a.sessionName)
                put("inTime", a.inTime)
                put("timestamp", a.timestamp)
            }
            arr.put(obj)
        }
        p.edit().putString(KEY_ATTENDANCE, arr.toString()).apply()
    }
}
