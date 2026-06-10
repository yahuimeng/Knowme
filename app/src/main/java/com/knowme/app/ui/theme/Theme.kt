package com.knowme.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// 品牌兜底配色（仅在不支持动态取色的老设备使用）
private val LightColors = lightColorScheme(
    primary = InkTeal,
    onPrimary = InkTealOn,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
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
    // 固定"灰底白卡"品牌配色（不跟随壁纸）：灰画布让白卡浮起来，最清爽舒服
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = KnowmeTypography,
        shapes = KnowmeShapes,
        content = content,
    )
}
