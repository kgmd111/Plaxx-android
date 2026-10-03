package com.plaxx.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.plaxx.app.navigation.AppNavigation
import com.plaxx.app.ui.theme.PlaxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlaxTheme {
                AppNavigation()
            }
        }
    }
}
