package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.TaskScope
import com.example.domain.model.PeriodSummary
import com.example.ui.components.PeriodPerformanceBanner
import com.example.ui.theme.ListenderTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun banner_screenshot() {
    val sampleSummary = PeriodSummary(
      scope = TaskScope.DAILY,
      periodKey = "2026-08-29",
      displayLabel = "Today",
      totalTasks = 3,
      completedTasks = 2,
      totalTargetStars = 5,
      totalEarnedStars = 3,
      completionPercentage = 60
    )

    composeTestRule.setContent {
      ListenderTheme {
        PeriodPerformanceBanner(summary = sampleSummary)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/banner.png")
  }
}
