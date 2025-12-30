package com.example.gamelabs.model

import android.net.Uri

data class Game(
    val name: String,
    val uri: Uri,
    val coverUri: Uri? = null
)