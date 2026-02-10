package com.applauncher.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val DarkBlue = Color(0xFF1a237e)
val LightBlue = Color(0xFF3949ab)
val AccentBlue = Color(0xFF5c6bc0)
val SurfaceDark = Color(0xFF1e1e2e)
val SurfaceLight = Color(0xFF2a2a3e)
val TextPrimary = Color(0xFFe0e0e0)
val TextSecondary = Color(0xFFa0a0a0)
val DragHighlight = Color(0xFF4CAF50)
val DropTarget = Color(0xFF2196F3)

val AppDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = LightBlue,
    onSecondary = Color.White,
    background = SurfaceDark,
    surface = SurfaceLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

val AppLightColorScheme = lightColorScheme(
    primary = DarkBlue,
    onPrimary = Color.White,
    secondary = LightBlue,
    onSecondary = Color.White,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onBackground = Color(0xFF1a1a1a),
    onSurface = Color(0xFF1a1a1a),
)
