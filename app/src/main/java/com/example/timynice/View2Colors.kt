package com.example.timynice

import androidx.compose.ui.graphics.Color
import com.example.timynice.ui.theme.TimyniceColors

/** View2 activity list stripes — synced with [TimyniceColors]. */
object View2Colors {
    val stripeDeep = TimyniceColors.StripeDeep
    val stripeLight = TimyniceColors.StripeLight
    val dropLine = TimyniceColors.DropLine

    fun stripeForIndex(index: Int): Color =
        if (index % 2 == 0) stripeDeep else stripeLight
}
