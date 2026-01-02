package com.example.gamelabs.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.gamelabs.model.Console
import com.example.gamelabs.model.Game
import com.example.gamelabs.util.FolderManager
import java.io.File
import androidx.core.net.toUri

/**
 * GameScanner: Centraliza a leitura de jogos e salvamento de capas.
 * Ajustado para suportar emuladores (arquivos) e Android (pacotes).
 */

// --- FUNÇÕES PÚBLICAS ---

fun scanGames(context: Context, console: Console): List<Game> {
    // Para Android, usamos o scanner de apps que lida com pacotes instalados
    if (console == Console.ANDROID) {
        return AndroidGameScanner.scanAllInstalledApps(context)
    }

    val prefs = context.getSharedPreferences("game_paths", Context.MODE_PRIVATE)
    val customUriString = prefs.getString(console.name, null)

    return if (customUriString != null) {
        scanCustomUri(context, customUriString.toUri(), console)
    } else {
        scanDefaultFolder(console)
    }
}

/**
 * Salva a capa de um jogo.
 * Útil para vincular imagens customizadas via package name (Android) ou nome do arquivo (Emuladores).
 */
fun saveGameCover(context: Context, console: Console, game: Game, sourceUri: Uri): Boolean {
    return try {
        val consoleName = if (console == Console.ANDROID) "ANDROID" else console.name
        val rootDir = FolderManager.getConsoleRootDir(consoleName)
        val coversFolder = File(rootDir, "Covers")

        if (!coversFolder.exists()) coversFolder.mkdirs()

        // DEFINIÇÃO DO NOME DO ARQUIVO:
        // Android: usa o host da URI (package name) -> com.exemplo.app.jpg
        // Outros: usa o nome do arquivo da ISO -> jogo.jpg
        val fileName = if (console == Console.ANDROID) {
            "${game.uri.host}.jpg"
        } else {
            "${game.name}.jpg"
        }

        val targetFile = File(coversFolder, fileName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// --- FUNÇÕES PRIVADAS DE LEITURA (SCAN) ---

private fun getAllowedExtensions(console: Console): Set<String> {
    return when (console) {
        Console.PS1 -> setOf("bin", "iso", "chd", "cue", "pbp")
        Console.PS2 -> setOf("iso", "chd", "gz")
        Console.PSP -> setOf("iso", "cso")
        Console.ANDROID -> emptySet()
    }
}

private fun scanDefaultFolder(console: Console): List<Game> {
    if (console == Console.ANDROID) return emptyList()

    val rootDir = FolderManager.getConsoleRootDir(console.name)
    if (!rootDir.exists() || !rootDir.isDirectory) return emptyList()

    val allowedExtensions = getAllowedExtensions(console)
    val coversMap = mutableMapOf<String, Uri>()

    // Mapeia capas existentes com timestamp para evitar cache do Compose
    val coversDir = File(rootDir, "Covers")
    if (coversDir.exists()) {
        coversDir.listFiles()?.forEach { file ->
            if (file.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp")) {
                val uriWithTimestamp = Uri.parse("${Uri.fromFile(file)}?t=${file.lastModified()}")
                coversMap[file.nameWithoutExtension.lowercase()] = uriWithTimestamp
            }
        }
    }

    return rootDir.listFiles()?.filter {
        it.isFile && it.extension.lowercase() in allowedExtensions
    }?.map { file ->
        val name = file.nameWithoutExtension
        Game(
            name = name,
            uri = Uri.fromFile(file),
            coverUri = coversMap[name.lowercase()]
        )
    }?.sortedBy { it.name } ?: emptyList()
}

private fun scanCustomUri(context: Context, rootUri: Uri, console: Console): List<Game> {
    val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()
    val allowedExtensions = getAllowedExtensions(console)
    val gamesList = mutableListOf<Game>()
    val coversMap = mutableMapOf<String, Uri>()

    // Busca pasta de capas em armazenamento externo (SAF)
    val coversDir = rootDoc.findFile("Covers")
    if (coversDir != null && coversDir.isDirectory) {
        coversDir.listFiles().forEach { file ->
            val name = file.name?.substringBeforeLast('.') ?: ""
            val ext = file.name?.substringAfterLast('.', "")?.lowercase()
            if (ext in setOf("jpg", "jpeg", "png", "webp")) {
                // Adiciona timestamp para refresh visual
                val uriWithTimestamp = "${file.uri}?t=${file.lastModified()}".toUri()
                coversMap[name.lowercase()] = uriWithTimestamp
            }
        }
    }

    rootDoc.listFiles().forEach { file ->
        if (!file.isDirectory && file.name != null) {
            val ext = file.name!!.substringAfterLast('.', "").lowercase()
            if (ext in allowedExtensions) {
                val name = file.name!!.substringBeforeLast('.')
                gamesList.add(
                    Game(
                        name = name,
                        uri = file.uri,
                        coverUri = coversMap[name.lowercase()]
                    )
                )
            }
        }
    }
    return gamesList.sortedBy { it.name }
}