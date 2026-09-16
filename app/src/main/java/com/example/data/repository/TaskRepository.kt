package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.NoteEntity
import com.example.data.model.PreferenceEntity
import com.example.data.model.StartOfWeek
import com.example.data.model.TaskEntity
import com.example.data.model.TaskLogEntity
import com.example.data.model.TaskScope
import com.example.data.model.ThemeMode
import com.example.domain.date.PeriodCalculator
import com.example.domain.model.PeriodSummary
import com.example.domain.model.TaskWithProgress
import com.example.export.ParsedBackupData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

enum class CollisionStrategy(val displayName: String) {
    MERGE_MAX_STARS("Merge Progress (Keep Highest Stars)"),
    SKIP_EXISTING("Skip Existing (Only Add New)"),
    OVERWRITE("Overwrite Existing Tasks")
}

data class ImportResult(
    val totalProcessed: Int,
    val tasksAdded: Int,
    val tasksUpdated: Int,
    val notesImported: Int,
    val errorMessage: String? = null
)

interface TaskRepository {
    fun getAllTasks(): Flow<List<TaskEntity>>
    fun getTasksForPeriod(periodKey: String): Flow<List<TaskEntity>>
    fun getTasksWithProgress(
        scope: TaskScope,
        date: LocalDate,
        startOfWeek: StartOfWeek
    ): Flow<List<TaskWithProgress>>
    fun getPeriodSummary(
        scope: TaskScope,
        date: LocalDate,
        startOfWeek: StartOfWeek
    ): Flow<PeriodSummary>
    fun getPreviousPeriodTasks(
        scope: TaskScope,
        date: LocalDate,
        startOfWeek: StartOfWeek
    ): Flow<List<TaskEntity>>
    fun hasImportedForPeriod(periodKey: String): Flow<Boolean>
    suspend fun importFromPreviousPeriod(
        scope: TaskScope,
        date: LocalDate,
        startOfWeek: StartOfWeek
    ): Int
    suspend fun importSelectedTasks(
        tasks: List<TaskEntity>,
        targetPeriodKey: String,
        scope: TaskScope
    ): Int
    suspend fun moveTaskToNextPeriod(taskId: Long, nextPeriodKey: String)
    suspend fun moveTask(taskId: Long, targetPeriodKey: String, targetScope: TaskScope? = null)
    suspend fun incrementStars(taskId: Long)
    suspend fun decrementStars(taskId: Long)
    suspend fun setStars(taskId: Long, stars: Int)
    suspend fun saveTask(task: TaskEntity): Long
    suspend fun deleteTask(taskId: Long)
    suspend fun reorderTasks(reorderedTasks: List<TaskEntity>)
    fun getNote(periodKey: String): Flow<NoteEntity?>
    suspend fun saveNote(periodKey: String, scope: TaskScope, content: String)
    fun getStartOfWeek(): Flow<StartOfWeek>
    suspend fun setStartOfWeek(startOfWeek: StartOfWeek)
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(themeMode: ThemeMode)
    fun isAppInitialised(): Flow<Boolean>
    suspend fun initialiseDefaultData(startOfWeek: StartOfWeek = StartOfWeek.MONDAY)
    suspend fun seedDefaultsIfEmpty()
    suspend fun markInitialised()
    fun getAllNotes(): Flow<List<NoteEntity>>
    suspend fun importBackupData(
        data: ParsedBackupData,
        collisionStrategy: CollisionStrategy = CollisionStrategy.MERGE_MAX_STARS
    ): ImportResult
}

