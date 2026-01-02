package com.example.gamelabs

import android.content.Intent
import android.net.Uri
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
import com.example.gamelabs.screen.* // Importa todas as suas telas
import com.example.gamelabs.ui.theme.GamelabsTheme
import com.example.gamelabs.util.FolderManager
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Garante que o app ocupe a tela toda embaixo das barras de sistema
        WindowCompat.setDecorFitsSystemWindows(window, false)

        enableEdgeToEdge()
        hideSystemUI()

        if (savedInstanceState == null) {
            checkPermissions()
            FolderManager.createExternalStructure()
            FolderManager.deploySystemFiles(this)
        }

        setContent { GamelabsTheme { AppRoot() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    addCategory("android.intent.category.DEFAULT")
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivity(intent)
        }
    }
}

private enum class Screen { HOME, GAMES, EMULATOR, SETTINGS }
@Composable
private fun AppRoot() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var selectedConsole by remember { mutableStateOf(Console.PS1) }
    var selectedGame by remember { mutableStateOf<Game?>(null) }

    // Gerencia o fluxo de telas
    when (screen) {
        Screen.HOME -> HomeScreen(
            onOpenConsole = { console ->
                selectedConsole = console
                screen = Screen.GAMES
            },
            onOpenSettings = { screen = Screen.SETTINGS }
        )

        Screen.GAMES -> {
            if (selectedConsole == Console.ANDROID) {
                AndroidGamesScreen(
                    onBack = { screen = Screen.HOME },
                    onPlayGame = { game ->
                        selectedGame = game
                        screen = Screen.EMULATOR
                    }
                )
            } else {
                GamesScreen(
                    console = selectedConsole,
                    onBack = { screen = Screen.HOME },
                    onPlayGame = { game ->
                        selectedGame = game
                        screen = Screen.EMULATOR
                    }
                )
            }
        }

        Screen.EMULATOR -> {
            val game = selectedGame ?: return

            // Lógica de fechamento: Sempre volta para a lista de jogos do console atual
            val closeAction = { screen = Screen.GAMES }

            when (selectedConsole) {
                Console.PS1 -> Ps1EmulatorScreen(game, onClose = closeAction)
                Console.PS2 -> Ps2EmulatorScreen(game, onClose = closeAction)
                Console.PSP -> PspEmulatorScreen(game, onClose = closeAction)
                Console.ANDROID -> AndroidLauncherScreen(game, onClose = closeAction)
            }
        }
        Screen.SETTINGS -> {
            SettingsScreen(
                onBack = { screen = Screen.HOME }
            )
        }
    }
}