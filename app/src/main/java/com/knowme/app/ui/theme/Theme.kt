package com.knowme.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = InkTeal,
    onPrimary = InkTealOn,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = InkTeal,
    onSecondary = InkTealOn,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnTealContainer,
    background = PaperBg,
    onBackground = InkText,
    surface = PaperSurface,
    onSurface = InkText,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariantWarm,
    outline = WarmOutline,
    error = WarmError,
    onError = OnWarmError,
    errorContainer = WarmErrorContainer,
    onErrorContainer = OnWarmErrorContainer,
)

private val DarkColors = darkColorScheme(
    primary = InkTealDark,
    onPrimary = OnInkTealDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = OnTealContainerDark,
    secondary = InkTealDark,
    onSecondary = OnInkTealDark,
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = OnTealContainerDark,
    background = PaperBgDark,
    onBackground = InkTextDark,
    surface = PaperSurfaceDark,
    onSurface = InkTextDark,
    surfaceVariant = PaperSurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantWarmDark,
    outline = WarmOutlineDark,
    error = WarmErrorDark,
    onError = OnWarmErrorDark,
    errorContainer = WarmErrorContainerDark,
    onErrorContainer = OnWarmErrorContainerDark,
)

@Composable
fun KnowmeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // 固定品牌配色：不再跟随系统壁纸（动态取色），才有自己的"暖纸感"识别
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = KnowmeTypography,
        shapes = KnowmeShapes,
        content = content,
    )
}
