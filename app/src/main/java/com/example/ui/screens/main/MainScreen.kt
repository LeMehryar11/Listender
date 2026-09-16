package com.example.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TaskEntity
import com.example.data.model.TaskScope
import com.example.domain.date.PeriodCalculator
import com.example.ui.components.ImportTasksDialog
import com.example.ui.components.MoveTaskOption
import com.example.ui.components.OnboardingDialog
import com.example.ui.components.PeriodNavigator
import com.example.ui.components.PeriodPerformanceBanner
import com.example.ui.components.ScopedNotesSection
import com.example.ui.components.TaskEditDialog
import com.example.ui.components.TaskItemCard
import com.example.ui.screens.reports.ReportsViewModel
import com.example.ui.theme.BrandCrimson

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    reportsViewModel: ReportsViewModel,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    showBottomNav: Boolean = true,
    modifier: Modifier = Modifier
) {
    val selectedScope by mainViewModel.selectedScope.collectAsStateWithLifecycle()
    val currentDate by mainViewModel.currentDate.collectAsStateWithLifecycle()
    val startOfWeek by mainViewModel.startOfWeek.collectAsStateWithLifecycle()
    val tasksWithProgress by mainViewModel.tasksWithProgress.collectAsStateWithLifecycle()
    val periodSummary by mainViewModel.periodSummary.collectAsStateWithLifecycle()
    val currentNote by mainViewModel.currentNote.collectAsStateWithLifecycle()
    val isInitialised by mainViewModel.isAppInitialised.collectAsStateWithLifecycle()
    val currentPeriodKey by mainViewModel.currentPeriodKey.collectAsStateWithLifecycle()
    val previousPeriodTasks by mainViewModel.previousPeriodTasks.collectAsStateWithLifecycle()
    val incompleteCounts by mainViewModel.incompleteTaskCounts.collectAsStateWithLifecycle()

    val isCurrentPeriod = remember(selectedScope, currentDate, startOfWeek) {
        PeriodCalculator.isCurrentPeriod(selectedScope, currentDate, startOfWeek)
    }

    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var showTaskEditDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    // First run initialisation modal
    if (isInitialised == false) {
        OnboardingDialog(
            onInitialiseDefaults = { sow ->
                mainViewModel.initialiseDefaults(sow)
            },
            onStartBlank = {
                mainViewModel.markInitialised()
            }
        )
    }

    if (showTaskEditDialog) {
        TaskEditDialog(
            initialTask = taskToEdit,
            defaultScope = selectedScope,
            currentPeriodKey = currentPeriodKey,
            onDismiss = {
                showTaskEditDialog = false
                taskToEdit = null
            },
            onSave = { task ->
                mainViewModel.saveTask(task)
                showTaskEditDialog = false
                taskToEdit = null
            }
        )
    }

    // Determine importable tasks (tasks in previous period not already present in current period)
    val existingTitles = remember(tasksWithProgress) {
        tasksWithProgress.map { it.task.title.trim().lowercase() }.toSet()
    }
    val importableTasks = remember(previousPeriodTasks, existingTitles) {
        previousPeriodTasks.filter { !existingTitles.contains(it.title.trim().lowercase()) }
    }

    if (showImportDialog && importableTasks.isNotEmpty()) {
        val relativeLabel = when (selectedScope) {
            TaskScope.DAILY -> "yesterday"
            TaskScope.WEEKLY -> "last week"
            TaskScope.MONTHLY -> "last month"
            TaskScope.YEARLY -> "last year"
        }
        ImportTasksDialog(
            tasks = importableTasks,
            relativePeriodLabel = relativeLabel,
            onDismiss = { showImportDialog = false },
            onConfirmImport = { selected ->
                mainViewModel.importSelectedTasks(selected)
                showImportDialog = false
            }
        )
    }

    // Hold-and-drag state for reordering tasks
    var localTasks by remember(tasksWithProgress) { mutableStateOf(tasksWithProgress) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val itemHeightPx = remember(density) { with(density) { 92.dp.toPx() } }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                AppBottomNav(
                    currentTab = NavTab.TASKS,
                    onTasksClick = { /* Already on Tasks */ },
                    onReportsClick = onNavigateToReports,
                    onSettingsClick = onNavigateToSettings
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    taskToEdit = null
                    showTaskEditDialog = true
                },
                containerColor = BrandCrimson,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("btn_add_task")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.testTag("main_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Period & Scope Tabs Navigator with incomplete counts badges
            Spacer(modifier = Modifier.height(6.dp))
            PeriodNavigator(
                selectedScope = selectedScope,
                onScopeSelected = { mainViewModel.selectScope(it) },
                periodDisplayLabel = PeriodCalculator.getPeriodDisplayLabel(selectedScope, currentDate, startOfWeek),
                onPreviousPeriod = { mainViewModel.shiftPeriod(-1) },
                onNextPeriod = { mainViewModel.shiftPeriod(1) },
                onResetToCurrent = { mainViewModel.resetToCurrentDate() },
                isCurrentPeriod = PeriodCalculator.isCurrentPeriod(selectedScope, currentDate, startOfWeek),
                incompleteCounts = incompleteCounts
            )

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    PeriodPerformanceBanner(summary = periodSummary)
                }

                if (localTasks.isEmpty()) {
                    if (importableTasks.isNotEmpty()) {
                        item {
                            val importLabel = when (selectedScope) {
                                TaskScope.DAILY -> "Import from yesterday"
                                TaskScope.WEEKLY -> "Import from last week"
                                TaskScope.MONTHLY -> "Import from last month"
                                TaskScope.YEARLY -> "Import from last year"
                            }
                            val countSuffix = " (${importableTasks.size} ${if (importableTasks.size == 1) "task" else "tasks"})"

                            OutlinedButton(
                                onClick = { showImportDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_import_previous_period")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = importLabel,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(end = 6.dp)
                                )
                                Text(
                                    text = "$importLabel$countSuffix",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(vertical = 32.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val emptyMessage = when (selectedScope) {
                                    TaskScope.DAILY -> "No tasks for today yet"
                                    TaskScope.WEEKLY -> "No tasks for this week yet"
                                    TaskScope.MONTHLY -> "No tasks for this month yet"
                                    TaskScope.YEARLY -> "No tasks for this year yet"
                                }
                                Text(
                                    text = emptyMessage,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap '+' below to add.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = localTasks,
                        key = { _, item -> "${item.task.id}_${item.periodKey}" }
                    ) { index, item ->
                        val isBeingDragged = draggedIndex == index
                        val dragModifier = if (isBeingDragged) {
                            Modifier
                                .zIndex(2f)
                                .graphicsLayer {
                                    translationY = dragOffsetY
                                    shadowElevation = with(density) { 8.dp.toPx() }
                                }
                        } else {
                            Modifier.zIndex(1f)
                        }

                        val moveOptions = remember(selectedScope, isCurrentPeriod, item.task.id) {
                            val options = mutableListOf<MoveTaskOption>()
                            TaskScope.entries.forEach { targetScope ->
                                val isAlreadyHere = targetScope == selectedScope && isCurrentPeriod
                                if (!isAlreadyHere) {
                                    val label = when (targetScope) {
                                        TaskScope.DAILY -> "Move to today"
                                        TaskScope.WEEKLY -> "Move to the current week"
                                        TaskScope.MONTHLY -> "Move to the current month"
                                        TaskScope.YEARLY -> "Move to the current year"
                                    }
                                    val tag = when (targetScope) {
                                        TaskScope.DAILY -> "menu_move_today_${item.task.id}"
                                        TaskScope.WEEKLY -> "menu_move_weekly_${item.task.id}"
                                        TaskScope.MONTHLY -> "menu_move_monthly_${item.task.id}"
                                        TaskScope.YEARLY -> "menu_move_yearly_${item.task.id}"
                                    }
                                    options.add(
                                        MoveTaskOption(
                                            label = label,
                                            testTag = tag,
                                            onClick = { mainViewModel.moveTaskToScope(item.task, targetScope) }
                                        )
                                    )
                                }
                            }
                            options
                        }

                        TaskItemCard(
                            taskWithProgress = item,
                            onIncrementStars = { mainViewModel.incrementStars(item.task.id) },
                            onDecrementStars = { mainViewModel.decrementStars(item.task.id) },
                            onEdit = {
                                taskToEdit = item.task
                                showTaskEditDialog = true
                            },
                            onDelete = { mainViewModel.deleteTask(item.task.id) },
                            onMoveToNextPeriod = { mainViewModel.moveTaskToNextPeriod(item.task) },
                            nextPeriodLabel = PeriodCalculator.getNextPeriodActionLabel(selectedScope),
                            moveOptions = moveOptions,
                            modifier = dragModifier
                                .pointerInput(localTasks) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val cur = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                            if (dragOffsetY > itemHeightPx && cur < localTasks.size - 1) {
                                                val mutable = localTasks.toMutableList()
                                                val moved = mutable.removeAt(cur)
                                                mutable.add(cur + 1, moved)
                                                localTasks = mutable
                                                draggedIndex = cur + 1
                                                dragOffsetY -= itemHeightPx
                                            } else if (dragOffsetY < -itemHeightPx && cur > 0) {
                                                val mutable = localTasks.toMutableList()
                                                val moved = mutable.removeAt(cur)
                                                mutable.add(cur - 1, moved)
                                                localTasks = mutable
                                                draggedIndex = cur - 1
                                                dragOffsetY += itemHeightPx
                                            }
                                        },
                                        onDragEnd = {
                                            mainViewModel.reorderTasks(localTasks.map { it.task })
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            mainViewModel.reorderTasks(localTasks.map { it.task })
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                        }
                                    )
                                }
                        )
                    }

                    // Import tasks button appears at the bottom of the tasks list if importable tasks remain
                    if (importableTasks.isNotEmpty()) {
                        item {
                            val importLabel = when (selectedScope) {
                                TaskScope.DAILY -> "Import from yesterday"
                                TaskScope.WEEKLY -> "Import from last week"
                                TaskScope.MONTHLY -> "Import from last month"
                                TaskScope.YEARLY -> "Import from last year"
                            }
                            val countSuffix = " (${importableTasks.size} ${if (importableTasks.size == 1) "task" else "tasks"})"

                            OutlinedButton(
                                onClick = { showImportDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("btn_import_previous_period")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = importLabel,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(end = 6.dp)
                                )
                                Text(
                                    text = "$importLabel$countSuffix",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Scoped Notes Section
                item {
                    ScopedNotesSection(
                        scope = selectedScope,
                        noteContent = currentNote,
                        onNoteChange = { mainViewModel.saveNote(it) }
                    )
                    Spacer(modifier = Modifier.height(72.dp)) // Leave room for floating action button
                }
            }
        }
    }
}
