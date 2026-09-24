package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.securemate.SecureMateApplication
import com.example.securemate.ui.navigation.SecureMateApp
import com.example.securemate.ui.theme.SecureMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SecureMateApplication
        val container = app.container

        setContent {
            val themeMode by container.securityPreferences.themeMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            SecureMateTheme(darkTheme = isDark) {
                SecureMateApp(container = container)
            }
        }
    }
}

