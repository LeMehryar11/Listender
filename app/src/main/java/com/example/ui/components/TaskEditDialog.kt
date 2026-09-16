package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TaskEntity
import com.example.data.model.TaskScope
import com.example.ui.theme.BrandCrimson

@Composable
fun TaskEditDialog(
    initialTask: TaskEntity?,
    defaultScope: TaskScope,
    currentPeriodKey: String,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit
) {
    val isNew = initialTask == null
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var selectedScope by remember {
        mutableStateOf(initialTask?.let { TaskScope.fromString(it.scope) } ?: defaultScope)
    }
    var targetStarsInput by remember {
        mutableStateOf((initialTask?.targetStars ?: 1).toString())
    }
    var achievedStarsInput by remember {
        mutableStateOf((initialTask?.starsEarned ?: 0).toString())
    }

    val parsedTarget = (targetStarsInput.toIntOrNull() ?: 1).coerceAtLeast(1)
    val parsedAchieved = achievedStarsInput.toIntOrNull() ?: 0
    val isAchievedExceeding = parsedAchieved > parsedTarget

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("task_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNew) "New Task" else "Modify Task",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Read 11 pages") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_task_title")
                )

                // Scope Selector
                Column {
                    Text(
                        text = "SCOPE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TaskScope.entries.forEach { scope ->
                            val isSelected = scope == selectedScope
                            val bg = if (isSelected) BrandCrimson else MaterialTheme.colorScheme.surfaceVariant
                            val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .clickable { selectedScope = scope }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = scope.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                // Target Stars Input
                Column {
                    Text(
                        text = "TARGET STARS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetStarsInput,
                            onValueChange = { input ->
                                targetStarsInput = input.filter { it.isDigit() }
                            },
                            placeholder = { Text("1") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_task_target_stars")
                        )
                        IconButton(
                            onClick = {
                                val cur = targetStarsInput.toIntOrNull() ?: 1
                                if (cur > 1) targetStarsInput = (cur - 1).toString()
                            },
                            enabled = (targetStarsInput.toIntOrNull() ?: 1) > 1,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Target",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                val cur = targetStarsInput.toIntOrNull() ?: 1
                                targetStarsInput = (cur + 1).toString()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Target",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Achieved Stars Input
                Column {
                    Text(
                        text = "ACHIEVED STARS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = achievedStarsInput,
                            onValueChange = { input ->
                                achievedStarsInput = input.filter { it.isDigit() }
                            },
                            placeholder = { Text("0") },
                            singleLine = true,
                            isError = isAchievedExceeding,
                            supportingText = if (isAchievedExceeding) {
                                { Text("Cannot exceed target ($parsedTarget)", color = BrandCrimson) }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_task_achieved_stars")
                        )
                        IconButton(
                            onClick = {
                                val cur = achievedStarsInput.toIntOrNull() ?: 0
                                if (cur > 0) achievedStarsInput = (cur - 1).toString()
                            },
                            enabled = (achievedStarsInput.toIntOrNull() ?: 0) > 0,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Achieved",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                val cur = achievedStarsInput.toIntOrNull() ?: 0
                                if (cur < parsedTarget) achievedStarsInput = (cur + 1).toString()
                            },
                            enabled = (achievedStarsInput.toIntOrNull() ?: 0) < parsedTarget,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Achieved",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank() && !isAchievedExceeding) {
                                val taskToSave = TaskEntity(
                                    id = initialTask?.id ?: 0L,
                                    title = title.trim(),
                                    scope = selectedScope.name,
                                    periodKey = initialTask?.periodKey ?: currentPeriodKey,
                                    targetStars = parsedTarget,
                                    starsEarned = parsedAchieved.coerceIn(0, parsedTarget),
                                    orderIndex = initialTask?.orderIndex ?: 0,
                                    createdAt = initialTask?.createdAt ?: System.currentTimeMillis()
                                )
                                onSave(taskToSave)
                            }
                        },
                        enabled = title.isNotBlank() && !isAchievedExceeding && targetStarsInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandCrimson,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_task")
                    ) {
                        Text(if (isNew) "Add Task" else "Save")
                    }
                }
            }
        }
    }
}
