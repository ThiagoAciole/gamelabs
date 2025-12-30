package com.example.gamelabs

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.gamelabs.model.Console
import com.example.gamelabs.model.Game
import com.example.gamelabs.screen.Ps1EmulatorScreen
import com.example.gamelabs.screen.Ps2EmulatorScreen
import com.example.gamelabs.screen.PspEmulatorScreen
import com.example.gamelabs.screen.GamesScreen
import com.example.gamelabs.screen.HomeScreen
import com.example.gamelabs.ui.theme.GamelabsTheme
import com.example.gamelabs.util.FolderManager
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        hideSystemUI()

        checkPermissions()
        FolderManager.createExternalStructure()
        FolderManager.deploySystemFiles(this)

        setContent { GamelabsTheme { AppRoot() } }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = "package:$packageName".toUri()
                    startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        } else {
            requestPermissions(arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 101)
        }
    }
}

private enum class Screen { HOME, GAMES, EMULATOR }

@Composable
private fun AppRoot() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var selectedConsole by remember { mutableStateOf(Console.PS1) }
    var selectedGame by remember { mutableStateOf<Game?>(null) }

    when (screen) {
        Screen.HOME -> HomeScreen(
            onOpenConsole = { console ->
                selectedConsole = console
                screen = Screen.GAMES
            }
        )

        Screen.GAMES -> GamesScreen(
            console = selectedConsole,
            onBack = { screen = Screen.HOME },
            onPlayGame = { game ->
                selectedGame = game
                screen = Screen.EMULATOR
            }
        )

        Screen.EMULATOR -> {
            if (selectedGame != null) {
                when (selectedConsole) {
                    Console.PS1 -> {
                        Ps1EmulatorScreen(
                            game = selectedGame!!,
                            onClose = { screen = Screen.GAMES }
                        )
                    }
                    Console.PS2 -> {
                        Ps2EmulatorScreen(
                            onClose = { screen = Screen.GAMES }
                        )
                    }
                    Console.PSP -> {
                        PspEmulatorScreen(
                            onClose = { screen = Screen.GAMES }
                        )
                    }
                }
            }
        }
    }
}