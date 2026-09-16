package com.example.domain.model

import com.example.data.model.TaskEntity
import com.example.data.model.TaskScope

data class TaskWithProgress(
    val task: TaskEntity,
    val starsEarned: Int,
    val periodKey: String
) {
    val targetStars: Int get() = task.targetStars
    val isCompleted: Boolean get() = starsEarned >= targetStars
    val completionPercentage: Float get() = if (targetStars <= 0) 0f else (starsEarned.toFloat() / targetStars.toFloat()).coerceAtMost(1f)
}

data class PeriodSummary(
    val scope: TaskScope,
    val periodKey: String,
    val displayLabel: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val totalTargetStars: Int,
    val totalEarnedStars: Int,
    val completionPercentage: Int // 0 to 100
)

data class ReportRow(
    val periodKey: String,
    val scope: String,
    val taskTitle: String,
    val targetStars: Int,
    val earnedStars: Int,
    val isMet: Boolean,
    val periodNotes: String?
)

data class ScopeReportSummary(
    val periodKey: String,
    val displayLabel: String,
    val totalTargetStars: Int,
    val totalEarnedStars: Int,
    val completionPercentage: Int,
    val tasksCount: Int,
    val completedTasksCount: Int,
    val note: String
)