class TaskRepositoryImpl(
    private val database: AppDatabase
) : TaskRepository {
    private val taskDao = database.taskDao()
    private val noteDao = database.noteDao()
    private val preferenceDao = database.preferenceDao()

    override fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    override fun getTasksForPeriod(periodKey: String): Flow<List<TaskEntity>> =
        taskDao.getTasksByPeriod(periodKey)

    override fun getTasksWithProgress(
        scope: TaskScope,
        date: LocalDate,
        startOfWeek: StartOfWeek
    ): Flow<List<TaskWithProgress>> {
        val periodKey = PeriodCalculator.getPeriodKey(scope, date, startOfWeek)
        return taskDao.getTasksByPeriod(periodKey).map { tasks ->
            tasks.map { task ->
                TaskWithProgress(
                    task = task,
                    starsEarned = task.starsEarned,
                    periodKey = periodKey
                )
            }
        }
    }

    override fun getPeriodSummary(
        scope: TaskScope,
        date: LocalDate,
        startOfWeek: StartOfWeek
    ): Flow<PeriodSummary> {
        return taskDao.getAllTasks().map { allTasks ->
            val aggregatedTasks = PeriodCalculator.getTasksForPeriodAggregated(allTasks, scope, date, startOfWeek)
            PeriodCalculator.calculatePeriodSummary(aggregatedTasks, scope, date, startOfWeek)
        }
    }

    override fun getPreviousPeriodTasks(
        scope: TaskScope,
        date: LocalDate,
        startOfWeek: StartOfWeek
    ): Flow<List<TaskEntity>> {
        val currentPeriodKey = PeriodCalculator.getPeriodKey(scope, date, startOfWeek)
        val previousPeriodKey = PeriodCalculator.getPreviousPeriodKey(scope, date, startOfWeek)
        return combine(
            taskDao.getTasksByPeriod(currentPeriodKey),
            taskDao.getTasksByPeriod(previousPeriodKey)
        ) { currentTasks, previousTasks ->
            val currentTitles = currentTasks.map { it.title.trim().lowercase() }.toSet()
            previousTasks.filter { !currentTitles.contains(it.title.trim().lowercase()) }
        }
    }

    override fun hasImportedForPeriod(periodKey: String): Flow<Boolean> {
        return preferenceDao.getPreference("imported_$periodKey").map { pref ->
            pref?.value == "true"
        }
    }

    override suspend fun importFromPreviousPeriod(
        scope: TaskScope,
        date: LocalDate,
        startOfWeek: StartOfWeek
    ): Int {
        val currentPeriodKey = PeriodCalculator.getPeriodKey(scope, date, startOfWeek)
        val previousPeriodKey = PeriodCalculator.getPreviousPeriodKey(scope, date, startOfWeek)

        val previousTasks = taskDao.getTasksByPeriodSync(previousPeriodKey)
        if (previousTasks.isEmpty()) return 0

        val currentTasks = taskDao.getTasksByPeriodSync(currentPeriodKey)
        val currentTitles = currentTasks.map { it.title.trim().lowercase() }.toSet()
        val startOrderIndex = (currentTasks.maxOfOrNull { it.orderIndex } ?: -1) + 1

        val tasksToImport = previousTasks
            .filter { !currentTitles.contains(it.title.trim().lowercase()) }
            .mapIndexed { index, task ->
                TaskEntity(
                    id = 0L,
                    title = task.title,
                    scope = scope.name,
                    periodKey = currentPeriodKey,
                    targetStars = task.targetStars,
                    starsEarned = 0,
                    orderIndex = startOrderIndex + index,
                    createdAt = System.currentTimeMillis()
                )
            }

        if (tasksToImport.isNotEmpty()) {
            taskDao.insertTasks(tasksToImport)
        }

        // Mark as imported so the import button for this period disappears
        preferenceDao.setPreference(
            PreferenceEntity(key = "imported_$currentPeriodKey", value = "true")
        )

        return tasksToImport.size
    }

    override suspend fun incrementStars(taskId: Long) {
        val task = taskDao.getTaskById(taskId) ?: return
        if (task.starsEarned < task.targetStars) {
            taskDao.updateStarsEarned(taskId, task.starsEarned + 1)
        }
    }

    override suspend fun decrementStars(taskId: Long) {
        val task = taskDao.getTaskById(taskId) ?: return
        if (task.starsEarned > 0) {
            taskDao.updateStarsEarned(taskId, task.starsEarned - 1)
        }
    }

    override suspend fun setStars(taskId: Long, stars: Int) {
        val task = taskDao.getTaskById(taskId) ?: return
        taskDao.updateStarsEarned(taskId, stars.coerceIn(0, task.targetStars))
    }

    override suspend fun importSelectedTasks(
        tasks: List<TaskEntity>,
        targetPeriodKey: String,
        scope: TaskScope
    ): Int {
        if (tasks.isEmpty()) return 0
        val currentTasks = taskDao.getTasksByPeriodSync(targetPeriodKey)
        val currentTitles = currentTasks.map { it.title.trim().lowercase() }.toSet()
        val startOrderIndex = (currentTasks.maxOfOrNull { it.orderIndex } ?: -1) + 1

        val tasksToInsert = tasks
            .filter { !currentTitles.contains(it.title.trim().lowercase()) }
            .mapIndexed { index, task ->
                TaskEntity(
                    id = 0L,
                    title = task.title,
                    scope = scope.name,
                    periodKey = targetPeriodKey,
                    targetStars = task.targetStars,
                    starsEarned = 0,
                    orderIndex = startOrderIndex + index,
                    createdAt = System.currentTimeMillis()
                )
            }

        if (tasksToInsert.isNotEmpty()) {
            taskDao.insertTasks(tasksToInsert)
        }
        return tasksToInsert.size
    }

    override suspend fun moveTaskToNextPeriod(taskId: Long, nextPeriodKey: String) {
        moveTask(taskId, nextPeriodKey, null)
    }

    override suspend fun moveTask(taskId: Long, targetPeriodKey: String, targetScope: TaskScope?) {
        val task = taskDao.getTaskById(taskId) ?: return
        val currentTasks = taskDao.getTasksByPeriodSync(targetPeriodKey)
        val minOrderIndex = currentTasks.minOfOrNull { it.orderIndex } ?: 0
        taskDao.updateTask(
            task.copy(
                periodKey = targetPeriodKey,
                scope = targetScope?.name ?: task.scope,
                starsEarned = 0,
                orderIndex = minOrderIndex - 1
            )
        )
    }

    override suspend fun saveTask(task: TaskEntity): Long {
        return if (task.id == 0L) {
            val currentTasks = taskDao.getTasksByPeriodSync(task.periodKey)
            val minOrderIndex = currentTasks.minOfOrNull { it.orderIndex } ?: 0
            val newTask = task.copy(orderIndex = minOrderIndex - 1)
            taskDao.insertTask(newTask)
        } else {
            taskDao.updateTask(task)
            task.id
        }
    }

    override suspend fun deleteTask(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }

    override suspend fun reorderTasks(reorderedTasks: List<TaskEntity>) {
        reorderedTasks.forEachIndexed { index, task ->
            taskDao.updateOrderIndex(task.id, index)
        }
    }

    override fun getNote(periodKey: String): Flow<NoteEntity?> {
        return noteDao.getNoteForPeriod(periodKey)
    }

    override suspend fun saveNote(periodKey: String, scope: TaskScope, content: String) {
        if (content.isBlank()) {
            noteDao.deleteNote(periodKey)
        } else {
            val note = NoteEntity(
                periodKey = periodKey,
                scope = scope.name,
                content = content.trim(),
                updatedAt = System.currentTimeMillis()
            )
            noteDao.insertOrUpdateNote(note)
        }
    }

    override fun getStartOfWeek(): Flow<StartOfWeek> {
        return preferenceDao.getPreference(KEY_START_OF_WEEK).map { pref ->
            StartOfWeek.fromString(pref?.value)
        }
    }

    override suspend fun setStartOfWeek(startOfWeek: StartOfWeek) {
        preferenceDao.setPreference(
            PreferenceEntity(key = KEY_START_OF_WEEK, value = startOfWeek.name)
        )
    }

    override fun getThemeMode(): Flow<ThemeMode> {
        return preferenceDao.getPreference(KEY_THEME_MODE).map { pref ->
            ThemeMode.fromString(pref?.value)
        }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        preferenceDao.setPreference(
            PreferenceEntity(key = KEY_THEME_MODE, value = themeMode.name)
        )
    }

    override fun isAppInitialised(): Flow<Boolean> {
        return preferenceDao.getPreference(KEY_IS_INITIALISED).map { pref ->
            pref?.value == "true"
        }
    }

    override suspend fun markInitialised() {
        preferenceDao.setPreference(
            PreferenceEntity(key = KEY_IS_INITIALISED, value = "true")
        )
    }

    override suspend fun seedDefaultsIfEmpty() {
        val isInit = preferenceDao.getPreferenceSync(KEY_IS_INITIALISED)
        if (isInit == null || isInit.value != "true") {
            initialiseDefaultData(StartOfWeek.MONDAY)
        }
    }

    override suspend fun initialiseDefaultData(startOfWeek: StartOfWeek) {
        val now = LocalDate.now()
        val todayKey = PeriodCalculator.getPeriodKey(TaskScope.DAILY, now, startOfWeek)
        val weekKey = PeriodCalculator.getPeriodKey(TaskScope.WEEKLY, now, startOfWeek)
        val monthKey = PeriodCalculator.getPeriodKey(TaskScope.MONTHLY, now, startOfWeek)
        
        val defaultTasks = listOf(
            TaskEntity(
                title = "Water",
                scope = TaskScope.DAILY.name,
                periodKey = todayKey,
                targetStars = 7,
                starsEarned = 0,
                orderIndex = 0
            ),
            TaskEntity(
                title = "Plants",
                scope = TaskScope.WEEKLY.name,
                periodKey = weekKey,
                targetStars = 1,
                starsEarned = 0,
                orderIndex = 0
            ),
            TaskEntity(
                title = "Laundry",
                scope = TaskScope.MONTHLY.name,
                periodKey = monthKey,
                targetStars = 3,
                starsEarned = 0,
                orderIndex = 0
            )
        )

        taskDao.insertTasks(defaultTasks)
        setStartOfWeek(startOfWeek)
        markInitialised()
    }

    override fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    override suspend fun importBackupData(
        data: ParsedBackupData,
        collisionStrategy: CollisionStrategy
    ): ImportResult {
        var addedCount = 0
        var updatedCount = 0
        var notesCount = 0

        // 1. Process tasks grouped by periodKey
        val tasksByPeriod = data.tasks.groupBy { it.periodKey }
        for ((periodKey, importedPeriodTasks) in tasksByPeriod) {
            val existingTasks = taskDao.getTasksByPeriodSync(periodKey).toMutableList()
            var nextOrderIndex = (existingTasks.maxOfOrNull { it.orderIndex } ?: -1) + 1

            for (imported in importedPeriodTasks) {
                val existingMatch = existingTasks.find {
                    it.title.trim().equals(imported.title.trim(), ignoreCase = true)
                }

                if (existingMatch != null) {
                    when (collisionStrategy) {
                        CollisionStrategy.MERGE_MAX_STARS -> {
                            val newEarned = maxOf(existingMatch.starsEarned, imported.starsEarned)
                            val newTarget = maxOf(existingMatch.targetStars, imported.targetStars)
                            val updatedTask = existingMatch.copy(
                                targetStars = newTarget,
                                starsEarned = newEarned
                            )
                            taskDao.updateTask(updatedTask)
                            val idx = existingTasks.indexOf(existingMatch)
                            if (idx != -1) existingTasks[idx] = updatedTask
                            updatedCount++
                        }
                        CollisionStrategy.OVERWRITE -> {
                            val updatedTask = existingMatch.copy(
                                targetStars = imported.targetStars,
                                starsEarned = imported.starsEarned
                            )
                            taskDao.updateTask(updatedTask)
                            val idx = existingTasks.indexOf(existingMatch)
                            if (idx != -1) existingTasks[idx] = updatedTask
                            updatedCount++
                        }
                        CollisionStrategy.SKIP_EXISTING -> {
                            // Keep existing unchanged
                        }
                    }
                } else {
                    val newTask = TaskEntity(
                        title = imported.title,
                        scope = imported.scope.name,
                        periodKey = periodKey,
                        targetStars = imported.targetStars,
                        starsEarned = imported.starsEarned,
                        orderIndex = nextOrderIndex++
                    )
                    taskDao.insertTask(newTask)
                    existingTasks.add(newTask)
                    addedCount++
                }
            }
        }

        // 2. Process notes
        for (importedNote in data.notes) {
            val existingNote = noteDao.getNoteForPeriodSync(importedNote.periodKey)
            if (existingNote == null) {
                noteDao.insertOrUpdateNote(
                    NoteEntity(
                        periodKey = importedNote.periodKey,
                        scope = importedNote.scope.name,
                        content = importedNote.content
                    )
                )
                notesCount++
            } else if (existingNote.content.isBlank()) {
                noteDao.insertOrUpdateNote(
                    existingNote.copy(content = importedNote.content)
                )
                notesCount++
            } else if (collisionStrategy == CollisionStrategy.OVERWRITE) {
                noteDao.insertOrUpdateNote(
                    existingNote.copy(content = importedNote.content)
                )
                notesCount++
            } else if (collisionStrategy == CollisionStrategy.MERGE_MAX_STARS && !existingNote.content.contains(importedNote.content)) {
                noteDao.insertOrUpdateNote(
                    existingNote.copy(content = "${existingNote.content}\n${importedNote.content}")
                )
                notesCount++
            }
        }

        markInitialised()

        return ImportResult(
            totalProcessed = data.tasks.size,
            tasksAdded = addedCount,
            tasksUpdated = updatedCount,
            notesImported = notesCount
        )
    }

    companion object {
        private const val KEY_START_OF_WEEK = "pref_start_of_week"
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_IS_INITIALISED = "pref_is_initialised"
    }
}
