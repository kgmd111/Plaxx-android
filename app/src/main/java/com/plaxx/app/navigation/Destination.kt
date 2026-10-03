package com.plaxx.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Daily : Destination("daily", "Dzien", Icons.Filled.ViewAgenda)
    object Weekly : Destination("weekly", "Tydzien", Icons.Filled.ViewWeek)
    object Groups : Destination("groups", "Grupy", Icons.Filled.Group)
    object Homework : Destination("homework", "Zadania", Icons.Filled.Assignment)
    object Colors : Destination("colors", "Kolory", Icons.Filled.Palette)
    object AppSettings : Destination("settings", "Ustawienia", Icons.Filled.Settings)

    companion object {
        val bottomBarItems = listOf(Daily, Weekly, Groups, Homework, Colors)
    }
}
