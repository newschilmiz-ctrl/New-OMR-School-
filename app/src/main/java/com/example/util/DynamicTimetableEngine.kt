package com.example.util

import com.example.data.TimetablePeriod
import java.util.UUID

data class SubjectRequirement(
    val subject: String,
    val teacherName: String,
    val roomNumber: String,
    val weeklyClasses: Int = 5
)

data class TimeSlotDef(
    val slotIndex: Int,
    val label: String,
    val startTime: String,
    val endTime: String,
    val isBreak: Boolean = false
)

data class ScheduleClash(
    val id: String = UUID.randomUUID().toString(),
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val type: ClashType,
    val entityName: String, // e.g., teacher name or room number
    val batchA: String,
    val batchB: String,
    val subjectA: String,
    val subjectB: String
)

enum class ClashType {
    TEACHER_DOUBLE_BOOKED,
    ROOM_DOUBLE_BOOKED,
    BATCH_DOUBLE_BOOKED
}

data class GenerationResult(
    val periods: List<TimetablePeriod>,
    val totalSlotsFilled: Int,
    val totalRequestedClasses: Int,
    val unassignedClasses: Int,
    val clashesAvoided: Int,
    val message: String
)

data class TeacherWorkload(
    val teacherName: String,
    val totalPeriods: Int,
    val subjects: List<String>,
    val daysActive: Set<String>
)

data class RoomOccupancy(
    val roomNumber: String,
    val totalPeriods: Int,
    val utilizationPercentage: Float,
    val busySlots: List<Pair<String, String>> // (Day, StartTime)
)

object DynamicTimetableEngine {

    val DAYS = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val WORKING_DAYS = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")

    val DEFAULT_ROOMS = listOf(
        "Room 101 (Smart Hall)",
        "Room 102 (Theory)",
        "Room 103 (Interactive)",
        "Hall A (Auditorium)",
        "Lab 1 (Physics/Chem)",
        "Lab 2 (Bio/Computer)"
    )

    val DEFAULT_TEACHERS = listOf(
        "Dr. R.K. Verma",
        "Prof. Ananya Sen",
        "Er. S.K. Jha",
        "Dr. Preeti Mishra",
        "Mrs. Sunita Roy",
        "Er. Vikram Rathore",
        "Prof. Amit Saxena"
    )

    fun getDefaultTimeSlots(): List<TimeSlotDef> {
        return listOf(
            TimeSlotDef(1, "Period 1", "08:00 AM", "08:50 AM"),
            TimeSlotDef(2, "Period 2", "08:55 AM", "09:45 AM"),
            TimeSlotDef(3, "Period 3", "09:50 AM", "10:40 AM"),
            TimeSlotDef(4, "Interval Break", "10:40 AM", "11:00 AM", isBreak = true),
            TimeSlotDef(5, "Period 4", "11:00 AM", "11:50 AM"),
            TimeSlotDef(6, "Period 5", "11:55 AM", "12:45 PM"),
            TimeSlotDef(7, "Period 6", "12:50 PM", "01:40 PM")
        )
    }

    fun getAfternoonSlots(): List<TimeSlotDef> {
        return listOf(
            TimeSlotDef(1, "Period 1", "02:00 PM", "02:50 PM"),
            TimeSlotDef(2, "Period 2", "02:55 PM", "03:45 PM"),
            TimeSlotDef(3, "Period 3", "03:50 PM", "04:40 PM"),
            TimeSlotDef(4, "Tea Break", "04:40 PM", "05:00 PM", isBreak = true),
            TimeSlotDef(5, "Period 4", "05:00 PM", "05:50 PM"),
            TimeSlotDef(6, "Period 5", "05:55 PM", "06:45 PM")
        )
    }

