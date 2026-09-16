package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StarEmptyDark
import com.example.ui.theme.StarGold
import com.example.ui.theme.StarSilver

@Composable
fun StarRatingBar(
    targetStars: Int,
    earnedStars: Int,
    onStarClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val totalStars = maxOf(targetStars, earnedStars, 1)
    val isGoalMet = earnedStars >= targetStars && targetStars > 0
    val activeStarColor by animateColorAsState(
        targetValue = if (isGoalMet) StarGold else StarSilver,
        animationSpec = spring(),
        label = "star_color_anim"
    )

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val availableWidth = maxWidth
        // Up to 11 stars are drawn full-size (18.dp).
        // If there are more than 11 stars, they maintain their full size and partially overlap instead of shrinking.
        val fullStarSize = 18.dp
        val spacing = if (totalStars <= 11) {
            if (availableWidth > 0.dp && totalStars > 1) {
                val naturalSpacing = (availableWidth - (fullStarSize * totalStars)) / (totalStars - 1)
                minOf(2.dp, naturalSpacing).coerceAtLeast(0.dp)
            } else {
                2.dp
            }
        } else {
            if (availableWidth > 0.dp && totalStars > 1) {
                ((availableWidth - (fullStarSize * totalStars)) / (totalStars - 1)).coerceAtLeast(-13.dp)
            } else {
                (-4).dp
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            for (i in 1..totalStars) {
                val isFilled = i <= earnedStars
                val isTarget = i <= targetStars

                Icon(
                    imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Star $i",
                    tint = when {
                        isFilled -> activeStarColor
                        isTarget -> StarEmptyDark.copy(alpha = 0.55f)
                        else -> StarEmptyDark.copy(alpha = 0.25f)
                    },
                    modifier = Modifier
                        .size(fullStarSize)
                        .testTag("star_item_$i")
                )
            }
        }
    }
}
