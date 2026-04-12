package com.santiifm.milou.util

import androidx.compose.ui.graphics.Color
import com.santiifm.milou.R
import com.santiifm.milou.ui.theme.LightText
import com.santiifm.milou.ui.theme.LightningBlue
import com.santiifm.milou.ui.theme.NeonGreen
import com.santiifm.milou.ui.theme.NeonRed
import com.santiifm.milou.ui.theme.WarmOrange

fun iconColorFor(icon: Int): Color = when(icon) {
    R.drawable.ic_arrow_down -> LightningBlue
    R.drawable.ic_check -> NeonGreen
    R.drawable.ic_error -> NeonRed
    R.drawable.ic_stop -> WarmOrange
    R.drawable.ic_arrow_up -> WarmOrange
    R.drawable.ic_trash -> NeonRed
    R.drawable.ic_folder -> LightningBlue
    else -> LightText
}
