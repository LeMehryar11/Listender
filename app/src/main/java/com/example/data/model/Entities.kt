package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["periodKey", "orderIndex"]),
        Index(value = ["scope"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val scope: String, // DAILY, WEEKLY, MONTHLY, YEARLY
    val periodKey: String, // e.g., "2026-08-30", "W_2026-08-24", "M_2026-08", "Y_2026"
    val targetStars: Int,
    val starsEarned: Int = 0,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "task_logs",
    indices = [
        Index(value = ["taskId", "periodKey"], unique = true),
        Index(value = ["periodKey"])
    ]
)
data class TaskLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long,
    val periodKey: String, // e.g., "2026-08-29", "2026-W35", "2026-08", "2026"
    val starsEarned: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val periodKey: String,
    val scope: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)
