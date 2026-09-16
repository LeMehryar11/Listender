package com.example.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TaskScope
import com.example.ui.components.TrendLineGraph
import com.example.ui.screens.main.AppBottomNav
import com.example.ui.screens.main.NavTab
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.StarGold
import com.example.ui.theme.SuccessGreen

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    showBottomNav: Boolean = true,
    modifier: Modifier = Modifier
) {
    val selectedTimeframe by viewModel.selectedTrendTimeframe.collectAsStateWithLifecycle()
    val analytics by viewModel.overallAnalytics.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                AppBottomNav(
                    currentTab = NavTab.REPORTS,
                    onTasksClick = onNavigateToTasks,
                    onReportsClick = { /* Already on Reports */ },
                    onSettingsClick = onNavigateToSettings
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.testTag("reports_screen")
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Interactive Line Graph Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "COMPLETION TREND",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Progress across ${selectedTimeframe.displayName.lowercase()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Timeframe Switcher: Days / Weeks / Months / Years
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            TrendTimeframe.entries.forEach { timeframe ->
                                val isSelected = timeframe == selectedTimeframe
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BrandCrimson else Color.Transparent)
                                        .clickable { viewModel.setTrendTimeframe(timeframe) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = timeframe.displayName,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    val currentTrendData = when (selectedTimeframe) {
                        TrendTimeframe.DAYS -> analytics.dailyTrend
                        TrendTimeframe.WEEKS -> analytics.weeklyTrend
                        TrendTimeframe.MONTHS -> analytics.monthlyTrend
                        TrendTimeframe.YEARS -> analytics.yearlyTrend
                    }

                    TrendLineGraph(
                        points = currentTrendData,
                        rangeLabel = analytics.currentTrendRangeLabel,
                        isAtCurrent = analytics.isAtCurrentOffset,
                        onNavigatePrevious = { viewModel.shiftTrendOffset(-1) },
                        onNavigateNext = { viewModel.shiftTrendOffset(1) },
                        onResetCurrent = { viewModel.resetTrendOffset() }
                    )
                }
            }

            // Scope Breakdown Overview
            item {
                SectionHeader(title = "CURRENT SCOPE BREAKDOWN")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    analytics.scopeStats.forEach { stat ->
                        ScopeStatCard(stat = stat)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ScopeStatCard(stat: ScopeStat) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(BrandCrimson)
                    )
                    Text(
                        text = stat.scope.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${stat.percentage}% Complete",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (stat.percentage == 100) SuccessGreen else MaterialTheme.colorScheme.onSurface
                )
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stat.percentage / 100f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (stat.percentage == 100) SuccessGreen else BrandCrimson)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stat.completedTasks}/${stat.totalTasks} Tasks Completed",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${stat.earnedStars}/${stat.targetStars} Stars Earned ★",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (stat.earnedStars >= stat.targetStars && stat.targetStars > 0) StarGold else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}
