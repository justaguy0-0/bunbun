package com.example.bunbun.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val ColorOnGreen = androidx.compose.ui.graphics.Color(0xFFCBFFD6)
private val AmberContainer = androidx.compose.ui.graphics.Color(0xFF332A17)
private val ErrorContainer = androidx.compose.ui.graphics.Color(0xFF321B1B)

private val BunbunColors = darkColorScheme(
    primary = TerminalGreen,
    onPrimary = TerminalBlack,
    primaryContainer = TerminalGreenDim,
    onPrimaryContainer = ColorOnGreen,
    secondary = TerminalAmber,
    onSecondary = TerminalBlack,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = TerminalAmber,
    background = TerminalBlack,
    onBackground = TerminalText,
    surface = TerminalSurface,
    onSurface = TerminalText,
    surfaceVariant = TerminalSurfaceHigh,
    onSurfaceVariant = TerminalTextMuted,
    outline = TerminalOutline,
    outlineVariant = TerminalOutline.copy(alpha = 0.55f),
    error = TerminalRed,
    onError = TerminalBlack,
    errorContainer = ErrorContainer,
    onErrorContainer = TerminalRed,
)

private val BunbunShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(5.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

@Composable
fun BunbunTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = TerminalBlack.toArgb()
            window.navigationBarColor = TerminalBlack.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = BunbunColors,
        typography = BunbunTypography,
        shapes = BunbunShapes,
        content = content,
    )
}
