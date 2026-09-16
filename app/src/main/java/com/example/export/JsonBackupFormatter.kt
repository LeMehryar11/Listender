package com.example.export

import com.example.data.model.NoteEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskScope
import org.json.JSONArray
import org.json.JSONObject

data class ImportedTaskRecord(
    val title: String,
    val scope: TaskScope,
    val periodKey: String,
    val targetStars: Int,
    val starsEarned: Int
)

data class ImportedNoteRecord(
    val periodKey: String,
    val scope: TaskScope,
    val content: String
)

data class ParsedBackupData(
    val tasks: List<ImportedTaskRecord>,
    val notes: List<ImportedNoteRecord>
)

object JsonBackupFormatter {

    fun generateJson(tasks: List<TaskEntity>, notes: List<NoteEntity>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "Listender")
        root.put("exportedAt", System.currentTimeMillis())

        val tasksArray = JSONArray()
        tasks.forEach { task ->
            val taskObj = JSONObject()
            taskObj.put("title", task.title)
            taskObj.put("scope", task.scope)
            taskObj.put("periodKey", task.periodKey)
            taskObj.put("targetStars", task.targetStars)
            taskObj.put("starsEarned", task.starsEarned)
            taskObj.put("orderIndex", task.orderIndex)
            taskObj.put("createdAt", task.createdAt)
            tasksArray.put(taskObj)
        }
        root.put("tasks", tasksArray)

        val notesArray = JSONArray()
        notes.forEach { note ->
            val noteObj = JSONObject()
            noteObj.put("periodKey", note.periodKey)
            noteObj.put("scope", note.scope)
            noteObj.put("content", note.content)
            noteObj.put("updatedAt", note.updatedAt)
            notesArray.put(noteObj)
        }
        root.put("notes", notesArray)

        return root.toString(2)
    }

    fun parseJson(jsonString: String): ParsedBackupData {
        val root = JSONObject(jsonString.trim())
        val tasksList = mutableListOf<ImportedTaskRecord>()
        val notesList = mutableListOf<ImportedNoteRecord>()

        if (root.has("tasks")) {
            val tasksArray = root.getJSONArray("tasks")
            for (i in 0 until tasksArray.length()) {
                val item = tasksArray.getJSONObject(i)
                val title = item.optString("title", "").trim()
                if (title.isBlank()) continue

                val scopeStr = item.optString("scope", TaskScope.DAILY.name)
                val scope = try { TaskScope.valueOf(scopeStr) } catch (_: Exception) { TaskScope.DAILY }
                val periodKey = item.optString("periodKey", "")
                val targetStars = item.optInt("targetStars", 1).coerceAtLeast(1)
                val starsEarned = item.optInt("starsEarned", 0).coerceAtLeast(0)

                tasksList.add(
                    ImportedTaskRecord(
                        title = title,
                        scope = scope,
                        periodKey = periodKey,
                        targetStars = targetStars,
                        starsEarned = starsEarned
                    )
                )
            }
        }

        if (root.has("notes")) {
            val notesArray = root.getJSONArray("notes")
            for (i in 0 until notesArray.length()) {
                val item = notesArray.getJSONObject(i)
                val content = item.optString("content", "")
                val periodKey = item.optString("periodKey", "")
                val scopeStr = item.optString("scope", TaskScope.DAILY.name)
                val scope = try { TaskScope.valueOf(scopeStr) } catch (_: Exception) { TaskScope.DAILY }

                if (periodKey.isNotBlank()) {
                    notesList.add(
                        ImportedNoteRecord(
                            periodKey = periodKey,
                            scope = scope,
                            content = content
                        )
                    )
                }
            }
        }

        return ParsedBackupData(tasks = tasksList, notes = notesList)
    }
}
