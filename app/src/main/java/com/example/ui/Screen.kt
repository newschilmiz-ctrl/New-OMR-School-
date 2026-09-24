package com.example.ui

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Students : Screen("students")
    object StudentAdmission : Screen("student_admission")
    object CreateExam : Screen("create_exam")
    object ExamDashboard : Screen("exam_dashboard/{examId}?tab={tab}") {
        fun createRoute(examId: Int, tab: Int = 0) = "exam_dashboard/$examId?tab=$tab"
    }
    object ScanOmr : Screen("scan_omr/{examId}") {
        fun createRoute(examId: Int) = "scan_omr/$examId"
    }
    object CustomOmrDesigner : Screen("custom_omr_designer")
    object SyncSettings : Screen("sync_settings")
    object CoachingControl : Screen("coaching_control")
    object CoachingSubjects : Screen("coaching_subjects")
    object StudentCards : Screen("student_cards/{rollNo}") {
        fun createRoute(rollNo: String = "ALL") = "student_cards/$rollNo"
    }
    object FeeTracker : Screen("fee_tracker")
    object AttendanceRegister : Screen("attendance_register")
    object CoachingHub : Screen("coaching_hub?tab={tab}") {
        fun createRoute(tab: Int = 0) = "coaching_hub?tab=$tab"
    }
    object DynamicTimetable : Screen("dynamic_timetable?batch={batch}") {
        fun createRoute(batch: String = "ALL") = "dynamic_timetable?batch=$batch"
    }
}
