package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

import android.util.Log
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            System.loadLibrary("opencv_java4")
            Log.d("OpenCV", "OpenCV loaded Successfully via native library")
        } catch (e: Throwable) {
            try {
                if (!OpenCVLoader.initDebug()) {
                    Log.w("OpenCV", "Unable to load OpenCV via OpenCVLoader")
                } else {
                    Log.d("OpenCV", "OpenCV loaded Successfully via OpenCVLoader")
                }
            } catch (t: Throwable) {
                Log.w("OpenCV", "OpenCV initialization failed: ${t.message}")
            }
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val viewModel: OmrViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val exams by viewModel.exams.collectAsStateWithLifecycle()

    val showBottomBar = currentRoute == Screen.Home.route ||
            currentRoute == Screen.Students.route ||
            currentRoute == Screen.CreateExam.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(54.dp)
                        .border(width = 1.dp, color = Color(0xFFF1F5F9))
                ) {
                    // 1. HOME
                    val isHome = currentRoute == Screen.Home.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home",
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        label = {
                            Text(
                                "Home",
                                fontSize = 9.5.sp,
                                fontWeight = if (isHome) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        selected = isHome,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFFFFE4E6),
                            selectedIconColor = Color(0xFFE11D48),
                            selectedTextColor = Color(0xFFE11D48),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B)
                        )
                    )

                    // 2. CREATE EXAM
                    val isCreate = currentRoute == Screen.CreateExam.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (isCreate) Icons.Filled.PostAdd else Icons.Outlined.PostAdd,
                                contentDescription = "New Exam",
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        label = {
                            Text(
                                "New Exam",
                                fontSize = 9.5.sp,
                                fontWeight = if (isCreate) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        selected = isCreate,
                        onClick = {
                            navController.navigate(Screen.CreateExam.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFFFFE4E6),
                            selectedIconColor = Color(0xFFE11D48),
                            selectedTextColor = Color(0xFFE11D48),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B)
                        )
                    )

                    // 3. LIVE SCAN (CENTER HERO ACTION)
                    NavigationBarItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE11D48)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.DocumentScanner,
                                    contentDescription = "Scan",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                "Live Scan",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE11D48)
                            )
                        },
                        selected = false,
                        onClick = {
                            if (exams.isNotEmpty()) {
                                navController.navigate(Screen.ScanOmr.createRoute(exams.first().id))
                            } else {
                                Toast.makeText(context, "Create an exam first to start scanning", Toast.LENGTH_SHORT).show()
                                navController.navigate(Screen.CreateExam.route)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            unselectedTextColor = Color(0xFFE11D48)
                        )
                    )

                    // 4. STUDENTS
                    val isStudents = currentRoute == Screen.Students.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (isStudents) Icons.Filled.People else Icons.Outlined.People,
                                contentDescription = "Students",
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        label = {
                            Text(
                                "Students",
                                fontSize = 9.5.sp,
                                fontWeight = if (isStudents) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        selected = isStudents,
                        onClick = {
                            navController.navigate(Screen.Students.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFFFFE4E6),
                            selectedIconColor = Color(0xFFE11D48),
                            selectedTextColor = Color(0xFFE11D48),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B)
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFFFAFBFD)
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(navController)
            }
            composable(Screen.Home.route) {
                HomeScreen(navController, viewModel)
            }
            composable(Screen.Students.route) {
                StudentsScreen(navController, viewModel)
            }
            composable(Screen.StudentAdmission.route) {
                StudentAdmissionScreen(navController, viewModel)
            }
            composable(Screen.CreateExam.route) {
                CreateExamScreen(navController, viewModel)
            }
            composable(
                route = Screen.ExamDashboard.route,
                arguments = listOf(
                    navArgument("examId") { type = NavType.IntType },
                    navArgument("tab") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { backStackEntry ->
                val examId = backStackEntry.arguments?.getInt("examId") ?: return@composable
                val initialTab = backStackEntry.arguments?.getInt("tab") ?: 0
                ExamDashboardScreen(navController, viewModel, examId, initialTab)
            }
            composable(
                route = Screen.ScanOmr.route,
                arguments = listOf(navArgument("examId") { type = NavType.IntType })
            ) { backStackEntry ->
                val examId = backStackEntry.arguments?.getInt("examId") ?: return@composable
                ScanOmrScreen(navController, viewModel, examId)
            }
            composable(Screen.CustomOmrDesigner.route) {
                CustomOmrDesignerScreen(navController, viewModel)
            }
            composable(Screen.SyncSettings.route) {
                SyncSettingsScreen(navController, viewModel)
            }
            composable(Screen.CoachingControl.route) {
                CoachingControlScreen(navController, viewModel)
            }
            composable(Screen.CoachingSubjects.route) {
                CoachingSubjectsScreen(navController, viewModel)
            }
            composable(
                route = Screen.StudentCards.route,
                arguments = listOf(
                    navArgument("rollNo") {
                        type = NavType.StringType
                        defaultValue = "ALL"
                    }
                )
            ) { backStackEntry ->
                val rollNo = backStackEntry.arguments?.getString("rollNo") ?: "ALL"
                StudentCardsScreen(navController, viewModel, rollNo)
            }
            composable(Screen.FeeTracker.route) {
                FeeTrackerScreen(navController, viewModel)
            }
            composable(Screen.AttendanceRegister.route) {
                AttendanceRegisterScreen(navController, viewModel)
            }
        }
    }
}
