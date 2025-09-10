package com.example.timynice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.timynice.room.AppDatabase
import com.example.timynice.ui.theme.TimyniceTheme
import java.time.YearMonth

class MainActivity : ComponentActivity() {

    private lateinit var calendarViewModel: CalendarViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        calendarViewModel = CalendarViewModel(database)

        setContent {
            TimyniceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TimyniceApp(calendarViewModel)
                }
            }
        }
    }
}

@Composable
fun TimyniceApp(calendarViewModel: CalendarViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "calendar") {
        composable("calendar") {
            CalendarScreen(
                calendarViewModel = calendarViewModel,
                onDayClick = { date ->
                    navController.navigate("date/$date")
                }
            )
        }

        composable("date/{date}") { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            DateScreen(
                date = date,
                calendarViewModel = calendarViewModel, // 🔁 pass the viewmodel
                onBackToCalendar = {
                    calendarViewModel.loadMonth(YearMonth.now()) // 🆕 trigger reload
                    navController.popBackStack()
                }
            )
        }
    }
}
