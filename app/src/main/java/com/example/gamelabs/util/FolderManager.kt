package com.example.gamelabs.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

object FolderManager {
    // Caminho principal no armazenamento público: /storage/emulated/0/Games
    private val rootPath = File(Environment.getExternalStorageDirectory(), "Games")
    private val consoles = listOf("PS1", "PS2", "PSP")

    /**
     * Cria a estrutura visível para o usuário (Games/PS1/Covers, Games/PS1/MemoryCards, etc.)
     * Chame isto na inicialização do App.
     */
    fun createExternalStructure() {
        try {
            if (!rootPath.exists()) {
                rootPath.mkdirs()
            }

            consoles.forEach { consoleName ->
                val consoleDir = File(rootPath, consoleName)
                if (!consoleDir.exists()) consoleDir.mkdirs()

                // 1. Cria a pasta Covers (Para todos)
                val coversDir = File(consoleDir, "Covers")
                if (!coversDir.exists()) coversDir.mkdirs()

                // 2. Cria a pasta MemoryCards (Específico para PS1 conforme solicitado)
                if (consoleName == "PS1") {
                    val memCardDir = File(consoleDir, "MemoryCards")
                    if (!memCardDir.exists()) memCardDir.mkdirs()
                }
            }
            Log.d("FolderManager", "Estrutura externa criada em: ${rootPath.absolutePath}")
        } catch (e: Exception) {
            Log.e("FolderManager", "Erro ao criar pastas externas", e)
        }
    }

    /**
     * Retorna a pasta de MemoryCards de um console.
     * Ex: /storage/emulated/0/Games/PS1/MemoryCards
     */
    fun getMemoryCardPath(consoleName: String): String {
        val path = File(File(rootPath, consoleName), "MemoryCards")
        // Garante que a pasta existe antes de retornar o caminho
        if (!path.exists()) path.mkdirs()
        return path.absolutePath
    }

    fun clearOldCache(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "rom_cache")
            if (cacheDir.exists()) {
                // Deleta a pasta inteira e tudo que tem dentro
                cacheDir.deleteRecursively()
                Log.d("FolderManager", "Cache de ROMs limpo com sucesso.")
            }
        } catch (e: Exception) {
            Log.e("FolderManager", "Erro ao limpar cache antigo", e)
        }
    }

    /**
     * Retorna o caminho da pasta de sistema interna (Onde o Core busca arquivos vitais).
     * Local: /data/user/0/com.example.gamelabs/files/retro_system
     */
    fun getInternalSystemPath(context: Context): String {
        val systemDir = File(context.filesDir, "retro_system")
        if (!systemDir.exists()) {
            systemDir.mkdirs()
        }
        return systemDir.absolutePath
    }

    /**
     * Prepara o arquivo no cache para que o C++ possa ler a ROM.
     */
    fun getGameCacheFile(context: Context, extension: String): File {
        val cacheDir = File(context.cacheDir, "emulator_run")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        return File(cacheDir, "current_game.$extension")
    }

    /**
     * Retorna a pasta de capas de um console específico para a UI.
     */
    fun getCoversPath(consoleName: String): String {
        return File(File(rootPath, consoleName), "Covers").absolutePath
    }

    /**
     * Retorna a pasta raiz do console (Onde ficam os jogos).
     */
    fun getConsoleRootDir(consoleName: String): File {
        return File(rootPath, consoleName)
    }

    /**
     * Prepara arquivos de sistema se necessário (Ex: Assets do PSP futuramente).
     */
    fun deploySystemFiles(context: Context) {
        val systemDir = getInternalSystemPath(context)
        Log.d("FolderManager", "Pasta de sistema interna pronta: $systemDir")
    }
}