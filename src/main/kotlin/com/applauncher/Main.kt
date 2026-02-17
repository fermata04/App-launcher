package com.applauncher

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.applauncher.model.AppLauncherState
import com.applauncher.ui.AppDarkColorScheme
import com.applauncher.ui.MainScreen
import com.applauncher.ui.setupWindowDropTarget

fun main() = application {
    val state = remember { AppLauncherState() }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "App Launcher",
        state = rememberWindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(500.dp, 700.dp)
        )
    ) {
        // ウィンドウにドロップターゲットを設定
        LaunchedEffect(Unit) {
            setupWindowDropTarget(window, state)
        }
        
        MaterialTheme(
            colorScheme = AppDarkColorScheme
        ) {
            MainScreen(state, onExitApplication = ::exitApplication)
        }
    }
}
