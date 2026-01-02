package com.example.gamelabs.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.gamelabs.model.Game
import com.example.gamelabs.util.FolderManager
import androidx.core.net.toUri
import java.io.File

object AndroidGameScanner {

    /**
     * Busca todos os apps instalados e verifica se possuem uma capa personalizada
     * na pasta Gamelabs/ANDROID/Covers/nome.do.pacote.jpg
     */
    fun scanAllInstalledApps(context: Context): List<Game> {
        val pm = context.packageManager
        val games = mutableListOf<Game>()

        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
        }

        // Referência para a pasta de capas do Android
        val rootDir = FolderManager.getConsoleRootDir("ANDROID")
        val coversFolder = File(rootDir, "Covers")

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            val rootDir = FolderManager.getConsoleRootDir("ANDROID")
            val coversFolder = File(rootDir, "Covers")
            val customCoverFile = File(coversFolder, "$packageName.jpg")

            val coverUri = if (customCoverFile.exists()) {
                Uri.fromFile(customCoverFile).buildUpon()
                    .appendQueryParameter("t", customCoverFile.lastModified().toString())
                    .build()
            } else {
                null
            }

            games.add(
                Game(
                    name = resolveInfo.loadLabel(pm).toString(),
                    uri = "android://$packageName".toUri(),
                    coverUri = coverUri // Se existir o arquivo, a capa será exibida
                )
            )
        }

        return games.distinctBy { it.uri.host }.sortedBy { it.name }
    }

    /**
     * Filtra os apps instalados retornando apenas os identificados como Jogos pelo sistema.
     */
    fun scanInstalledGames(context: Context): List<Game> {
        val allApps = scanAllInstalledApps(context)
        val pm = context.packageManager

        return allApps.filter { game ->
            val pkgName = game.uri.host ?: return@filter false
            try {
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_GAME
                } else {
                    @Suppress("DEPRECATION")
                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_IS_GAME) != 0
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}