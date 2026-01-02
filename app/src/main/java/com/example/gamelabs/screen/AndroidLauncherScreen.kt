package com.example.gamelabs.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.gamelabs.model.Game
import com.example.gamelabs.service.OverlayService

@Composable
fun AndroidLauncherScreen(
    game: Game,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // Extrai o nome do pacote da nossa URI customizada
        val packageName = game.uri.host

        if (packageName != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                // Ativa o botão flutuante para poder fechar o processo depois
                context.startService(Intent(context, OverlayService::class.java))
                context.startActivity(intent)
            }
        }

        // Retorna o GameLabs para a lista de jogos imediatamente
        onClose()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
}