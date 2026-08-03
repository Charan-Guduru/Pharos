package com.vnrvjiet.attendancemonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.vnrvjiet.attendancemonitor.data.repository.SettingsRepository
import com.vnrvjiet.attendancemonitor.ui.navigation.AppNavigation
import com.vnrvjiet.attendancemonitor.ui.theme.AttendanceMonitorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val settingsRepository = SettingsRepository.getInstance(applicationContext)
        
        enableEdgeToEdge()
        setContent {
            val themePreference by settingsRepository.theme.collectAsState()
            
            AttendanceMonitorTheme(themePreference = themePreference) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
