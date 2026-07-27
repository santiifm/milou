package com.santiifm.milou.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiifm.milou.domain.model.FilterMode
import com.santiifm.milou.ui.theme.LightningBlue
import com.santiifm.milou.ui.theme.PurpleAccent

@Composable
fun FilterModeToggle(
    currentMode: FilterMode,
    onToggle: () -> Unit,
    isConsoleFilter: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isConsoleFilter) {
        PurpleAccent.copy(alpha = 0.8f)
    } else {
        LightningBlue.copy(alpha = 0.8f)
    }
    
    val textColor = MaterialTheme.colorScheme.onSurface
    
    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (currentMode == FilterMode.AND) "AND" else "OR",
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "↔",
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


