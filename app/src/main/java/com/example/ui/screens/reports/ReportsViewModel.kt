package com.example.ui.screens.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.StartOfWeek
import com.example.data.model.TaskEntity
import com.example.data.model.TaskScope
import com.example.data.repository.CollisionStrategy
import com.example.data.repository.ImportResult
import com.example.data.repository.TaskRepository
import com.example.domain.date.PeriodCalculator
import com.example.domain.model.ScopeReportSummary
import com.example.export.JsonBackupFormatter
import com.example.export.ReportExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ReportPreset(val displayName: String) {
    LAST_7_DAYS("7 Days"),
    LAST_30_DAYS("30 Days"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

data class TrendGraphPoint(
    val label: String,
    val sublabel: String,
    val earnedStars: Int,
    val targetStars: Int,
    val completionPercentage: Int
)

data class ScopeStat(
    val scope: TaskScope,
    val totalTasks: Int,
    val completedTasks: Int,
    val earnedStars: Int,
    val targetStars: Int,
    val percentage: Int
)

data class OverallReportAnalytics(
    val totalStarsAllTime: Int,
    val totalCompletedTasksAllTime: Int,
    val averageCompletionRate: Int,
    val currentTrendRangeLabel: String,
    val isAtCurrentOffset: Boolean,
    val dailyTrend: List<TrendGraphPoint>,
    val weeklyTrend: List<TrendGraphPoint>,
    val monthlyTrend: List<TrendGraphPoint>,
    val yearlyTrend: List<TrendGraphPoint>,
    val scopeStats: List<ScopeStat>
)

enum class TrendTimeframe(val displayName: String) {
    DAYS("Days"),
    WEEKS("Weeks"),
    MONTHS("Months"),
    YEARS("Years")
}

class ReportsViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _startDate = MutableStateFlow(LocalDate.now().minusDays(6))
    val startDate: StateFlow<LocalDate> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(LocalDate.now())
    val endDate: StateFlow<LocalDate> = _endDate.asStateFlow()

    private val _selectedScopes = MutableStateFlow(TaskScope.entries.toSet())
    val selectedScopes: StateFlow<Set<TaskScope>> = _selectedScopes.asStateFlow()

    private val _selectedPreset = MutableStateFlow(ReportPreset.LAST_7_DAYS)
    val selectedPreset: StateFlow<ReportPreset> = _selectedPreset.asStateFlow()

    private val _selectedTrendTimeframe = MutableStateFlow(TrendTimeframe.DAYS)
    val selectedTrendTimeframe: StateFlow<TrendTimeframe> = _selectedTrendTimeframe.asStateFlow()

    private val _trendOffset = MutableStateFlow(0)
    val trendOffset: StateFlow<Int> = _trendOffset.asStateFlow()

    val startOfWeek: StateFlow<StartOfWeek> = repository.getStartOfWeek()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StartOfWeek.MONDAY)

    private val filterCriteria = combine(_startDate, _endDate, _selectedScopes, startOfWeek) { sDate, eDate, scopes, sow ->
        FilterConfig(sDate, eDate, scopes, sow)
    }

    private val allData = combine(repository.getAllTasks(), repository.getAllNotes()) { tasks, notes ->
        Pair(tasks, notes)
    }

    val reportSummaries: StateFlow<List<ScopeReportSummary>> =
        combine(filterCriteria, allData) { config, (tasks, notes) ->
            val result = mutableListOf<ScopeReportSummary>()
            val tasksByPeriod = tasks.groupBy { it.periodKey }
            val notesMap = notes.associateBy { it.periodKey }

            for (scope in config.scopes) {
                val periodKeys = PeriodCalculator.getPeriodKeysInRange(scope, config.startDate, config.endDate, config.startOfWeek)
                for ((periodKey, label) in periodKeys) {
                    val periodTasks = tasksByPeriod[periodKey] ?: emptyList()

                    val totalTarget = periodTasks.sumOf { it.targetStars }
                    val cappedEarned = periodTasks.sumOf { minOf(it.starsEarned, it.targetStars) }
                    val totalEarned = periodTasks.sumOf { it.starsEarned }
                    val completedCount = periodTasks.count { it.starsEarned >= it.targetStars && it.targetStars > 0 }
                    val pct = if (totalTarget > 0) ((cappedEarned.toDouble() / totalTarget.toDouble()) * 100).toInt().coerceIn(0, 100) else if (periodTasks.isNotEmpty()) 100 else 0

                    result.add(
                        ScopeReportSummary(
                            periodKey = periodKey,
                            displayLabel = label,
                            totalTargetStars = totalTarget,
                            totalEarnedStars = totalEarned,
                            completionPercentage = pct,
                            tasksCount = periodTasks.size,
                            completedTasksCount = completedCount,
                            note = notesMap[periodKey]?.content ?: ""
                        )
                    )
                }
            }
            result
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overallAnalytics: StateFlow<OverallReportAnalytics> =
        combine(repository.getAllTasks(), startOfWeek, _selectedTrendTimeframe, _trendOffset) { tasks, sow, timeframe, offset ->
            computeAnalytics(tasks, sow, timeframe, offset)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            OverallReportAnalytics(0, 0, 0, "", true, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        )

    private data class FilterConfig(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val scopes: Set<TaskScope>,
        val startOfWeek: StartOfWeek
    )

    fun setTrendTimeframe(timeframe: TrendTimeframe) {
        _selectedTrendTimeframe.value = timeframe
        _trendOffset.value = 0
    }

    fun shiftTrendOffset(delta: Int) {
        _trendOffset.value += delta
    }

    fun resetTrendOffset() {
        _trendOffset.value = 0
    }

    fun setPreset(preset: ReportPreset) {
        _selectedPreset.value = preset
        val today = LocalDate.now()
        when (preset) {
            ReportPreset.LAST_7_DAYS -> {
                _startDate.value = today.minusDays(6)
                _endDate.value = today
            }
            ReportPreset.LAST_30_DAYS -> {
                _startDate.value = today.minusDays(29)
                _endDate.value = today
            }
            ReportPreset.THIS_MONTH -> {
                _startDate.value = today.withDayOfMonth(1)
                _endDate.value = today
            }
            ReportPreset.THIS_YEAR -> {
                _startDate.value = today.withDayOfYear(1)
                _endDate.value = today
            }
            ReportPreset.ALL_TIME -> {
                _startDate.value = today.minusYears(1)
                _endDate.value = today
            }
        }
    }

    fun toggleScope(scope: TaskScope) {
        val current = _selectedScopes.value.toMutableSet()
        if (current.contains(scope)) {
            if (current.size > 1) current.remove(scope)
        } else {
            current.add(scope)
        }
        _selectedScopes.value = current
    }

    fun exportJson(context: Context) {
        viewModelScope.launch {
            val (tasks, notes) = repository.getAllTasks().combine(repository.getAllNotes()) { tasks, notes ->
                Pair(tasks, notes)
            }.first()

            val jsonContent = JsonBackupFormatter.generateJson(tasks, notes)
            ReportExporter.exportReport(
                context = context,
                content = jsonContent,
                fileExtension = "json",
                mimeType = "application/json",
                title = "Listender Tasks & Notes Backup (JSON)"
            )
        }
    }

    suspend fun importBackupContent(content: String, strategy: CollisionStrategy = CollisionStrategy.MERGE_MAX_STARS): ImportResult {
        return try {
            val parsedData = JsonBackupFormatter.parseJson(content)

            if (parsedData.tasks.isEmpty() && parsedData.notes.isEmpty()) {
                ImportResult(0, 0, 0, 0, errorMessage = "No valid task or note records found in backup file.")
            } else {
                repository.importBackupData(parsedData, strategy)
            }
        } catch (e: Exception) {
            ImportResult(0, 0, 0, 0, errorMessage = e.message ?: "Failed to parse backup file.")
        }
    }

    private fun computeAnalytics(
        tasks: List<TaskEntity>,
        sow: StartOfWeek,
        timeframe: TrendTimeframe,
        offset: Int
    ): OverallReportAnalytics {
        val today = LocalDate.now()
        val dayMonthFmt = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
        val monthYearFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.UK)

        // 1. Days Trend (7 Days window with offset)
        val dayAnchor = today.plusDays(offset.toLong())
        val dailyItems = (6 downTo 0).map { daysAgo ->
            val date = dayAnchor.minusDays(daysAgo.toLong())
            val periodTasks = PeriodCalculator.getTasksForPeriodAggregated(tasks, TaskScope.DAILY, date, sow)
            val summary = PeriodCalculator.calculatePeriodSummary(periodTasks, TaskScope.DAILY, date, sow)
            val dayName = date.dayOfWeek.name.take(3)
            val dayNum = date.dayOfMonth.toString()
            TrendGraphPoint(
                label = dayName,
                sublabel = dayNum,
                earnedStars = summary.totalEarnedStars,
                targetStars = summary.totalTargetStars,
                completionPercentage = summary.completionPercentage
            )
        }
        val firstDay = dayAnchor.minusDays(6)
        val dailyRangeLabel = "${firstDay.format(dayMonthFmt)} – ${dayAnchor.format(dayMonthFmt)} ${dayAnchor.year}"

        // 2. Weeks Trend (4 Weeks window with offset)
        val weekAnchor = today.plusWeeks(offset.toLong())
        val weeklyItems = (3 downTo 0).map { weeksAgo ->
            val date = weekAnchor.minusWeeks(weeksAgo.toLong())
            val periodTasks = PeriodCalculator.getTasksForPeriodAggregated(tasks, TaskScope.WEEKLY, date, sow)
            val summary = PeriodCalculator.calculatePeriodSummary(periodTasks, TaskScope.WEEKLY, date, sow)
            val (start, end) = PeriodCalculator.getWeekRange(date, sow)
            val weekSublabel = if (start.month == end.month) {
                "${start.dayOfMonth}–${end.dayOfMonth} ${start.month.name.take(3)}"
            } else {
                "${start.dayOfMonth} ${start.month.name.take(3)}–${end.dayOfMonth} ${end.month.name.take(3)}"
            }
            val label = if (weeksAgo == 0 && offset == 0) "This Week" else "Wk ${start.dayOfMonth}/${start.monthValue}"
            TrendGraphPoint(
                label = label,
                sublabel = weekSublabel,
                earnedStars = summary.totalEarnedStars,
                targetStars = summary.totalTargetStars,
                completionPercentage = summary.completionPercentage
            )
        }
        val (firstWeekStart, _) = PeriodCalculator.getWeekRange(weekAnchor.minusWeeks(3), sow)
        val (_, lastWeekEnd) = PeriodCalculator.getWeekRange(weekAnchor, sow)
        val weeklyRangeLabel = "${firstWeekStart.format(dayMonthFmt)} – ${lastWeekEnd.format(dayMonthFmt)} ${lastWeekEnd.year}"

        // 3. Months Trend (6 Months window with offset)
        val monthAnchor = today.plusMonths(offset.toLong())
        val monthlyItems = (5 downTo 0).map { monthsAgo ->
            val date = monthAnchor.minusMonths(monthsAgo.toLong())
            val periodTasks = PeriodCalculator.getTasksForPeriodAggregated(tasks, TaskScope.MONTHLY, date, sow)
            val summary = PeriodCalculator.calculatePeriodSummary(periodTasks, TaskScope.MONTHLY, date, sow)
            val monthName = date.month.name.take(3)
            TrendGraphPoint(
                label = monthName,
                sublabel = date.year.toString(),
                earnedStars = summary.totalEarnedStars,
                targetStars = summary.totalTargetStars,
                completionPercentage = summary.completionPercentage
            )
        }
        val firstMonth = monthAnchor.minusMonths(5)
        val monthlyRangeLabel = "${firstMonth.format(monthYearFmt)} – ${monthAnchor.format(monthYearFmt)}"

        // 4. Years Trend (5 Years window with offset)
        val yearAnchor = today.plusYears(offset.toLong())
        val yearlyItems = (4 downTo 0).map { yearsAgo ->
            val date = yearAnchor.minusYears(yearsAgo.toLong())
            val periodTasks = PeriodCalculator.getTasksForPeriodAggregated(tasks, TaskScope.YEARLY, date, sow)
            val summary = PeriodCalculator.calculatePeriodSummary(periodTasks, TaskScope.YEARLY, date, sow)
            val yearStr = date.year.toString()
            TrendGraphPoint(
                label = yearStr,
                sublabel = if (yearsAgo == 0 && offset == 0) "Current" else "${date.year}",
                earnedStars = summary.totalEarnedStars,
                targetStars = summary.totalTargetStars,
                completionPercentage = summary.completionPercentage
            )
        }
        val firstYear = yearAnchor.minusYears(4)
        val yearlyRangeLabel = "${firstYear.year} – ${yearAnchor.year}"

        val currentTrendRangeLabel = when (timeframe) {
            TrendTimeframe.DAYS -> dailyRangeLabel
            TrendTimeframe.WEEKS -> weeklyRangeLabel
            TrendTimeframe.MONTHS -> monthlyRangeLabel
            TrendTimeframe.YEARS -> yearlyRangeLabel
        }

        // 5. Scope stats (Aggregated for current periods of each scope)
        val scopeStats = TaskScope.entries.map { scope ->
            val aggregatedScopeTasks = PeriodCalculator.getTasksForPeriodAggregated(tasks, scope, today, sow)
            val summary = PeriodCalculator.calculatePeriodSummary(aggregatedScopeTasks, scope, today, sow)
            ScopeStat(
                scope = scope,
                totalTasks = summary.totalTasks,
                completedTasks = summary.completedTasks,
                earnedStars = summary.totalEarnedStars,
                targetStars = summary.totalTargetStars,
                percentage = summary.completionPercentage
            )
        }

        val totalStarsAllTime = tasks.sumOf { it.starsEarned }
        val totalCompletedTasksAllTime = tasks.count { it.starsEarned >= it.targetStars && it.targetStars > 0 }
        val totalTarget = tasks.sumOf { it.targetStars }
        val totalCapped = tasks.sumOf { minOf(it.starsEarned, it.targetStars) }
        val averageCompletionRate = if (totalTarget > 0) ((totalCapped.toDouble() / totalTarget.toDouble()) * 100).toInt().coerceIn(0, 100) else 0

        return OverallReportAnalytics(
            totalStarsAllTime = totalStarsAllTime,
            totalCompletedTasksAllTime = totalCompletedTasksAllTime,
            averageCompletionRate = averageCompletionRate,
            currentTrendRangeLabel = currentTrendRangeLabel,
            isAtCurrentOffset = offset == 0,
            dailyTrend = dailyItems,
            weeklyTrend = weeklyItems,
            monthlyTrend = monthlyItems,
            yearlyTrend = yearlyItems,
            scopeStats = scopeStats
        )
    }
}

class ReportsViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            return ReportsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

