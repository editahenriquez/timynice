package com.example.timynice.ui.theme

import androidx.compose.ui.graphics.Color

/** Brand palette — keep in sync with [com.example.timynice.View2Colors]. */
object TimyniceColors {
    val NavyPrimary = Color(0xFF4A6FA5)
    val NavyOnPrimary = Color(0xFFFFFFFF)
    val NavyPrimaryContainer = Color(0xFFD8E4F0)
    val NavyOnPrimaryContainer = Color(0xFF2A4568)

    val StripeDeep = Color(0xFFD8E4F0)
    val StripeLight = Color(0xFFE9F1FA)
    val DropLine = Color(0xFF4A6FA5)

    val Background = Color(0xFFF6F8FB)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFE9F1FA)

    val OnBackground = Color(0xFF1A1C1E)
    val OnSurface = Color(0xFF1A1C1E)
    val OnSurfaceVariant = Color(0xFF5C6570)

    val Outline = Color(0xFFB8C5D4)
    val OutlineVariant = Color(0xFFDCE4ED)

    val SecondaryContainer = Color(0xFFE3ECF5)
    val OnSecondaryContainer = Color(0xFF3D5678)

    val ErrorContainer = Color(0xFFF9DEDC)
    val OnErrorContainer = Color(0xFF8C1D18)

    // Dark
    val DarkBackground = Color(0xFF121820)
    val DarkSurface = Color(0xFF1A2230)
    val DarkSurfaceVariant = Color(0xFF243044)
    val DarkPrimary = Color(0xFF9BB8D9)
    val DarkOnPrimary = Color(0xFF1A2A40)
    val DarkPrimaryContainer = Color(0xFF2E4563)
    val DarkOnPrimaryContainer = Color(0xFFD8E4F0)
    val DarkOnSurface = Color(0xFFE8EDF2)
    val DarkOnSurfaceVariant = Color(0xFFB0BAC6)
    val DarkOutline = Color(0xFF4A5A6E)
    val DarkOutlineVariant = Color(0xFF354558)
}

// Legacy aliases (unused by theme; kept for compatibility if referenced elsewhere)
val Purple80 = TimyniceColors.DarkPrimary
val PurpleGrey80 = TimyniceColors.DarkOnSurfaceVariant
val Pink80 = TimyniceColors.DarkPrimaryContainer
val Purple40 = TimyniceColors.NavyPrimary
val PurpleGrey40 = TimyniceColors.OnSurfaceVariant
val Pink40 = TimyniceColors.NavyOnPrimaryContainer
