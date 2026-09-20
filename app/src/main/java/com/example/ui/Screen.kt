package com.example.ui

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Students : Screen("students")
    object StudentAdmission : Screen("student_admission")
    object CreateExam : Screen("create_exam")
    object ExamDashboard : Screen("exam_dashboard/{examId}") {
        fun createRoute(examId: Int) = "exam_dashboard/$examId"
    }
    object ScanOmr : Screen("scan_omr/{examId}") {
        fun createRoute(examId: Int) = "scan_omr/$examId"
    }
}
