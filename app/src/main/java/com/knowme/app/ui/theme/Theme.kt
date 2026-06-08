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

private val LightColors = lightColorScheme(
    primary = KnowmeTeal,
    onPrimary = KnowmeSurface,
    primaryContainer = KnowmeTealLight,
    surface = KnowmeSurface,
    background = KnowmeSurface,
)

private val DarkColors = darkColorScheme(
    primary = KnowmeTealLight,
    onPrimary = KnowmeTealDark,
    primaryContainer = KnowmeTealDark,
    surface = KnowmeSurfaceDark,
    background = KnowmeSurfaceDark,
)

@Composable
fun KnowmeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Android 12+ 跟随壁纸动态取色（Material You）；老设备降级到品牌色
    dynamicColor: Boolean = true,
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
        content = content,
    )
}
