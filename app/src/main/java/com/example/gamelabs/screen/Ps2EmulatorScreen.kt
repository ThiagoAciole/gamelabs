package com.example.gamelabs.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.gamelabs.model.Game
import com.example.gamelabs.service.OverlayService
import java.io.File

@Composable
fun Ps2EmulatorScreen(
    game: Game,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // 1. Verificar permissão de sobreposição (Overlay) para o botão X
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "Ative a permissão de sobreposição para o botão de fechar", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            context.startActivity(intent)
            onClose()
            return@LaunchedEffect
        }

        // 2. Iniciar o serviço do botão flutuante (X)
        val serviceIntent = Intent(context, OverlayService::class.java)
        context.startService(serviceIntent)

        // 3. Abrir o jogo diretamente no AetherSX2
        launchPs2GameDirectly(context, game, onClose)
    }

    // Tela de transição preta
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}
private fun launchPs2GameDirectly(context: Context, game: Game, onClose: () -> Unit) {
    try {
        val gameFile = File(game.uri.path ?: "")

        // Log de conferência
        android.util.Log.d("GamelabsPath", "Tentando lançamento direto para: ${gameFile.absolutePath}")

        // 1. Pegamos a Intent principal de abertura do emulador
        val intent = context.packageManager.getLaunchIntentForPackage("xyz.aethersx2.android")

        if (intent != null) {
            // 2. Adicionamos os "Extras" que forçam o boot do jogo
            // Testamos as duas chaves mais comuns para versões modificadas
            intent.putExtra("boot_game", gameFile.absolutePath)
            intent.putExtra("path", gameFile.absolutePath)

            // 3. Garantimos que ele abra em uma nova tarefa
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)
        } else {
            // Se cair aqui, o pacote xyz.aethersx2.android realmente não está instalado
            Toast.makeText(context, "Emulador AetherSX2 não encontrado!", Toast.LENGTH_LONG).show()
            onClose()
        }

    } catch (e: Exception) {
        android.util.Log.e("GamelabsPath", "Erro ao lançar: ${e.message}")
        onClose()
    }
}