    fun getPresetRequirements(presetName: String): Pair<String, List<SubjectRequirement>> {
        return when (presetName) {
            "JEE Advanced (PCM)" -> Pair(
                "Class 12 - JEE Advanced",
                listOf(
                    SubjectRequirement("Physics (Mechanics & Waves)", "Dr. R.K. Verma", "Room 101 (Smart Hall)", 6),
                    SubjectRequirement("Chemistry (Organic & Physical)", "Prof. Ananya Sen", "Room 101 (Smart Hall)", 6),
                    SubjectRequirement("Mathematics (Calculus & Algebra)", "Er. S.K. Jha", "Room 101 (Smart Hall)", 6),
                    SubjectRequirement("Physics Lab / Problem Solving", "Er. Vikram Rathore", "Lab 1 (Physics/Chem)", 3),
                    SubjectRequirement("Doubt Clearing & Mock Test", "Dr. R.K. Verma", "Hall A (Auditorium)", 3)
                )
            )
            "NEET Medical (PCB)" -> Pair(
                "Class 12 - NEET Medical",
                listOf(
                    SubjectRequirement("Biology (Botany & Zoology)", "Dr. Preeti Mishra", "Room 102 (Theory)", 8),
                    SubjectRequirement("Physics (Optics & Electromagnetism)", "Dr. R.K. Verma", "Room 102 (Theory)", 6),
                    SubjectRequirement("Chemistry (Inorganic & Bio-chem)", "Prof. Ananya Sen", "Room 102 (Theory)", 6),
                    SubjectRequirement("Biology Dissection & Micro Lab", "Dr. Preeti Mishra", "Lab 2 (Bio/Computer)", 3),
                    SubjectRequirement("Weekly Test Analysis", "Prof. Amit Saxena", "Hall A (Auditorium)", 2)
                )
            )
            "Foundation Class 10" -> Pair(
                "Class 10 - Foundation",
                listOf(
                    SubjectRequirement("Mathematics (Standard)", "Er. S.K. Jha", "Room 103 (Interactive)", 6),
                    SubjectRequirement("Science (Physics & Chem)", "Dr. R.K. Verma", "Room 103 (Interactive)", 5),
                    SubjectRequirement("Science (Biology)", "Dr. Preeti Mishra", "Room 103 (Interactive)", 4),
                    SubjectRequirement("English Language & Lit", "Mrs. Sunita Roy", "Room 103 (Interactive)", 4),
                    SubjectRequirement("Social Science (SST)", "Prof. Amit Saxena", "Room 103 (Interactive)", 4),
                    SubjectRequirement("Mental Ability & Reasoning", "Prof. Amit Saxena", "Room 103 (Interactive)", 2)
                )
            )
            else -> Pair(
                "Class 11 - Evening Batch",
                listOf(
                    SubjectRequirement("Physics", "Dr. R.K. Verma", "Room 101 (Smart Hall)", 5),
                    SubjectRequirement("Chemistry", "Prof. Ananya Sen", "Room 101 (Smart Hall)", 5),
                    SubjectRequirement("Mathematics", "Er. S.K. Jha", "Room 101 (Smart Hall)", 5),
                    SubjectRequirement("English", "Mrs. Sunita Roy", "Room 101 (Smart Hall)", 3)
                )
            )
        }
    }

