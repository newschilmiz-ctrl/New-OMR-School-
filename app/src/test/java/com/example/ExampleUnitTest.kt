package com.example

import com.example.data.TimetablePeriod
import com.example.util.ClashType
import com.example.util.DynamicTimetableEngine
import com.example.util.SubjectRequirement
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testDynamicTimetableGeneration_createsClashFreeSchedule() {
        val (batchName, requirements) = DynamicTimetableEngine.getPresetRequirements("JEE Advanced (PCM)")
        val result = DynamicTimetableEngine.generateSchedule(
            batchName = batchName,
            subjects = requirements
        )

        assertTrue(result.periods.isNotEmpty())
        assertEquals(result.totalRequestedClasses, result.totalSlotsFilled + result.unassignedClasses)

        // Verify intra-batch collision free
        val clashes = DynamicTimetableEngine.findConflicts(result.periods)
        assertEquals(0, clashes.size)
    }

    @Test
    fun testConflictDetector_flagsTeacherAndRoomDoubleBooking() {
        val periodA = TimetablePeriod(
            id = "1",
            batchName = "Batch A",
            dayOfWeek = "MONDAY",
            startTime = "08:00 AM",
            endTime = "08:50 AM",
            subject = "Physics",
            teacherName = "Dr. R.K. Verma",
            roomNumber = "Room 101"
        )

        val periodB = TimetablePeriod(
            id = "2",
            batchName = "Batch B",
            dayOfWeek = "MONDAY",
            startTime = "08:00 AM",
            endTime = "08:50 AM",
            subject = "Chemistry",
            teacherName = "Dr. R.K. Verma", // Same teacher at same time!
            roomNumber = "Room 101" // Same room at same time!
        )

        val clashes = DynamicTimetableEngine.findConflicts(listOf(periodA, periodB))
        assertTrue(clashes.isNotEmpty())
        assertTrue(clashes.any { it.type == ClashType.TEACHER_DOUBLE_BOOKED })
        assertTrue(clashes.any { it.type == ClashType.ROOM_DOUBLE_BOOKED })
    }

    @Test
    fun testWorkloadAndOccupancyCalculations() {
        val periodA = TimetablePeriod(
            id = "1",
            batchName = "Batch A",
            dayOfWeek = "MONDAY",
            startTime = "08:00 AM",
            endTime = "08:50 AM",
            subject = "Physics",
            teacherName = "Dr. Verma",
            roomNumber = "Room 101"
        )
        val periodB = TimetablePeriod(
            id = "2",
            batchName = "Batch A",
            dayOfWeek = "TUESDAY",
            startTime = "08:00 AM",
            endTime = "08:50 AM",
            subject = "Physics",
            teacherName = "Dr. Verma",
            roomNumber = "Room 101"
        )

        val workloads = DynamicTimetableEngine.calculateTeacherWorkload(listOf(periodA, periodB))
        assertEquals(1, workloads.size)
        assertEquals(2, workloads.first().totalPeriods)
        assertEquals(2, workloads.first().daysActive.size)

        val occupancy = DynamicTimetableEngine.calculateRoomOccupancy(listOf(periodA, periodB))
        assertEquals(1, occupancy.size)
        assertEquals("Room 101", occupancy.first().roomNumber)
        assertEquals(2, occupancy.first().totalPeriods)
    }

    @Test
    fun testExportShareText_containsBatchAndPeriods() {
        val period = TimetablePeriod(
            id = "1",
            batchName = "Target Batch",
            dayOfWeek = "MONDAY",
            startTime = "09:00 AM",
            endTime = "09:50 AM",
            subject = "Advanced Mathematics",
            teacherName = "Er. S.K. Jha",
            roomNumber = "Audi 1"
        )

        val text = DynamicTimetableEngine.exportTimetableAsShareText(listOf(period), "Target Batch")
        assertTrue(text.contains("Target Batch"))
        assertTrue(text.contains("Advanced Mathematics"))
        assertTrue(text.contains("Er. S.K. Jha"))
        assertTrue(text.contains("Audi 1"))
    }
}
