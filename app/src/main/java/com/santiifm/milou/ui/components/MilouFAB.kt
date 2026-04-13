package com.santiifm.milou.ui.components

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.santiifm.milou.R
import com.santiifm.milou.ui.navigation.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilouFAB(
    currentRoute: String,
    navController: NavController,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {

    val fabSize = 64.dp
    val expandedFabWidth by animateDpAsState(
        targetValue = if (isExpanded) 160.dp else fabSize,
        animationSpec = spring(dampingRatio = 1f)
    )
    val expandedFabHeight by animateDpAsState(
        targetValue = if (isExpanded) 58.dp else fabSize,
        animationSpec = spring(dampingRatio = 1f)
    )

    Column {
        Box(
            modifier = modifier
                .offset(y = (-15).dp)
                .size(
                    width = expandedFabWidth,
                    height = animateDpAsState(
                        if (isExpanded) 180.dp else 0.dp,
                        animationSpec = spring(dampingRatio = 1f)
                    ).value
                )
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            if (isExpanded) {
                Column(
                    modifier = modifier.padding(16.dp)
                ) {
                    NavRoutes.allRoutes
                        .filter { it.route != currentRoute }
                        .forEach { route ->
                            Row(
                                modifier = modifier
                                    .clickable {
                                        navController.navigate(route.route)
                                        onToggle()
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = route.label,
                                    modifier = modifier.padding(start = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                }
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            modifier = modifier
                .width(expandedFabWidth)
                .height(expandedFabHeight),
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.fab_menu),
                modifier = modifier
                    .size(24.dp)
                    .offset(x = animateDpAsState(
                        if (isExpanded) -50.dp else 0.dp,
                        animationSpec = spring(dampingRatio = 3f)
                    ).value)
            )

            Text(
                text = stringResource(R.string.fab_menu),
                softWrap = false,
                modifier = modifier
                    .offset(x = animateDpAsState(
                        if (isExpanded) -10.dp else 20.dp,
                        animationSpec = spring(dampingRatio = 3f)
                    ).value)
                    .alpha(
                        animateFloatAsState(
                            targetValue = if (isExpanded) 1f else 0f,
                            animationSpec = tween(
                                durationMillis = if (isExpanded) 350 else 100,
                                delayMillis = if (isExpanded) 100 else 0,
                                easing = EaseIn
                            )
                        ).value
                    )
            )
        }
    }
}
