package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NoteEntity
import com.example.data.model.PreferenceEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY orderIndex ASC, id ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE periodKey = :periodKey ORDER BY orderIndex ASC, id ASC")
    fun getTasksByPeriod(periodKey: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE periodKey = :periodKey ORDER BY orderIndex ASC, id ASC")
    suspend fun getTasksByPeriodSync(periodKey: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE scope = :scope ORDER BY orderIndex ASC, id ASC")
    fun getTasksByScope(scope: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>): List<Long>

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET orderIndex = :newOrder WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, newOrder: Int)

    @Query("UPDATE tasks SET starsEarned = :stars WHERE id = :id")
    suspend fun updateStarsEarned(id: Long, stars: Int)
}

@Dao
interface TaskLogDao {
    @Query("SELECT * FROM task_logs WHERE periodKey = :periodKey")
    fun getLogsForPeriod(periodKey: String): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM task_logs")
    fun getAllLogs(): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM task_logs WHERE periodKey IN (:periodKeys)")
    fun getLogsForPeriodKeys(periodKeys: List<String>): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM task_logs WHERE taskId = :taskId AND periodKey = :periodKey LIMIT 1")
    suspend fun getLogForTaskAndPeriod(taskId: Long, periodKey: String): TaskLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLog(log: TaskLogEntity)

    @Query("DELETE FROM task_logs WHERE taskId = :taskId")
    suspend fun deleteLogsForTask(taskId: Long)

    @Query("SELECT * FROM task_logs WHERE periodKey LIKE 'W_%'")
    suspend fun getAllWeeklyLogsSync(): List<TaskLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLogs(logs: List<TaskLogEntity>)

    @Query("DELETE FROM task_logs WHERE id IN (:ids)")
    suspend fun deleteLogsByIds(ids: List<Long>)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE periodKey = :periodKey LIMIT 1")
    fun getNoteForPeriod(periodKey: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE periodKey = :periodKey LIMIT 1")
    suspend fun getNoteForPeriodSync(periodKey: String): NoteEntity?

    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE periodKey = :periodKey")
    suspend fun deleteNote(periodKey: String)

    @Query("SELECT * FROM notes WHERE periodKey LIKE 'W_%'")
    suspend fun getAllWeeklyNotesSync(): List<NoteEntity>
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preferences WHERE `key` = :key LIMIT 1")
    fun getPreference(key: String): Flow<PreferenceEntity?>

    @Query("SELECT * FROM preferences WHERE `key` = :key LIMIT 1")
    suspend fun getPreferenceSync(key: String): PreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(preference: PreferenceEntity)
}
