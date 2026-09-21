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
}
