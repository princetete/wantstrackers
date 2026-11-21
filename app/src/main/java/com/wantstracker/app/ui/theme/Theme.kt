package com.wantstracker.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.wantstracker.app.domain.Status

private val LightColors = lightColorScheme(
    primary = Color(0xFF5648D8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E0FF),
    onPrimaryContainer = Color(0xFF160062),
    secondary = Color(0xFF0E8F8C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDF3EF),
    onSecondaryContainer = Color(0xFF00201F),
    tertiary = Color(0xFFB45F06),
    background = Color(0xFFFBF9FF),
    onBackground = Color(0xFF1B1B20),
    surface = Color(0xFFFBF9FF),
    onSurface = Color(0xFF1B1B20),
    surfaceVariant = Color(0xFFE5E1EC),
    onSurfaceVariant = Color(0xFF47464F),
    outline = Color(0xFF787680),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFF0EDF6),
    surfaceContainerHigh = Color(0xFFEAE7F1),
    surfaceContainerHighest = Color(0xFFE4E1EB)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC6BFFF),
    onPrimary = Color(0xFF2A1A9E),
    primaryContainer = Color(0xFF4033C0),
    onPrimaryContainer = Color(0xFFE4E0FF),
    secondary = Color(0xFF5ADAD4),
    onSecondary = Color(0xFF003735),
    secondaryContainer = Color(0xFF00504D),
    onSecondaryContainer = Color(0xFFCDF3EF),
    tertiary = Color(0xFFFFB778),
    background = Color(0xFF131218),
    onBackground = Color(0xFFE5E1E9),
    surface = Color(0xFF131218),
    onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF48454E),
    onSurfaceVariant = Color(0xFFC9C5D0),
    outline = Color(0xFF938F99),
    surfaceContainerLowest = Color(0xFF0E0D13),
    surfaceContainerLow = Color(0xFF1B1A21),
    surfaceContainer = Color(0xFF1F1E25),
    surfaceContainerHigh = Color(0xFF2A292F),
    surfaceContainerHighest = Color(0xFF35343A)
)

fun statusColor(s: Status): Color = when (s) {
    Status.NOT_STARTED -> Color(0xFF8E8E9A)
    Status.IN_PROGRESS -> Color(0xFF3B82F6)
    Status.BOUGHT -> Color(0xFF22A06B)
    Status.FAILED -> Color(0xFFE5484D)
}

@Composable
fun WantsTheme(dark: Boolean, content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
