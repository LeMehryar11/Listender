package com.example.domain.date

import com.example.data.model.StartOfWeek
import com.example.data.model.TaskEntity
import com.example.data.model.TaskScope
import com.example.domain.model.PeriodSummary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object PeriodCalculator {
    private val ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd
    private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.UK)
    private val DAY_MONTH_FORMATTER = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
    private val FULL_DAY_FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.UK)
    private val SHORT_DAY_FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.UK)

    fun parseIsoDate(dateString: String): LocalDate {
        return try {
            LocalDate.parse(dateString, ISO_DATE_FORMATTER)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    fun formatIsoDate(date: LocalDate): String {
        return date.format(ISO_DATE_FORMATTER)
    }

    /**
     * Calculates the start date of the week containing the given date based on the chosen start of week.
     */
    fun getWeekStartDate(date: LocalDate, startOfWeek: StartOfWeek): LocalDate {
        val targetDayOfWeek = startOfWeek.dayOfWeek
        var current = date
        while (current.dayOfWeek != targetDayOfWeek) {
            current = current.minusDays(1)
        }
        return current
    }

    /**
     * Returns the 7-day span (start date to end date) for the week containing [date].
     */
    fun getWeekRange(date: LocalDate, startOfWeek: StartOfWeek): Pair<LocalDate, LocalDate> {
        val start = getWeekStartDate(date, startOfWeek)
        val end = start.plusDays(6)
        return Pair(start, end)
    }

    /**
     * Generates a deterministic canonical key for storing logs and notes per period.
     */
    fun getPeriodKey(scope: TaskScope, date: LocalDate, startOfWeek: StartOfWeek): String {
        return when (scope) {
            TaskScope.DAILY -> date.format(ISO_DATE_FORMATTER)
            TaskScope.WEEKLY -> {
                val start = getWeekStartDate(date, startOfWeek)
                "W_${start.format(ISO_DATE_FORMATTER)}"
            }
            TaskScope.MONTHLY -> "M_${date.year}-${String.format(Locale.ROOT, "%02d", date.monthValue)}"
            TaskScope.YEARLY -> "Y_${date.year}"
        }
    }

    /**
     * Formats human-readable period display labels (e.g. "Today, Sun, 30 Aug", "Mon, 31 Aug 2026", "24 Aug – 30 Aug 2026", "August 2026", "2026").
     */
    fun getPeriodDisplayLabel(scope: TaskScope, date: LocalDate, startOfWeek: StartOfWeek): String {
        return when (scope) {
            TaskScope.DAILY -> {
                val today = LocalDate.now()
                if (date == today) "Today, ${date.format(SHORT_DAY_FORMATTER)}"
                else date.format(FULL_DAY_FORMATTER)
            }
            TaskScope.WEEKLY -> {
                val (start, end) = getWeekRange(date, startOfWeek)
                if (start.month == end.month && start.year == end.year) {
                    "${start.dayOfMonth}–${end.dayOfMonth} ${start.format(MONTH_YEAR_FORMATTER)}"
                } else if (start.year == end.year) {
                    "${start.format(DAY_MONTH_FORMATTER)} – ${end.format(DAY_MONTH_FORMATTER)} ${start.year}"
                } else {
                    "${start.format(DAY_MONTH_FORMATTER)} ${start.year} – ${end.format(DAY_MONTH_FORMATTER)} ${end.year}"
                }
            }
            TaskScope.MONTHLY -> {
                date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.UK))
            }
            TaskScope.YEARLY -> {
                "${date.year}"
            }
        }
    }

    /**
     * Checks if the given date corresponds to the present/current period for the given scope.
     */
    fun isCurrentPeriod(scope: TaskScope, date: LocalDate, startOfWeek: StartOfWeek): Boolean {
        val today = LocalDate.now()
        return when (scope) {
            TaskScope.DAILY -> date == today
            TaskScope.WEEKLY -> {
                val (start, end) = getWeekRange(date, startOfWeek)
                isDateInCurrentWeek(today, start, end)
            }
            TaskScope.MONTHLY -> date.year == today.year && date.month == today.month
            TaskScope.YEARLY -> date.year == today.year
        }
    }

    private fun isDateInCurrentWeek(today: LocalDate, start: LocalDate, end: LocalDate): Boolean {
        return !today.isBefore(start) && !today.isAfter(end)
    }

    /**
     * Shifts the active date forwards or backwards by 1 unit of the given scope.
     */
    fun shiftPeriod(scope: TaskScope, currentDate: LocalDate, delta: Int): LocalDate {
        return when (scope) {
            TaskScope.DAILY -> currentDate.plusDays(delta.toLong())
            TaskScope.WEEKLY -> currentDate.plusWeeks(delta.toLong())
            TaskScope.MONTHLY -> currentDate.plusMonths(delta.toLong())
            TaskScope.YEARLY -> currentDate.plusYears(delta.toLong())
        }
    }

    /**
     * Returns the previous period key for the given scope and date.
     */
    fun getPreviousPeriodKey(scope: TaskScope, currentDate: LocalDate, startOfWeek: StartOfWeek): String {
        val previousDate = shiftPeriod(scope, currentDate, -1)
        return getPeriodKey(scope, previousDate, startOfWeek)
    }

    /**
     * Returns the next period key for the given scope and date.
     */
    fun getNextPeriodKey(scope: TaskScope, currentDate: LocalDate, startOfWeek: StartOfWeek): String {
        val nextDate = shiftPeriod(scope, currentDate, 1)
        return getPeriodKey(scope, nextDate, startOfWeek)
    }

    /**
     * Returns the menu label for moving a task to the next period.
     */
    fun getNextPeriodActionLabel(scope: TaskScope): String {
        return when (scope) {
            TaskScope.DAILY -> "Move to tomorrow"
            TaskScope.WEEKLY -> "Move to the next week"
            TaskScope.MONTHLY -> "Move to the next month"
            TaskScope.YEARLY -> "Move to the next year"
        }
    }

    /**
     * Returns the menu label for moving a task to the current period of the same scope.
     */
    fun getCurrentPeriodActionLabel(scope: TaskScope): String {
        return when (scope) {
            TaskScope.DAILY -> "Move to today"
            TaskScope.WEEKLY -> "Move to the current week"
            TaskScope.MONTHLY -> "Move to the current month"
            TaskScope.YEARLY -> "Move to the current year"
        }
    }

    /**
     * Returns the relative import button label (e.g. "yesterday", "last week", "last month", "last year").
     */
    fun getPreviousRelativeLabel(scope: TaskScope): String {
        return when (scope) {
            TaskScope.DAILY -> "the day before"
            TaskScope.WEEKLY -> "the week before"
            TaskScope.MONTHLY -> "the month before"
            TaskScope.YEARLY -> "the year before"
        }
    }

    /**
     * Returns the previous period's full human-readable label.
     */
    fun getPreviousPeriodDisplayLabel(scope: TaskScope, currentDate: LocalDate, startOfWeek: StartOfWeek): String {
        val previousDate = shiftPeriod(scope, currentDate, -1)
        return getPeriodDisplayLabel(scope, previousDate, startOfWeek)
    }

    /**
     * Filters all tasks to those belonging to the given scope and period, including all lower layers:
     * - Daily: only daily tasks for that date (periodKey == "yyyy-MM-dd").
     * - Weekly: weekly tasks for that week ("W_{startDate}") + all daily tasks for days in [startDate..endDate].
     * - Monthly: monthly tasks for that month ("M_{yyyy-MM}") + weekly tasks whose start falls in that month + daily tasks in that month.
     * - Yearly: yearly tasks for that year ("Y_{yyyy}") + monthly tasks in that year + weekly tasks in that year + daily tasks in that year.
     */
    fun getTasksForPeriodAggregated(
        allTasks: List<TaskEntity>,
        scope: TaskScope,
        date: LocalDate,
        startOfWeek: StartOfWeek
    ): List<TaskEntity> {
        return when (scope) {
            TaskScope.DAILY -> {
                val dayKey = getPeriodKey(TaskScope.DAILY, date, startOfWeek)
                allTasks.filter { it.periodKey == dayKey }
            }
            TaskScope.WEEKLY -> {
                val (start, end) = getWeekRange(date, startOfWeek)
                val weekKey = getPeriodKey(TaskScope.WEEKLY, date, startOfWeek)
                val startIso = formatIsoDate(start)
                val endIso = formatIsoDate(end)

                allTasks.filter { task ->
                    task.periodKey == weekKey ||
                    (task.scope == TaskScope.DAILY.name && task.periodKey in startIso..endIso)
                }
            }
            TaskScope.MONTHLY -> {
                val monthKey = getPeriodKey(TaskScope.MONTHLY, date, startOfWeek)
                val monthPrefix = "${date.year}-${String.format(Locale.ROOT, "%02d", date.monthValue)}"

                allTasks.filter { task ->
                    task.periodKey == monthKey ||
                    (task.scope == TaskScope.DAILY.name && task.periodKey.startsWith(monthPrefix)) ||
                    (task.scope == TaskScope.WEEKLY.name && task.periodKey.startsWith("W_$monthPrefix"))
                }
            }
            TaskScope.YEARLY -> {
                val yearKey = getPeriodKey(TaskScope.YEARLY, date, startOfWeek)
                val yearPrefix = "${date.year}-"

                allTasks.filter { task ->
                    task.periodKey == yearKey ||
                    (task.scope == TaskScope.MONTHLY.name && task.periodKey.startsWith("M_$yearPrefix")) ||
                    (task.scope == TaskScope.WEEKLY.name && task.periodKey.startsWith("W_$yearPrefix")) ||
                    (task.scope == TaskScope.DAILY.name && task.periodKey.startsWith(yearPrefix))
                }
            }
        }
    }

    /**
     * Calculates the aggregated period performance summary for a given scope and date.
     */
    fun calculatePeriodSummary(
        tasks: List<TaskEntity>,
        scope: TaskScope,
        date: LocalDate,
        startOfWeek: StartOfWeek
    ): PeriodSummary {
        val periodKey = getPeriodKey(scope, date, startOfWeek)
        val displayLabel = getPeriodDisplayLabel(scope, date, startOfWeek)
        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.starsEarned >= it.targetStars && it.targetStars > 0 }
        val totalTargetStars = tasks.sumOf { it.targetStars }
        val totalEarnedStars = tasks.sumOf { it.starsEarned }
        val cappedEarnedSum = tasks.sumOf { minOf(it.starsEarned, it.targetStars) }
        val percentage = if (totalTargetStars <= 0) {
            if (totalTasks > 0 && completedTasks == totalTasks) 100 else 0
        } else {
            ((cappedEarnedSum.toDouble() / totalTargetStars.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        }

        return PeriodSummary(
            scope = scope,
            periodKey = periodKey,
            displayLabel = displayLabel,
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            totalTargetStars = totalTargetStars,
            totalEarnedStars = totalEarnedStars,
            completionPercentage = percentage
        )
    }

    /**
     * Returns ordered pairs of (periodKey, displayLabel) for each distinct period within [startDate, endDate].
     */
    fun getPeriodKeysInRange(
        scope: TaskScope,
        startDate: LocalDate,
        endDate: LocalDate,
        startOfWeek: StartOfWeek
    ): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val seen = mutableSetOf<String>()
        var current = startDate

        while (!current.isAfter(endDate)) {
            val key = getPeriodKey(scope, current, startOfWeek)
            if (seen.add(key)) {
                val label = getPeriodDisplayLabel(scope, current, startOfWeek)
                result.add(Pair(key, label))
            }
            current = when (scope) {
                TaskScope.DAILY -> current.plusDays(1)
                TaskScope.WEEKLY -> current.plusWeeks(1)
                TaskScope.MONTHLY -> current.plusMonths(1)
                TaskScope.YEARLY -> current.plusYears(1)
            }
        }
        return result
    }
}
