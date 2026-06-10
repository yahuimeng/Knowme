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
    // 补齐 secondary/tertiary，避免回退到 Material 默认的淡紫/粉色（导航选中、chip 用这些槽）
    secondary = InkTeal,
    onSecondary = InkTealOn,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnTealContainer,
    tertiary = InkTeal,
    onTertiary = InkTealOn,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealContainer,
    background = PaperBg,
    onBackground = InkText,
    surface = PaperSurface,
    onSurface = InkText,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariantWarm,
    // 关键：Card 实际用 surfaceContainer* 槽，必须显式设为纯白/中性，否则回退到默认带紫粉调的色（卡片发红）
    surfaceTint = InkTeal,
    surfaceBright = PaperSurface,
    surfaceDim = PaperBg,
    // 所有 container 槽都设白色：不管 Card 取哪个槽，卡片都是纯白（灰色留给背景）
    surfaceContainerLowest = PaperSurface,
    surfaceContainerLow = PaperSurface,
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurface,
    surfaceContainerHighest = PaperSurface,
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
    secondary = InkTealDark,
    onSecondary = OnInkTealDark,
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = OnTealContainerDark,
    tertiary = InkTealDark,
    onTertiary = OnInkTealDark,
    tertiaryContainer = TealContainerDark,
    onTertiaryContainer = OnTealContainerDark,
    surface = PaperSurfaceDark,
    onSurface = InkTextDark,
    surfaceVariant = PaperSurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantWarmDark,
    surfaceTint = InkTealDark,
    surfaceBright = PaperSurfaceVariantDark,
    surfaceDim = PaperBgDark,
    // 所有 container 槽统一为卡片色，卡片一致地浮在更暗的背景上
    surfaceContainerLowest = PaperSurfaceDark,
    surfaceContainerLow = PaperSurfaceDark,
    surfaceContainer = PaperSurfaceDark,
    surfaceContainerHigh = PaperSurfaceDark,
    surfaceContainerHighest = PaperSurfaceDark,
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
