package com.example.timynice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.timynice.FontScaleLock

private val TimyniceLightColorScheme = lightColorScheme(
    primary = TimyniceColors.NavyPrimary,
    onPrimary = TimyniceColors.NavyOnPrimary,
    primaryContainer = TimyniceColors.NavyPrimaryContainer,
    onPrimaryContainer = TimyniceColors.NavyOnPrimaryContainer,
    secondary = TimyniceColors.OnSecondaryContainer,
    onSecondary = TimyniceColors.NavyOnPrimary,
    secondaryContainer = TimyniceColors.SecondaryContainer,
    onSecondaryContainer = TimyniceColors.OnSecondaryContainer,
    tertiary = TimyniceColors.NavyPrimary,
    onTertiary = TimyniceColors.NavyOnPrimary,
    tertiaryContainer = TimyniceColors.StripeLight,
    onTertiaryContainer = TimyniceColors.NavyOnPrimaryContainer,
    background = TimyniceColors.Background,
    onBackground = TimyniceColors.OnBackground,
    surface = TimyniceColors.Surface,
    onSurface = TimyniceColors.OnSurface,
    surfaceVariant = TimyniceColors.SurfaceVariant,
    onSurfaceVariant = TimyniceColors.OnSurfaceVariant,
    outline = TimyniceColors.Outline,
    outlineVariant = TimyniceColors.OutlineVariant,
    errorContainer = TimyniceColors.ErrorContainer,
    onErrorContainer = TimyniceColors.OnErrorContainer,
)

private val TimyniceDarkColorScheme = darkColorScheme(
    primary = TimyniceColors.DarkPrimary,
    onPrimary = TimyniceColors.DarkOnPrimary,
    primaryContainer = TimyniceColors.DarkPrimaryContainer,
    onPrimaryContainer = TimyniceColors.DarkOnPrimaryContainer,
    secondaryContainer = TimyniceColors.DarkSurfaceVariant,
    onSecondaryContainer = TimyniceColors.DarkOnSurfaceVariant,
    tertiaryContainer = TimyniceColors.DarkSurfaceVariant,
    onTertiaryContainer = TimyniceColors.DarkOnPrimaryContainer,
    background = TimyniceColors.DarkBackground,
    onBackground = TimyniceColors.DarkOnSurface,
    surface = TimyniceColors.DarkSurface,
    onSurface = TimyniceColors.DarkOnSurface,
    surfaceVariant = TimyniceColors.DarkSurfaceVariant,
    onSurfaceVariant = TimyniceColors.DarkOnSurfaceVariant,
    outline = TimyniceColors.DarkOutline,
    outlineVariant = TimyniceColors.DarkOutlineVariant,
)

@Composable
fun TimyniceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor -> TimyniceLightColorScheme
        darkTheme -> TimyniceDarkColorScheme
        else -> TimyniceLightColorScheme
    }

    val density = LocalDensity.current
    FontScaleLock.ensureLocked(density.fontScale)
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = FontScaleLock.scale,
        ),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = TimyniceShapes,
            content = content,
        )
    }
}
