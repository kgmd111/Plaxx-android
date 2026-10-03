package com.plaxx.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.plaxx.app.ui.components.ExpandedTileOverlay
import com.plaxx.app.ui.components.PlaxxBottomBar
import com.plaxx.app.ui.components.PlaxxTopBar
import com.plaxx.app.ui.components.TileMorphData
import com.plaxx.app.ui.components.monthLabel
import com.plaxx.app.ui.components.monthOrder
import com.plaxx.app.ui.screens.AuthScreen
import com.plaxx.app.ui.screens.ColorsScreen
import com.plaxx.app.ui.viewmodels.ColorsViewModel
import com.plaxx.app.ui.screens.DailyScreen
import com.plaxx.app.ui.screens.GroupsScreen
import com.plaxx.app.ui.screens.HomeworkScreen
import com.plaxx.app.ui.screens.SettingsScreen
import com.plaxx.app.ui.screens.SplashScreen
import com.plaxx.app.ui.screens.WeeklyScreen
import com.plaxx.app.ui.viewmodels.CustomEventsViewModel
import com.plaxx.app.ui.viewmodels.HomeworkViewModel
import com.plaxx.app.ui.viewmodels.ScheduleViewModel

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val viewModel: ScheduleViewModel = viewModel()
    val homeworkViewModel: HomeworkViewModel = viewModel()
    val customEventsViewModel: CustomEventsViewModel = viewModel()
    val colorsViewModel: ColorsViewModel = viewModel()

    val state by viewModel.state.collectAsState()

    var bootstrapped by remember { mutableStateOf(false) }
    var calendarExpanded by remember { mutableStateOf(false) }
    var visibleMonthLabel by remember(state.selectedDate) {
        mutableStateOf(monthLabel(state.selectedDate))
    }
    var visibleMonthOrder by remember(state.selectedDate) {
        mutableStateOf(monthOrder(state.selectedDate))
    }

    var expandedTile by remember { mutableStateOf<TileMorphData?>(null) }

    // Bootstrap gate
    if (!bootstrapped) {
        SplashScreen(onReady = { bootstrapped = true })
        return
    }

    // Auth gate
    if (!state.isAuthenticated) {
        AuthScreen(viewModel = viewModel, onLoginSuccess = { })
        return
    }

    // Po pierwszym wejsciu do planu wczytaj HW + custom events + odpal pierwszy sync.
    androidx.compose.runtime.LaunchedEffect(state.planId) {
        if (state.planId != null) {
            homeworkViewModel.load()
            customEventsViewModel.load()
            colorsViewModel.load(state.planId!!)
            // Pierwszy sync po zalogowaniu (delty od currentVersion).
            viewModel.syncNow()
            // Zarejestruj jednorazowy sync w tle z aktualnym planId+since.
            viewModel.scheduleSyncWork(context)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                PlaxxTopBar(
                    monthLabel = visibleMonthLabel,
                    monthOrder = visibleMonthOrder,
                    onSettingsClick = { navController.navigate(Destination.AppSettings.route) },
                    onAccountClick = { },
                    isSyncing = state.syncing,
                    onSyncNow = { viewModel.syncNow() },
                    onExportIcs = {
                        val weekStart = state.selectedDate.with(java.time.DayOfWeek.MONDAY)
                        val eventsForWeek = com.plaxx.app.util.IcsExporter.eventsInWeek(
                            state.events, weekStart
                        )
                        com.plaxx.app.util.IcsExporter.exportAndShare(
                            context, eventsForWeek, "plaxx-week"
                        )
                    }
                )
            },
            bottomBar = { PlaxxBottomBar(navController = navController) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Destination.Daily.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Destination.Daily.route) {
                    DailyScreen(
                        viewModel = viewModel,
                        calendarExpanded = calendarExpanded,
                        onCalendarExpandChange = { calendarExpanded = it },
                        onVisibleMonthChange = { label ->
                            if (label != visibleMonthLabel) {
                                visibleMonthOrder += if (label > visibleMonthLabel) 1 else -1
                                visibleMonthLabel = label
                            }
                        },
                        expandedEventId = expandedTile?.event?.id,
                        onTileExpand = { data -> expandedTile = data },
                        homeworkViewModel = homeworkViewModel,
                        customEventsViewModel = customEventsViewModel,
                        colorsViewModel = colorsViewModel
                    )
                }
                composable(Destination.Weekly.route) {
                    WeeklyScreen(
                        viewModel = viewModel,
                        expandedEventId = expandedTile?.event?.id,
                        onTileExpand = { data -> expandedTile = data },
                        homeworkViewModel = homeworkViewModel,
                        customEventsViewModel = customEventsViewModel,
                        colorsViewModel = colorsViewModel
                    )
                }
                composable(Destination.Groups.route) {
                    GroupsScreen(viewModel = viewModel)
                }
                composable(Destination.Homework.route) {
                    HomeworkScreen(viewModel = homeworkViewModel)
                }
                composable(Destination.Colors.route) {
                    ColorsScreen(
                        scheduleViewModel = viewModel,
                        colorsViewModel = colorsViewModel
                    )
                }
                composable(Destination.AppSettings.route) {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }

        expandedTile?.let { data ->
            ExpandedTileOverlay(
                data = data,
                onDismiss = { expandedTile = null }
            )
        }
    }
}
