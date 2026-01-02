package com.example.gamelabs.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
fun PspEmulatorScreen(
    game: Game, // Agora recebe o objeto Game para saber qual ISO abrir
    onClose: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // 1. Verificar se o app tem permissão de sobreposição (Overlay)
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "Ative a permissão de sobreposição para o botão de fechar", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            context.startActivity(intent)
            onClose() // Volta para garantir que o usuário tente novamente após dar a permissão
            return@LaunchedEffect
        }

        // 2. Iniciar o serviço do botão flutuante (o seu X minimalista)
        val serviceIntent = Intent(context, OverlayService::class.java)
        context.startService(serviceIntent)

        // 3. Abrir o jogo diretamente no PPSSPP
        launchPspGameDirectly(context, game, onClose)
    }

    // Tela de transição preta enquanto o app externo abre
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

private fun launchPspGameDirectly(context: Context, game: Game, onClose: () -> Unit) {
    try {
        // 1. Extrair o arquivo físico da URI do Scanner
        val gameFile = if (game.uri.scheme == "file") {
            File(game.uri.path ?: "")
        } else {
            // Se for do Cartão SD (SAF), precisamos tratar de forma diferente
            // Por enquanto, assumindo que está na memória interna/File
            File(game.uri.path ?: "")
        }

        if (!gameFile.exists()) {
            Toast.makeText(context, "ISO não encontrada no caminho: ${gameFile.absolutePath}", Toast.LENGTH_LONG).show()
            onClose()
            return
        }

        // 2. Gerar URI via FileProvider
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            gameFile
        )

        // 3. Criar a Intent
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/x-psp-rom")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // 4. Tentar abrir o PPSSPP oficial (Free)
        intent.setPackage("org.ppsspp.ppsspp")

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // 5. Se falhar o Free, tenta o Gold
            try {
                intent.setPackage("org.ppsspp.ppssppgold")
                context.startActivity(intent)
            } catch (e2: Exception) {
                // 6. Se nenhum estiver instalado
                Toast.makeText(context, "Instale o emulador PPSSPP oficial para jogar!", Toast.LENGTH_LONG).show()
                onClose()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        onClose()
    }
}