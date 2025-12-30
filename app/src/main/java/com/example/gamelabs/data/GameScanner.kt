package com.example.gamelabs.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.gamelabs.model.Console
import com.example.gamelabs.model.Game
import com.example.gamelabs.util.FolderManager
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * GameScanner: Centraliza toda a lógica de Ler Jogos e Salvar Capas.
 * Suporta tanto o armazenamento interno padrão quanto pastas customizadas (SD Card/USB).
 */

// --- FUNÇÕES PÚBLICAS ---

fun scanGames(context: Context, console: Console): List<Game> {
    val prefs = context.getSharedPreferences("game_paths", Context.MODE_PRIVATE)
    val customUriString = prefs.getString(console.name, null)

    return if (customUriString != null) {
        scanCustomUri(context, Uri.parse(customUriString), console)
    } else {
        scanDefaultFolder(console)
    }
}

fun saveGameCover(context: Context, console: Console, game: Game, sourceImageUri: Uri): Boolean {
    val prefs = context.getSharedPreferences("game_paths", Context.MODE_PRIVATE)
    val customUriString = prefs.getString(console.name, null)

    return if (customUriString != null) {
        // Salva via SAF (DocumentFile) para pastas customizadas
        saveCoverToCustomUri(context, Uri.parse(customUriString), game, sourceImageUri)
    } else {
        // Salva via IO padrão (File) para pasta interna
        saveCoverToDefaultFolder(context, console, game, sourceImageUri)
    }
}

// --- FUNÇÕES PRIVADAS DE LEITURA (SCAN) ---

private fun getAllowedExtensions(console: Console): Set<String> {
    return when (console) {
        Console.PS1 -> setOf("bin", "iso", "chd", "cue", "pbp")
        Console.PS2 -> setOf("iso", "chd", "gz")
        Console.PSP -> setOf("iso", "cso")
    }
}

private fun scanDefaultFolder(console: Console): List<Game> {
    val rootDir = FolderManager.getConsoleRootDir(console.name)
    if (!rootDir.exists() || !rootDir.isDirectory) return emptyList()

    val allowedExtensions = getAllowedExtensions(console)
    val coversMap = mutableMapOf<String, Uri>()

    val coversDir = File(rootDir, "Covers")
    if (coversDir.exists()) {
        coversDir.listFiles()?.forEach { file ->
            if (file.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp")) {
                coversMap[file.nameWithoutExtension.lowercase()] = Uri.fromFile(file)
            }
        }
    }

    return rootDir.listFiles()?.filter {
        it.isFile && it.extension.lowercase() in allowedExtensions
    }?.map { file ->
        Game(
            name = file.nameWithoutExtension,
            uri = Uri.fromFile(file),
            coverUri = coversMap[file.nameWithoutExtension.lowercase()]
        )
    }?.sortedBy { it.name } ?: emptyList()
}

private fun scanCustomUri(context: Context, rootUri: Uri, console: Console): List<Game> {
    val rootDir = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()
    val allowedExtensions = getAllowedExtensions(console)
    val gamesList = mutableListOf<Game>()
    val coversMap = mutableMapOf<String, Uri>()

    val coversDir = rootDir.findFile("Covers")
    if (coversDir != null && coversDir.isDirectory) {
        coversDir.listFiles().forEach { file ->
            val name = file.name?.substringBeforeLast('.') ?: ""
            val ext = file.name?.substringAfterLast('.', "")?.lowercase()
            if (ext in setOf("jpg", "jpeg", "png", "webp")) {
                coversMap[name.lowercase()] = file.uri
            }
        }
    }

    rootDir.listFiles().forEach { file ->
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

// --- FUNÇÕES PRIVADAS DE ESCRITA (SAVE) ---

private fun saveCoverToDefaultFolder(context: Context, console: Console, game: Game, sourceUri: Uri): Boolean {
    return try {
        val rootDir = FolderManager.getConsoleRootDir(console.name)
        val coversFolder = File(rootDir, "Covers")
        if (!coversFolder.exists()) coversFolder.mkdirs()

        val targetFile = File(coversFolder, "${game.name}.jpg")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun saveCoverToCustomUri(context: Context, rootUri: Uri, game: Game, sourceUri: Uri): Boolean {
    val root = DocumentFile.fromTreeUri(context, rootUri) ?: return false

    // 1. Acha ou cria pasta Covers
    var coversDir = root.findFile("Covers")
    if (coversDir == null) coversDir = root.createDirectory("Covers")
    if (coversDir == null) return false

    // 2. Prepara arquivo
    val targetName = "${game.name}.jpg"
    val existing = coversDir.findFile(targetName)
    existing?.delete()

    val newFile = coversDir.createFile("image/jpeg", targetName) ?: return false

    // 3. Copia
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
        val outputStream: OutputStream? = context.contentResolver.openOutputStream(newFile.uri)
        if (inputStream != null && outputStream != null) {
            inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }
            true
        } else false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}