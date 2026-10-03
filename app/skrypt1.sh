#!/usr/bin/env bash
set -e

ROOT="$(pwd)"
SRC="$ROOT/app/src/main/java/com/plaxx/app"

# ═══════════════════════════════════════════════════════════════
# 1) PlaxxTopBar: wskaznik sync + poprawka ikony
# ═══════════════════════════════════════════════════════════════
python3 - << 'PY_EOF'
path = "app/src/main/java/com/plaxx/app/ui/components/PlaxxTopBar.kt"
with open(path, "r", encoding="utf-8") as f:
    src = f.read()

# 1a) dodaj import CircularProgressIndicator i Refresh
imports_to_add = [
    "import androidx.compose.material.icons.filled.Refresh",
    "import androidx.compose.material3.CircularProgressIndicator",
]
for imp in imports_to_add:
    if imp not in src:
        src = src.replace(
            "import androidx.compose.material.icons.filled.Settings",
            imp + "\nimport androidx.compose.material.icons.filled.Settings",
            1
        )

# 1b) import size/height dla wskaznika
if 'import androidx.compose.foundation.layout.size' not in src:
    src = src.replace(
        "import androidx.compose.foundation.layout.width",
        "import androidx.compose.foundation.layout.size\nimport androidx.compose.foundation.layout.width",
        1
    )

# 1c) parametry - dodaj isSyncing
old_sig = '''fun PlaxxTopBar(
    monthLabel: String = "",
    monthOrder: Int = 0,
    onSettingsClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onExportIcs: (() -> Unit)? = null,
    onSyncNow: (() -> Unit)? = null,
) {'''
new_sig = '''fun PlaxxTopBar(
    monthLabel: String = "",
    monthOrder: Int = 0,
    onSettingsClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onExportIcs: (() -> Unit)? = null,
    onSyncNow: (() -> Unit)? = null,
    isSyncing: Boolean = false,
) {'''
if 'isSyncing: Boolean = false' not in src:
    src = src.replace(old_sig, new_sig, 1)

# 1d) wskaznik przed actions + poprawka ikony Refresh w menu
old_actions = '''        actions = {
            if (onExportIcs != null || onSyncNow != null) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Wiecej")
                }'''
new_actions = '''        actions = {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp).padding(end = 8.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (onExportIcs != null || onSyncNow != null) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Wiecej")
                }'''
if 'if (isSyncing) {' not in src:
    src = src.replace(old_actions, new_actions, 1)

# 1e) ikona Sync w menu -> Refresh
old_icon = '''                            leadingIcon = {
                                Icon(Icons.Filled.Settings, contentDescription = null)
                            }'''
new_icon = '''                            leadingIcon = {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                            }'''
if 'Icons.Filled.Refresh, contentDescription = null' not in src:
    src = src.replace(old_icon, new_icon, 1)

with open(path, "w", encoding="utf-8") as f:
    f.write(src)
print("OK: PlaxxTopBar sync indicator + icon")
PY_EOF

# ═══════════════════════════════════════════════════════════════
# 2) DailyScreen: PullToRefreshBox + empty state
# ═══════════════════════════════════════════════════════════════
python3 - << 'PY_EOF'
path = "app/src/main/java/com/plaxx/app/ui/screens/DailyScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    src = f.read()

# 2a) importy
imports_to_add = [
    "import androidx.compose.material3.ExperimentalMaterial3Api",
    "import androidx.compose.material3.pulltorefresh.PullToRefreshBox",
]
for imp in imports_to_add:
    if imp not in src:
        src = src.replace(
            "import androidx.compose.material3.HorizontalDivider",
            imp + "\nimport androidx.compose.material3.HorizontalDivider",
            1
        )

# 2b) OptIn na DailyScreen
if '@OptIn(ExperimentalMaterial3Api::class)' not in src:
    src = src.replace(
        '@Composable\nfun DailyScreen(',
        '@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun DailyScreen(',
        1
    )

# 2c) Zastap wywolanie DominoPager przez PullToRefreshBox
old_pager = '''        DominoPager(
            currentDate = state.selectedDate,
            events = allEvents,
            onDateChange = { viewModel.selectDate(it) },
            onPlanTouched = { if (calendarExpanded) onCalendarExpandChange(false) },
            expandedEventId = expandedEventId,
            onTileExpand = onTileExpand,
            homeworkCounts = homeworkViewModel
                ?.items?.collectAsState()?.value
                ?.let { list ->
                    list.filter { it.planEventId != null && it.status != "done" }
                        .groupingBy { it.planEventId!! }
                        .eachCount()
                } ?: emptyMap(),
            colorsMap = colorsViewModel?.colors?.collectAsState()?.value ?: emptyMap(),
            modifier = Modifier.fillMaxSize()
        )'''
new_pager = '''        PullToRefreshBox(
            isRefreshing = state.syncing,
            onRefresh = { viewModel.syncNow() },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                DominoPager(
                    currentDate = state.selectedDate,
                    events = allEvents,
                    onDateChange = { viewModel.selectDate(it) },
                    onPlanTouched = { if (calendarExpanded) onCalendarExpandChange(false) },
                    expandedEventId = expandedEventId,
                    onTileExpand = onTileExpand,
                    homeworkCounts = homeworkViewModel
                        ?.items?.collectAsState()?.value
                        ?.let { list ->
                            list.filter { it.planEventId != null && it.status != "done" }
                                .groupingBy { it.planEventId!! }
                                .eachCount()
                        } ?: emptyMap(),
                    colorsMap = colorsViewModel?.colors?.collectAsState()?.value ?: emptyMap(),
                    modifier = Modifier.fillMaxSize()
                )

                // Empty state — pokaz tylko gdy brak eventow w wybranym dniu
                val hasEventsForDay = allEvents.any { ev ->
                    runCatching {
                        LocalDateTime.parse(ev.start).toLocalDate()
                    }.getOrNull() == state.selectedDate
                }
                if (!hasEventsForDay) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(top = 120.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            "Brak zajec tego dnia",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }'''
if 'PullToRefreshBox(' not in src:
    src = src.replace(old_pager, new_pager, 1)
    print("OK: DailyScreen PullToRefreshBox")
else:
    print("SKIP: DailyScreen juz ma PullToRefreshBox")

with open(path, "w", encoding="utf-8") as f:
    f.write(src)
PY_EOF

# ═══════════════════════════════════════════════════════════════
# 3) AppNavigation: przekaz isSyncing do TopBar
# ═══════════════════════════════════════════════════════════════
python3 - << 'PY_EOF'
path = "app/src/main/java/com/plaxx/app/navigation/AppNavigation.kt"
with open(path, "r", encoding="utf-8") as f:
    src = f.read()

old_topbar = '''                    onSyncNow = { viewModel.syncNow() },'''
new_topbar = '''                    isSyncing = state.syncing,
                    onSyncNow = { viewModel.syncNow() },'''
if 'isSyncing = state.syncing' not in src:
    src = src.replace(old_topbar, new_topbar, 1)
    print("OK: AppNavigation isSyncing")
else:
    print("SKIP: AppNavigation juz ma isSyncing")

with open(path, "w", encoding="utf-8") as f:
    f.write(src)
PY_EOF

echo "OK: Script 16 - pull-to-refresh + polish"
echo ""
echo "Zmiany:"
echo "  ui/components/PlaxxTopBar.kt   (isSyncing + ikona Refresh)"
echo "  ui/screens/DailyScreen.kt      (PullToRefreshBox + empty state)"
echo "  navigation/AppNavigation.kt    (isSyncing = state.syncing)"
