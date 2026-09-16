package com.example

import com.example.data.model.StartOfWeek
import com.example.data.model.TaskScope
import com.example.domain.date.PeriodCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PeriodCalculatorTest {

    @Test
    fun testStartOfWeekCalculation() {
        // 2026-08-29 is a Saturday
        val date = LocalDate.of(2026, 8, 29)

        // Monday start
        val monStart = PeriodCalculator.getWeekStartDate(date, StartOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 8, 24), monStart)

        // Sunday start
        val sunStart = PeriodCalculator.getWeekStartDate(date, StartOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 8, 23), sunStart)

        // Saturday start
        val satStart = PeriodCalculator.getWeekStartDate(date, StartOfWeek.SATURDAY)
        assertEquals(LocalDate.of(2026, 8, 29), satStart)
    }

    @Test
    fun testPeriodKeys() {
        val date = LocalDate.of(2026, 8, 29)
        assertEquals("2026-08-29", PeriodCalculator.getPeriodKey(TaskScope.DAILY, date, StartOfWeek.MONDAY))
        assertEquals("W_2026-08-24", PeriodCalculator.getPeriodKey(TaskScope.WEEKLY, date, StartOfWeek.MONDAY))
        assertEquals("M_2026-08", PeriodCalculator.getPeriodKey(TaskScope.MONTHLY, date, StartOfWeek.MONDAY))
        assertEquals("Y_2026", PeriodCalculator.getPeriodKey(TaskScope.YEARLY, date, StartOfWeek.MONDAY))
    }

    @Test
    fun testShiftPeriod() {
        val date = LocalDate.of(2026, 8, 29)
        assertEquals(LocalDate.of(2026, 8, 30), PeriodCalculator.shiftPeriod(TaskScope.DAILY, date, 1))
        assertEquals(LocalDate.of(2026, 8, 28), PeriodCalculator.shiftPeriod(TaskScope.DAILY, date, -1))
        assertEquals(LocalDate.of(2026, 9, 29), PeriodCalculator.shiftPeriod(TaskScope.MONTHLY, date, 1))
        assertEquals(LocalDate.of(2027, 8, 29), PeriodCalculator.shiftPeriod(TaskScope.YEARLY, date, 1))
    }

    @Test
    fun testPreviousPeriodKeys() {
        val date = LocalDate.of(2026, 8, 29)
        val prevDayKey = PeriodCalculator.getPreviousPeriodKey(TaskScope.DAILY, date, StartOfWeek.MONDAY)
        assertEquals("2026-08-28", prevDayKey)

        val prevWeekKey = PeriodCalculator.getPreviousPeriodKey(TaskScope.WEEKLY, date, StartOfWeek.MONDAY)
        assertEquals("W_2026-08-17", prevWeekKey)

        val prevMonthKey = PeriodCalculator.getPreviousPeriodKey(TaskScope.MONTHLY, date, StartOfWeek.MONDAY)
        assertEquals("M_2026-07", prevMonthKey)

        val prevYearKey = PeriodCalculator.getPreviousPeriodKey(TaskScope.YEARLY, date, StartOfWeek.MONDAY)
        assertEquals("Y_2025", prevYearKey)
    }

    @Test
    fun testAggregationAcrossScopes() {
        val date = LocalDate.of(2026, 8, 29)
        val sow = StartOfWeek.MONDAY

        val taskDaily = com.example.data.model.TaskEntity(
            id = 1,
            title = "Drink Water",
            scope = TaskScope.DAILY.name,
            periodKey = "2026-08-29",
            starsEarned = 3,
            targetStars = 5
        )
        val taskWeekly = com.example.data.model.TaskEntity(
            id = 2,
            title = "Water Plants",
            scope = TaskScope.WEEKLY.name,
            periodKey = "W_2026-08-24",
            starsEarned = 1,
            targetStars = 1
        )
        val taskMonthly = com.example.data.model.TaskEntity(
            id = 3,
            title = "Budget Check",
            scope = TaskScope.MONTHLY.name,
            periodKey = "M_2026-08",
            starsEarned = 1,
            targetStars = 1
        )
        val allTasks = listOf(taskDaily, taskWeekly, taskMonthly)

        // Weekly aggregated should include daily + weekly
        val weeklyAggregated = PeriodCalculator.getTasksForPeriodAggregated(allTasks, TaskScope.WEEKLY, date, sow)
        assertEquals(2, weeklyAggregated.size)
        val weeklySummary = PeriodCalculator.calculatePeriodSummary(weeklyAggregated, TaskScope.WEEKLY, date, sow)
        assertEquals(4, weeklySummary.totalEarnedStars)
        assertEquals(6, weeklySummary.totalTargetStars)

        // Monthly aggregated should include daily + weekly + monthly
        val monthlyAggregated = PeriodCalculator.getTasksForPeriodAggregated(allTasks, TaskScope.MONTHLY, date, sow)
        assertEquals(3, monthlyAggregated.size)
        val monthlySummary = PeriodCalculator.calculatePeriodSummary(monthlyAggregated, TaskScope.MONTHLY, date, sow)
        assertEquals(5, monthlySummary.totalEarnedStars)
        assertEquals(7, monthlySummary.totalTargetStars)
    }

    @Test
    fun testIsCurrentPeriod() {
        val today = LocalDate.now()
        val sow = StartOfWeek.MONDAY
        assertEquals(true, PeriodCalculator.isCurrentPeriod(TaskScope.DAILY, today, sow))
        assertEquals(false, PeriodCalculator.isCurrentPeriod(TaskScope.DAILY, today.minusDays(1), sow))

        assertEquals(true, PeriodCalculator.isCurrentPeriod(TaskScope.WEEKLY, today, sow))
        assertEquals(false, PeriodCalculator.isCurrentPeriod(TaskScope.WEEKLY, today.minusWeeks(2), sow))

        assertEquals(true, PeriodCalculator.isCurrentPeriod(TaskScope.MONTHLY, today, sow))
        assertEquals(false, PeriodCalculator.isCurrentPeriod(TaskScope.MONTHLY, today.minusMonths(2), sow))

        assertEquals(true, PeriodCalculator.isCurrentPeriod(TaskScope.YEARLY, today, sow))
        assertEquals(false, PeriodCalculator.isCurrentPeriod(TaskScope.YEARLY, today.minusYears(1), sow))
    }

    @Test
    fun testPreviousRelativeLabel() {
        assertEquals("the day before", PeriodCalculator.getPreviousRelativeLabel(TaskScope.DAILY))
        assertEquals("the week before", PeriodCalculator.getPreviousRelativeLabel(TaskScope.WEEKLY))
        assertEquals("the month before", PeriodCalculator.getPreviousRelativeLabel(TaskScope.MONTHLY))
        assertEquals("the year before", PeriodCalculator.getPreviousRelativeLabel(TaskScope.YEARLY))
    }

    @Test
    fun testNextPeriodKeyAndLabel() {
        val date = LocalDate.of(2026, 8, 29)
        val sow = StartOfWeek.MONDAY

        val nextDayKey = PeriodCalculator.getNextPeriodKey(TaskScope.DAILY, date, sow)
        assertEquals("2026-08-30", nextDayKey)
        assertEquals("Move to tomorrow", PeriodCalculator.getNextPeriodActionLabel(TaskScope.DAILY))

        val nextWeekKey = PeriodCalculator.getNextPeriodKey(TaskScope.WEEKLY, date, sow)
        assertEquals("W_2026-08-31", nextWeekKey)
        assertEquals("Move to the next week", PeriodCalculator.getNextPeriodActionLabel(TaskScope.WEEKLY))

        val nextMonthKey = PeriodCalculator.getNextPeriodKey(TaskScope.MONTHLY, date, sow)
        assertEquals("M_2026-09", nextMonthKey)
        assertEquals("Move to the next month", PeriodCalculator.getNextPeriodActionLabel(TaskScope.MONTHLY))

        val nextYearKey = PeriodCalculator.getNextPeriodKey(TaskScope.YEARLY, date, sow)
        assertEquals("Y_2027", nextYearKey)
        assertEquals("Move to the next year", PeriodCalculator.getNextPeriodActionLabel(TaskScope.YEARLY))
    }

    @Test
    fun testCurrentPeriodActionLabel() {
        assertEquals("Move to today", PeriodCalculator.getCurrentPeriodActionLabel(TaskScope.DAILY))
        assertEquals("Move to the current week", PeriodCalculator.getCurrentPeriodActionLabel(TaskScope.WEEKLY))
        assertEquals("Move to the current month", PeriodCalculator.getCurrentPeriodActionLabel(TaskScope.MONTHLY))
        assertEquals("Move to the current year", PeriodCalculator.getCurrentPeriodActionLabel(TaskScope.YEARLY))
    }

    @Test
    fun testScopeTargetKeys() {
        val date = LocalDate.of(2026, 9, 16)
        val sow = StartOfWeek.MONDAY

        val dailyKey = PeriodCalculator.getPeriodKey(TaskScope.DAILY, date, sow)
        val weeklyKey = PeriodCalculator.getPeriodKey(TaskScope.WEEKLY, date, sow)
        val monthlyKey = PeriodCalculator.getPeriodKey(TaskScope.MONTHLY, date, sow)
        val yearlyKey = PeriodCalculator.getPeriodKey(TaskScope.YEARLY, date, sow)

        assertEquals("2026-09-16", dailyKey)
        assertEquals("W_2026-09-14", weeklyKey)
        assertEquals("M_2026-09", monthlyKey)
        assertEquals("Y_2026", yearlyKey)
    }
}
