package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.reports.TrendGraphPoint
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.SuccessGreen

@Composable
fun TrendLineGraph(
    points: List<TrendGraphPoint>,
    rangeLabel: String = "",
    isAtCurrent: Boolean = true,
    onNavigatePrevious: () -> Unit = {},
    onNavigateNext: () -> Unit = {},
    onResetCurrent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 400))
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trend_line_graph_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Navigation & Window Range Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onNavigatePrevious() }
                        .testTag("btn_trend_prev"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous timeframe",
                        tint = onSurfaceColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = rangeLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor,
                        textAlign = TextAlign.Center
                    )
                    if (!isAtCurrent) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onResetCurrent() }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Return",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onNavigateNext() }
                        .testTag("btn_trend_next"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next timeframe",
                        tint = onSurfaceColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Selected Point Details Banner (Fixed contrast in dark mode)
            val activePoint = selectedIndex?.let { points.getOrNull(it) } ?: points.lastOrNull()
            if (activePoint != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${activePoint.label} (${activePoint.sublabel})",
                                style = MaterialTheme.typography.labelMedium,
                                color = onSurfaceVariant
                            )
                            Text(
                                text = "${activePoint.completionPercentage}% Completion",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (activePoint.completionPercentage == 100) SuccessGreen else onSurfaceColor
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "★ ${activePoint.earnedStars} / ${activePoint.targetStars} stars",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = onSurfaceColor
                            )
                        }
                    }
                }
            }

            // Canvas Graph with Drag & Tap Gesture Support
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("trend_line_canvas")
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragAccumulator = 0f },
                                onDragEnd = { dragAccumulator = 0f },
                                onDragCancel = { dragAccumulator = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragAccumulator += dragAmount
                                    val stepDistance = 45f
                                    while (dragAccumulator > stepDistance) {
                                        onNavigatePrevious()
                                        dragAccumulator -= stepDistance
                                    }
                                    while (dragAccumulator < -stepDistance) {
                                        onNavigateNext()
                                        dragAccumulator += stepDistance
                                    }
                                }
                            )
                        }
                        .pointerInput(points) {
                            detectTapGestures { tapOffset ->
                                val spacing = size.width / (points.size.coerceAtLeast(2) - 1)
                                val index = ((tapOffset.x + (spacing / 2)) / spacing).toInt()
                                if (index in points.indices) {
                                    selectedIndex = index
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingBottom = 20.dp.toPx()
                    val paddingTop = 12.dp.toPx()
                    val usableHeight = height - paddingBottom - paddingTop

                    // Draw subtle horizontal gridlines (0%, 50%, 100%)
                    val gridLevels = listOf(0f, 0.5f, 1f)
                    gridLevels.forEach { level ->
                        val y = paddingTop + usableHeight * (1f - level)
                        drawLine(
                            color = surfaceVariant,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                    }

                    if (points.size < 2) {
                        val pt = points[0]
                        val x = width / 2f
                        val y = paddingTop + usableHeight * (1f - (pt.completionPercentage / 100f).coerceIn(0f, 1f))
                        drawCircle(
                            color = primaryColor,
                            radius = 6.dp.toPx(),
                            center = Offset(x, y)
                        )
                        return@Canvas
                    }

                    val pointOffsets = mutableListOf<Offset>()
                    val spacing = width / (points.size - 1)

                    points.forEachIndexed { i, pt ->
                        val x = i * spacing
                        val ratio = (pt.completionPercentage / 100f).coerceIn(0f, 1f)
                        val y = paddingTop + usableHeight * (1f - ratio)
                        pointOffsets.add(Offset(x, y))
                    }

                    // Build smooth cubic bezier curve
                    val linePath = Path()
                    val fillPath = Path()

                    fillPath.moveTo(0f, height - paddingBottom)

                    pointOffsets.forEachIndexed { i, offset ->
                        val animatedY = (height - paddingBottom) - ((height - paddingBottom - offset.y) * animatedProgress.value)
                        val currentOffset = Offset(offset.x, animatedY)

                        if (i == 0) {
                            linePath.moveTo(currentOffset.x, currentOffset.y)
                            fillPath.lineTo(currentOffset.x, currentOffset.y)
                        } else {
                            val prevOffset = pointOffsets[i - 1]
                            val prevAnimatedY = (height - paddingBottom) - ((height - paddingBottom - prevOffset.y) * animatedProgress.value)

                            val cX1 = prevOffset.x + (currentOffset.x - prevOffset.x) / 2
                            val cY1 = prevAnimatedY
                            val cX2 = prevOffset.x + (currentOffset.x - prevOffset.x) / 2
                            val cY2 = animatedY

                            linePath.cubicTo(cX1, cY1, cX2, cY2, currentOffset.x, currentOffset.y)
                            fillPath.cubicTo(cX1, cY1, cX2, cY2, currentOffset.x, currentOffset.y)
                        }
                    }

                    fillPath.lineTo(width, height - paddingBottom)
                    fillPath.close()

                    // Draw gradient fill under line
                    val gradient = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.3f * animatedProgress.value),
                            primaryColor.copy(alpha = 0.02f)
                        ),
                        startY = paddingTop,
                        endY = height - paddingBottom
                    )
                    drawPath(path = fillPath, brush = gradient)

                    // Draw line
                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // Draw data nodes
                    pointOffsets.forEachIndexed { i, offset ->
                        val animatedY = (height - paddingBottom) - ((height - paddingBottom - offset.y) * animatedProgress.value)
                        val isSelected = selectedIndex == i

                        // Outer halo
                        drawCircle(
                            color = surfaceColor,
                            radius = (if (isSelected) 7.dp else 4.5.dp).toPx(),
                            center = Offset(offset.x, animatedY)
                        )
                        // Inner circle
                        drawCircle(
                            color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.85f),
                            radius = (if (isSelected) 5.dp else 3.dp).toPx(),
                            center = Offset(offset.x, animatedY)
                        )
                    }
                }
            }

            // X-Axis Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEachIndexed { index, point ->
                    val isSelected = selectedIndex == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) primaryColor else onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = point.sublabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

