package com.example

import com.example.data.model.NoteEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskScope
import com.example.export.JsonBackupFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JsonBackupFormatterTest {

    @Test
    fun testJsonExportAndImport() {
        val tasks = listOf(
            TaskEntity(
                id = 10,
                title = "Meditate",
                scope = TaskScope.DAILY.name,
                periodKey = "2026-09-16",
                starsEarned = 2,
                targetStars = 3,
                orderIndex = 0
            ),
            TaskEntity(
                id = 11,
                title = "Run 5k",
                scope = TaskScope.WEEKLY.name,
                periodKey = "W_2026-09-14",
                starsEarned = 1,
                targetStars = 1,
                orderIndex = 1
            )
        )
        val notes = listOf(
            NoteEntity(
                periodKey = "2026-09-16",
                scope = TaskScope.DAILY.name,
                content = "Felt energized today"
            )
        )

        val jsonString = JsonBackupFormatter.generateJson(tasks, notes)

        assertTrue(jsonString.contains("Meditate"))
        assertTrue(jsonString.contains("Run 5k"))
        assertTrue(jsonString.contains("Felt energized today"))

        val backupData = JsonBackupFormatter.parseJson(jsonString)
        assertEquals(2, backupData.tasks.size)
        assertEquals(1, backupData.notes.size)
        assertEquals("Meditate", backupData.tasks[0].title)
        assertEquals(2, backupData.tasks[0].starsEarned)
        assertEquals(3, backupData.tasks[0].targetStars)
        assertEquals("Felt energized today", backupData.notes[0].content)
    }
}
