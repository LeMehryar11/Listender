package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TaskWithProgress

data class MoveTaskOption(
    val label: String,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
fun TaskItemCard(
    taskWithProgress: TaskWithProgress,
    onIncrementStars: () -> Unit,
    onDecrementStars: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveToNextPeriod: () -> Unit,
    nextPeriodLabel: String,
    moveOptions: List<MoveTaskOption> = emptyList(),
    modifier: Modifier = Modifier
) {
    val task = taskWithProgress.task
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onEdit() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("task_item_${task.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ROW 1: Separate Row for Title and 3-dots Menu so high star counts never clip titles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Hold and drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 6.dp)
                )

                Text(
                    text = task.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_title_${task.id}")
                )

                // 3-dots Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("task_more_btn_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Task Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Task", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(nextPeriodLabel, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                menuExpanded = false
                                onMoveToNextPeriod()
                            },
                            modifier = Modifier.testTag("menu_move_next_${task.id}")
                        )
                        moveOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    menuExpanded = false
                                    option.onClick()
                                },
                                modifier = Modifier.testTag(option.testTag)
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete Task", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // ROW 2: Re-purposed down/up arrows for adding/removing stars, and squished stars row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Re-purposed Down Arrow: Decrement Star
                IconButton(
                    onClick = onDecrementStars,
                    enabled = taskWithProgress.starsEarned > 0,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("btn_task_decrement_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Remove Star",
                        tint = if (taskWithProgress.starsEarned > 0)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Squished Stars Bar
                StarRatingBar(
                    targetStars = task.targetStars,
                    earnedStars = taskWithProgress.starsEarned,
                    modifier = Modifier.weight(1f)
                )

                // Progress count
                Text(
                    text = "${taskWithProgress.starsEarned}/${task.targetStars}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (taskWithProgress.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Re-purposed Up Arrow: Increment Star
                IconButton(
                    onClick = onIncrementStars,
                    enabled = taskWithProgress.starsEarned < task.targetStars,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("btn_task_increment_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Add Star",
                        tint = if (taskWithProgress.starsEarned < task.targetStars)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
