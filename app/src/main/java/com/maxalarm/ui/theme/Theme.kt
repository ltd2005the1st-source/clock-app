package com.maxalarm.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Fallback palette (warm orange seed) for phones without Material You dynamic color.
private val Light = lightColorScheme(
    primary = Color(0xFF8A2C0D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF390B00),
    secondary = Color(0xFF77574C),
    secondaryContainer = Color(0xFFFFDBD0),
    onSecondaryContainer = Color(0xFF2C160E),
    tertiary = Color(0xFF6B5E2F),
    tertiaryContainer = Color(0xFFF4E2A7),
    onTertiaryContainer = Color(0xFF221B00),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB59E),
    onPrimary = Color(0xFF5E1700),
    primaryContainer = Color(0xFF7C2E12),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFE7BDB0),
    secondaryContainer = Color(0xFF5D4036),
    onSecondaryContainer = Color(0xFFFFDBD0),
    tertiary = Color(0xFFD8C68D),
    tertiaryContainer = Color(0xFF524619),
    onTertiaryContainer = Color(0xFFF4E2A7),
    background = Color(0xFF1A110D),
    surface = Color(0xFF1A110D),
)

@Composable
fun MaxAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = colors, content = content)
}
