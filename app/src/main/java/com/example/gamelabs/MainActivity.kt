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
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.gamelabs.model.Console
import com.example.gamelabs.model.Game
import com.example.gamelabs.screen.*
import com.example.gamelabs.ui.theme.GamelabsTheme
import com.example.gamelabs.util.FolderManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configura tela cheia (atrás das barras de sistema)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        hideSystemUI()

        // 2. Inicializações únicas (apenas quando o app abre do zero)
        if (savedInstanceState == null) {
            checkPermissions()

            // Cria pastas Games/PS1/MemoryCards, Covers, etc.
            FolderManager.createExternalStructure()

            // Prepara arquivos de sistema se necessário
            FolderManager.deploySystemFiles(this)

            // --- IMPORTANTE: Limpa lixo de sessões anteriores ---
            // Se o app crashou ou foi morto pelo sistema enquanto jogava,
            // isso libera o espaço da ROM temporária agora.
            FolderManager.clearOldCache(this)
        }

        setContent {
            GamelabsTheme {
                AppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Garante que o modo imersivo volte se o usuário puxar a barra e soltar
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun checkPermissions() {
        // Permissão de Arquivos (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    addCategory("android.intent.category.DEFAULT")
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
            }
        }

        // Permissão de Sobreposição (Overlay) - Útil para controles virtuais sobre outros apps
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivity(intent)
        }
    }
}

// Enum para controle de navegação simples
private enum class Screen { HOME, GAMES, EMULATOR, SETTINGS }

@Composable
private fun AppRoot() {
    // Estados de navegação
    var screen by remember { mutableStateOf(Screen.HOME) }
    var selectedConsole by remember { mutableStateOf(Console.PS1) }
    var selectedGame by remember { mutableStateOf<Game?>(null) }

    when (screen) {
        Screen.HOME -> {
            HomeScreen(
                onOpenConsole = { console ->
                    selectedConsole = console
                    screen = Screen.GAMES
                },
                // Certifique-se que sua HomeScreen aceita este parâmetro,
                // caso contrário remova ou ajuste a HomeScreen conforme conversas anteriores.
                onOpenSettings = {
                    screen = Screen.SETTINGS
                }
            )
        }

        Screen.GAMES -> {
            // Lógica específica para Android vs Consoles Retro
            if (selectedConsole == Console.ANDROID) {
                // Supondo que AndroidGamesScreen exista no seu projeto
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

            // Ao fechar o emulador, volta para a lista de jogos
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