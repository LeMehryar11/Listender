package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.model.ThemeMode
import com.example.data.repository.TaskRepositoryImpl
import com.example.ui.screens.main.AppBottomNav
import com.example.ui.screens.main.NavTab
import com.example.ui.screens.main.MainScreen
import com.example.ui.screens.main.MainViewModel
import com.example.ui.screens.main.MainViewModelFactory
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.reports.ReportsViewModel
import com.example.ui.screens.reports.ReportsViewModelFactory
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.ListenderTheme
import kotlinx.coroutines.launch

enum class AppDestination {
    TASKS,
    REPORTS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = TaskRepositoryImpl(database)

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(repository)
            )
            val reportsViewModel: ReportsViewModel = viewModel(
                factory = ReportsViewModelFactory(repository)
            )

            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }

            ListenderTheme(darkTheme = useDarkTheme) {
                val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
                val coroutineScope = rememberCoroutineScope()

                Scaffold(
                    bottomBar = {
                        val currentTab = when (pagerState.currentPage) {
                            0 -> NavTab.TASKS
                            1 -> NavTab.REPORTS
                            else -> NavTab.SETTINGS
                        }
                        AppBottomNav(
                            currentTab = currentTab,
                            onTasksClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            },
                            onReportsClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onSettingsClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) { page ->
                        when (page) {
                            0 -> {
                                MainScreen(
                                    mainViewModel = mainViewModel,
                                    reportsViewModel = reportsViewModel,
                                    onNavigateToReports = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                    onNavigateToSettings = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                    },
                                    showBottomNav = false,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            1 -> {
                                ReportsScreen(
                                    viewModel = reportsViewModel,
                                    onNavigateToTasks = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                    },
                                    onNavigateToSettings = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                    },
                                    showBottomNav = false,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            2 -> {
                                SettingsScreen(
                                    mainViewModel = mainViewModel,
                                    reportsViewModel = reportsViewModel,
                                    onNavigateToTasks = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                    },
                                    onNavigateToReports = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                    showBottomNav = false,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
