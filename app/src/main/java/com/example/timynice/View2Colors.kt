package com.example.timynice

import androidx.compose.ui.graphics.Color

/** View2 activity list only — not used by global app theme. */
object View2Colors {
    val stripeDeep = Color(0xFFD8E4F0)
    val stripeLight = Color(0xFFE9F1FA)
    val dropLine = Color(0xFF4A6FA5)

    fun stripeForIndex(index: Int): Color =
        if (index % 2 == 0) stripeDeep else stripeLight
}