    /**
     * Constraint-based generator algorithm:
     * Generates a collision-free timetable schedule for the target batch
     * based on subject requirements, room availability, and teacher schedules,
     * avoiding clashes with all existing periods in other batches!
     */
    fun generateSchedule(
        batchName: String,
        subjects: List<SubjectRequirement>,
        days: List<String> = WORKING_DAYS,
        timeSlots: List<TimeSlotDef> = getDefaultTimeSlots().filterNot { it.isBreak },
        existingPeriods: List<TimetablePeriod> = emptyList(),
        replaceBatchExisting: Boolean = true
    ): GenerationResult {
        val nonConflictingExisting = if (replaceBatchExisting) {
            existingPeriods.filterNot { it.batchName.equals(batchName, ignoreCase = true) }
        } else {
            existingPeriods
        }

        val subjectPool = mutableListOf<SubjectRequirement>()
        for (sub in subjects) {
            repeat(sub.weeklyClasses) {
                subjectPool.add(sub)
            }
        }
        val totalRequested = subjectPool.size

        val newPeriods = mutableListOf<TimetablePeriod>()
        var clashesAvoided = 0

        // Track allocations per day to distribute subjects evenly
        val dailySubjectCount = mutableMapOf<Pair<String, String>, Int>() // (Day, Subject) -> Count
        val lastSubjectInDay = mutableMapOf<String, String>() // Day -> Last Subject

        // Matrix of available slots (Day, Slot)
        val availableSlots = mutableListOf<Pair<String, TimeSlotDef>>()
        for (day in days) {
            for (slot in timeSlots) {
                availableSlots.add(Pair(day, slot))
            }
        }

        // Shuffle subject pool slightly to avoid clustering while keeping deterministic round-robin
        subjectPool.sortByDescending { it.weeklyClasses }

        // Iterate through each day and slot
        for ((day, slot) in availableSlots) {
            if (subjectPool.isEmpty()) break

            // Find best candidate subject that:
            // 1. Teacher is free at this day & slot
            // 2. Room is free at this day & slot
            // 3. Subject has not exceeded reasonable daily limit (max 2 per day)
            // 4. Not identical to immediate previous period if possible
            val candidateIndex = subjectPool.indexOfFirst { candidate ->
                // Check teacher clash with existing and already-scheduled new periods
                val teacherClash = hasTeacherClash(
                    teacherName = candidate.teacherName,
                    dayOfWeek = day,
                    startTime = slot.startTime,
                    otherPeriods = nonConflictingExisting + newPeriods
                )
                if (teacherClash) {
                    clashesAvoided++
                    return@indexOfFirst false
                }

                // Check room clash
                val roomClash = hasRoomClash(
                    roomNumber = candidate.roomNumber,
                    dayOfWeek = day,
                    startTime = slot.startTime,
                    otherPeriods = nonConflictingExisting + newPeriods
                )
                if (roomClash) {
                    clashesAvoided++
                    return@indexOfFirst false
                }

                // Distribution heuristics: max 2 periods of same subject per day
                val countToday = dailySubjectCount[Pair(day, candidate.subject)] ?: 0
                if (countToday >= 2 && subjectPool.any { dailySubjectCount[Pair(day, it.subject)] ?: 0 < 2 }) {
                    return@indexOfFirst false
                }

                // Avoid back-to-back same subject if other subjects are waiting
                val lastSub = lastSubjectInDay[day]
                if (lastSub == candidate.subject && subjectPool.any { it.subject != candidate.subject }) {
                    return@indexOfFirst false
                }

                true
            }

            if (candidateIndex != -1) {
                val chosen = subjectPool.removeAt(candidateIndex)
                val period = TimetablePeriod(
                    id = UUID.randomUUID().toString(),
                    batchId = UUID.randomUUID().toString(),
                    batchName = batchName,
                    dayOfWeek = day,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    subject = chosen.subject,
                    teacherName = chosen.teacherName,
                    roomNumber = chosen.roomNumber,
                    timestamp = System.currentTimeMillis()
                )
                newPeriods.add(period)
                dailySubjectCount[Pair(day, chosen.subject)] = (dailySubjectCount[Pair(day, chosen.subject)] ?: 0) + 1
                lastSubjectInDay[day] = chosen.subject
            }
        }

        // If there are still remaining subjects because of strict distribution, place remaining in available slots with relaxed limits
        if (subjectPool.isNotEmpty()) {
            for ((day, slot) in availableSlots) {
                if (subjectPool.isEmpty()) break
                // Check if this slot already has a class in newPeriods
                val slotOccupied = newPeriods.any { it.dayOfWeek.equals(day, ignoreCase = true) && it.startTime == slot.startTime }
                if (slotOccupied) continue

                val candidateIndex = subjectPool.indexOfFirst { candidate ->
                    val teacherClash = hasTeacherClash(candidate.teacherName, day, slot.startTime, nonConflictingExisting + newPeriods)
                    val roomClash = hasRoomClash(candidate.roomNumber, day, slot.startTime, nonConflictingExisting + newPeriods)
                    !teacherClash && !roomClash
                }

                if (candidateIndex != -1) {
                    val chosen = subjectPool.removeAt(candidateIndex)
                    newPeriods.add(
                        TimetablePeriod(
                            id = UUID.randomUUID().toString(),
                            batchId = UUID.randomUUID().toString(),
                            batchName = batchName,
                            dayOfWeek = day,
                            startTime = slot.startTime,
                            endTime = slot.endTime,
                            subject = chosen.subject,
                            teacherName = chosen.teacherName,
                            roomNumber = chosen.roomNumber,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        val unassigned = subjectPool.size
        val message = if (unassigned == 0) {
            "Successfully generated 100% clash-free schedule with ${newPeriods.size} periods across ${days.size} days."
        } else {
            "Generated ${newPeriods.size} periods. $unassigned periods could not be placed due to slot/room constraints."
        }

        return GenerationResult(
            periods = newPeriods,
            totalSlotsFilled = newPeriods.size,
            totalRequestedClasses = totalRequested,
            unassignedClasses = unassigned,
            clashesAvoided = clashesAvoided,
            message = message
        )
    }

    fun hasTeacherClash(
        teacherName: String,
        dayOfWeek: String,
        startTime: String,
        otherPeriods: List<TimetablePeriod>
    ): Boolean {
        return otherPeriods.any {
            it.dayOfWeek.equals(dayOfWeek, ignoreCase = true) &&
                    it.startTime.equals(startTime, ignoreCase = true) &&
                    it.teacherName.equals(teacherName, ignoreCase = true)
        }
    }

    fun hasRoomClash(
        roomNumber: String,
        dayOfWeek: String,
        startTime: String,
        otherPeriods: List<TimetablePeriod>
    ): Boolean {
        return otherPeriods.any {
            it.dayOfWeek.equals(dayOfWeek, ignoreCase = true) &&
                    it.startTime.equals(startTime, ignoreCase = true) &&
                    it.roomNumber.equals(roomNumber, ignoreCase = true)
        }
    }

    /**
     * Scans any timetable list for conflicts
     */
    fun findConflicts(periods: List<TimetablePeriod>): List<ScheduleClash> {
        val clashes = mutableListOf<ScheduleClash>()

        for (i in periods.indices) {
            for (j in i + 1 until periods.size) {
                val a = periods[i]
                val b = periods[j]

                if (a.dayOfWeek.equals(b.dayOfWeek, ignoreCase = true) && a.startTime.equals(b.startTime, ignoreCase = true)) {
                    // Check teacher clash
                    if (a.teacherName.equals(b.teacherName, ignoreCase = true) && !a.batchName.equals(b.batchName, ignoreCase = true)) {
                        clashes.add(
                            ScheduleClash(
                                dayOfWeek = a.dayOfWeek,
                                startTime = a.startTime,
                                endTime = a.endTime,
                                type = ClashType.TEACHER_DOUBLE_BOOKED,
                                entityName = a.teacherName,
                                batchA = a.batchName,
                                batchB = b.batchName,
                                subjectA = a.subject,
                                subjectB = b.subject
                            )
                        )
                    }

                    // Check room clash
                    if (a.roomNumber.equals(b.roomNumber, ignoreCase = true) && !a.batchName.equals(b.batchName, ignoreCase = true)) {
                        clashes.add(
                            ScheduleClash(
                                dayOfWeek = a.dayOfWeek,
                                startTime = a.startTime,
                                endTime = a.endTime,
                                type = ClashType.ROOM_DOUBLE_BOOKED,
                                entityName = a.roomNumber,
                                batchA = a.batchName,
                                batchB = b.batchName,
                                subjectA = a.subject,
                                subjectB = b.subject
                            )
                        )
                    }

                    // Check batch clash
                    if (a.batchName.equals(b.batchName, ignoreCase = true) && a.id != b.id) {
                        clashes.add(
                            ScheduleClash(
                                dayOfWeek = a.dayOfWeek,
                                startTime = a.startTime,
                                endTime = a.endTime,
                                type = ClashType.BATCH_DOUBLE_BOOKED,
                                entityName = a.batchName,
                                batchA = a.batchName,
                                batchB = b.batchName,
                                subjectA = a.subject,
                                subjectB = b.subject
                            )
                        )
                    }
                }
            }
        }
        return clashes
    }

    /**
     * Calculate teacher workloads for analytics & balance
     */
    fun calculateTeacherWorkload(periods: List<TimetablePeriod>): List<TeacherWorkload> {
        val map = periods.groupBy { it.teacherName }
        return map.map { (teacher, list) ->
            TeacherWorkload(
                teacherName = teacher,
                totalPeriods = list.size,
                subjects = list.map { it.subject }.distinct(),
                daysActive = list.map { it.dayOfWeek }.toSet()
            )
        }.sortedByDescending { it.totalPeriods }
    }

    /**
     * Calculate room occupancy rates
     */
    fun calculateRoomOccupancy(periods: List<TimetablePeriod>, totalSlotsPerWeek: Int = 36): List<RoomOccupancy> {
        val map = periods.groupBy { it.roomNumber }
        return map.map { (room, list) ->
            val busy = list.map { Pair(it.dayOfWeek, it.startTime) }
            RoomOccupancy(
                roomNumber = room,
                totalPeriods = list.size,
                utilizationPercentage = ((list.size.toFloat() / totalSlotsPerWeek.toFloat()) * 100f).coerceIn(0f, 100f),
                busySlots = busy
            )
        }.sortedByDescending { it.totalPeriods }
    }

    /**
     * Formats the timetable as clean text suitable for WhatsApp / Telegram sharing
     */
    fun exportTimetableAsShareText(periods: List<TimetablePeriod>, batchFilter: String? = null): String {
        val filtered = if (batchFilter != null && batchFilter != "ALL") {
            periods.filter { it.batchName.equals(batchFilter, ignoreCase = true) }
        } else {
            periods
        }

        val sb = StringBuilder()
        sb.append("📅 COACHING CLASS ROUTINE & TIMETABLE\n")
        if (batchFilter != null && batchFilter != "ALL") {
            sb.append("🎓 Batch: $batchFilter\n")
        }
        sb.append("════════════════════════════════════\n\n")

        for (day in DAYS) {
            val dayPeriods = filtered.filter { it.dayOfWeek.equals(day, ignoreCase = true) }
                .sortedBy { it.startTime }
            if (dayPeriods.isNotEmpty()) {
                sb.append("📌 $day (${dayPeriods.size} Classes)\n")
                sb.append("────────────────────────────────────\n")
                dayPeriods.forEachIndexed { index, p ->
                    sb.append("${index + 1}. [${p.startTime} - ${p.endTime}]\n")
                    sb.append("   📖 ${p.subject}\n")
                    sb.append("   👨‍🏫 Faculty: ${p.teacherName}\n")
                    sb.append("   🏢 Room: ${p.roomNumber}")
                    if (batchFilter == "ALL" || batchFilter == null) {
                        sb.append(" | Batch: ${p.batchName}")
                    }
                    sb.append("\n\n")
                }
            }
        }
        sb.append("════════════════════════════════════\n")
        sb.append("Generated with Coaching Institute Dynamic Timetable Manager")
        return sb.toString()
    }
}
