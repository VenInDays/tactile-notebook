package com.tactile.notebook.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ClayWarm,
    onPrimary = CreamLight,
    primaryContainer = Sandstone,
    onPrimaryContainer = InkBrown,
    secondary = MossMuted,
    onSecondary = CreamLight,
    secondaryContainer = ParchmentDark,
    onSecondaryContainer = SlateDeep,
    tertiary = RustAccent,
    onTertiary = CreamLight,
    background = Parchment,
    onBackground = SlateDeep,
    surface = CreamLight,
    onSurface = InkBrown,
    surfaceVariant = Sandstone,
    onSurfaceVariant = SlateMedium,
    outline = ClayDark,
    outlineVariant = ParchmentDark,
)

@Composable
fun TactileNotebookTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SlateDeep.toArgb()
            window.navigationBarColor = Parchment.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TactileTypography,
        content = content
    )
}
