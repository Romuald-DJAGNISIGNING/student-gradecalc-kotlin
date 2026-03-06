package com.romualdsigning.studentgradecalc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF174A7A),
    secondary = Color(0xFFB45F06),
    tertiary = Color(0xFF2A7A4F),
    background = Color(0xFFF5F8FC),
    surface = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF99C2F2),
    secondary = Color(0xFFF0B466),
    tertiary = Color(0xFF9FDAB9),
)

@Composable
fun StudentGradeCalcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}

