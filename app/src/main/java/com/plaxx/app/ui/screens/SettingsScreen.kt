package com.plaxx.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.plaxx.app.ui.viewmodels.ScheduleViewModel

@Composable
fun SettingsScreen(viewModel: ScheduleViewModel) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Ustawienia")
    }
}
