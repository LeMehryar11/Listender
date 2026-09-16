package com.example.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.StartOfWeek
import com.example.data.model.TaskEntity
import com.example.data.model.TaskScope
import com.example.data.model.ThemeMode
import com.example.data.repository.TaskRepository
import com.example.domain.date.PeriodCalculator
import com.example.domain.model.PeriodSummary
import com.example.domain.model.TaskWithProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _selectedScope = MutableStateFlow(TaskScope.DAILY)
    val selectedScope: StateFlow<TaskScope> = _selectedScope.asStateFlow()

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    val startOfWeek: StateFlow<StartOfWeek> = repository.getStartOfWeek()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StartOfWeek.MONDAY)

    val themeMode: StateFlow<ThemeMode> = repository.getThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val isAppInitialised: StateFlow<Boolean?> = repository.isAppInitialised()
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val incompleteTaskCounts: StateFlow<Map<TaskScope, Int>> =
        combine(repository.getAllTasks(), startOfWeek) { allTasks, sow ->
            val now = LocalDate.now()
            val result = mutableMapOf<TaskScope, Int>()
            for (scope in TaskScope.entries) {
                val currentPeriodKey = PeriodCalculator.getPeriodKey(scope, now, sow)
                val count = allTasks.count { task ->
                    task.scope == scope.name &&
                        task.periodKey == currentPeriodKey &&
                        task.starsEarned < task.targetStars
                }
                result[scope] = count
            }
            result
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentPeriodKey: StateFlow<String> =
        combine(_selectedScope, _currentDate, startOfWeek) { scope, date, sow ->
            PeriodCalculator.getPeriodKey(scope, date, sow)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodCalculator.formatIsoDate(LocalDate.now()))

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksWithProgress: StateFlow<List<TaskWithProgress>> =
        combine(_selectedScope, _currentDate, startOfWeek) { scope, date, sow ->
            Triple(scope, date, sow)
        }.flatMapLatest { (scope, date, sow) ->
            repository.getTasksWithProgress(scope, date, sow)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val periodSummary: StateFlow<PeriodSummary> =
        combine(_selectedScope, _currentDate, startOfWeek) { scope, date, sow ->
            Triple(scope, date, sow)
        }.flatMapLatest { (scope, date, sow) ->
            repository.getPeriodSummary(scope, date, sow)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PeriodSummary(
                scope = TaskScope.DAILY,
                periodKey = PeriodCalculator.formatIsoDate(LocalDate.now()),
                displayLabel = "Today",
                totalTasks = 0,
                completedTasks = 0,
                totalTargetStars = 0,
                totalEarnedStars = 0,
                completionPercentage = 0
            )
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val hasImported: StateFlow<Boolean> =
        combine(_selectedScope, _currentDate, startOfWeek) { scope, date, sow ->
            PeriodCalculator.getPeriodKey(scope, date, sow)
        }.flatMapLatest { periodKey ->
            repository.hasImportedForPeriod(periodKey)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val previousPeriodTasks: StateFlow<List<TaskEntity>> =
        combine(_selectedScope, _currentDate, startOfWeek) { scope, date, sow ->
            Triple(scope, date, sow)
        }.flatMapLatest { (scope, date, sow) ->
            repository.getPreviousPeriodTasks(scope, date, sow)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentNote: StateFlow<String> =
        combine(_selectedScope, _currentDate, startOfWeek) { scope, date, sow ->
            PeriodCalculator.getPeriodKey(scope, date, sow)
        }.flatMapLatest { periodKey ->
            repository.getNote(periodKey)
        }.combine(_selectedScope) { noteEntity, _ ->
            noteEntity?.content ?: ""
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun selectScope(scope: TaskScope) {
        _selectedScope.value = scope
    }

    fun shiftPeriod(delta: Int) {
        _currentDate.value = PeriodCalculator.shiftPeriod(_selectedScope.value, _currentDate.value, delta)
    }

    fun resetToCurrentDate() {
        _currentDate.value = LocalDate.now()
    }

    fun importFromPreviousPeriod() {
        viewModelScope.launch {
            repository.importFromPreviousPeriod(
                scope = _selectedScope.value,
                date = _currentDate.value,
                startOfWeek = startOfWeek.value
            )
        }
    }

    fun importSelectedTasks(tasks: List<TaskEntity>) {
        viewModelScope.launch {
            val targetPeriodKey = PeriodCalculator.getPeriodKey(_selectedScope.value, _currentDate.value, startOfWeek.value)
            repository.importSelectedTasks(tasks, targetPeriodKey, _selectedScope.value)
        }
    }

    fun moveTaskToNextPeriod(task: TaskEntity) {
        viewModelScope.launch {
            val nextPeriodKey = PeriodCalculator.getNextPeriodKey(_selectedScope.value, _currentDate.value, startOfWeek.value)
            repository.moveTaskToNextPeriod(task.id, nextPeriodKey)
        }
    }

    fun moveTaskToScope(task: TaskEntity, targetScope: TaskScope) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val targetPeriodKey = PeriodCalculator.getPeriodKey(targetScope, today, startOfWeek.value)
            repository.moveTask(task.id, targetPeriodKey, targetScope)
        }
    }

    fun moveTaskToCurrentPeriod(task: TaskEntity) {
        moveTaskToScope(task, _selectedScope.value)
    }

    fun moveTaskToToday(task: TaskEntity) {
        moveTaskToScope(task, TaskScope.DAILY)
    }

    fun incrementStars(taskId: Long) {
        viewModelScope.launch {
            repository.incrementStars(taskId)
        }
    }

    fun decrementStars(taskId: Long) {
        viewModelScope.launch {
            repository.decrementStars(taskId)
        }
    }

    fun setStars(taskId: Long, starCount: Int) {
        viewModelScope.launch {
            repository.setStars(taskId, starCount)
        }
    }

    fun saveTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.saveTask(task)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun moveTaskUp(currentIndex: Int) {
        val currentList = tasksWithProgress.value.map { it.task }.toMutableList()
        if (currentIndex > 0 && currentIndex < currentList.size) {
            val item = currentList.removeAt(currentIndex)
            currentList.add(currentIndex - 1, item)
            viewModelScope.launch {
                repository.reorderTasks(currentList)
            }
        }
    }

    fun moveTaskDown(currentIndex: Int) {
        val currentList = tasksWithProgress.value.map { it.task }.toMutableList()
        if (currentIndex >= 0 && currentIndex < currentList.size - 1) {
            val item = currentList.removeAt(currentIndex)
            currentList.add(currentIndex + 1, item)
            viewModelScope.launch {
                repository.reorderTasks(currentList)
            }
        }
    }

    fun reorderTasks(reorderedList: List<TaskEntity>) {
        viewModelScope.launch {
            repository.reorderTasks(reorderedList)
        }
    }

    fun saveNote(content: String) {
        viewModelScope.launch {
            val periodKey = PeriodCalculator.getPeriodKey(_selectedScope.value, _currentDate.value, startOfWeek.value)
            repository.saveNote(periodKey, _selectedScope.value, content)
        }
    }

    fun setStartOfWeek(newStartOfWeek: StartOfWeek) {
        viewModelScope.launch {
            repository.setStartOfWeek(newStartOfWeek)
        }
    }

    fun setThemeMode(newMode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(newMode)
        }
    }

    fun initialiseDefaults(startOfWeek: StartOfWeek) {
        viewModelScope.launch {
            repository.initialiseDefaultData(startOfWeek)
        }
    }

    fun markInitialised() {
        viewModelScope.launch {
            repository.markInitialised()
        }
    }
}

class MainViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
