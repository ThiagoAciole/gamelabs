package com.example.gamelabs.screen

import android.app.Activity
import android.opengl.GLSurfaceView
import android.view.InputDevice
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.gamelabs.R
import com.example.gamelabs.data.Ps1Bridge
import com.example.gamelabs.model.Game
import com.example.gamelabs.model.RetroInput
import com.example.gamelabs.util.FolderManager
import com.example.gamelabs.util.mapKeyCodeToRetroId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun Ps1EmulatorScreen(
    game: Game,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Estados
    var isRunning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var inputState by remember { mutableIntStateOf(0) }

    // Variável para guardar o caminho do arquivo temporário da ROM
    var tempGamePath by remember { mutableStateOf<String?>(null) }

    var showOverlay by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }

    // --- MODO IMERSIVO ---
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { insetsController.show(WindowInsetsCompat.Type.systemBars()) }
    }

    // --- RENDERER OPENGL ---
    val renderer = remember {
        object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) { Ps1Bridge.initGL() }
            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) { Ps1Bridge.resizeGL(width, height) }
            override fun onDrawFrame(gl: GL10?) {
                if (isRunning) {
                    Ps1Bridge.runFrame()
                    Ps1Bridge.drawFrameGL()
                }
            }
        }
    }

    // --- INICIALIZAÇÃO ---
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()

        val connectedControllerName = checkForGamepad()
        if (connectedControllerName != null) {
            showOverlay = false
            Toast.makeText(context, "Conectado ao $connectedControllerName", Toast.LENGTH_LONG).show()
        }

        withContext(Dispatchers.IO) {
            try {
                // 1. Configurar pasta do Memory Card (Games/PS1/MemoryCards)
                val savePath = FolderManager.getMemoryCardPath("PS1")
                Ps1Bridge.setDirectories(savePath)

                // 2. Carregar Core
                val corePath = Ps1Bridge.getCorePath(context)
                if (!Ps1Bridge.loadCore(corePath)) throw Exception("Falha ao carregar Core PS1")

                // 3. Copiar Jogo para Cache
                val path = copyGameToCache(context, game.uri, game.name) ?: throw Exception("Falha na ROM")
                tempGamePath = path // Guarda referência para deletar depois

                if (!Ps1Bridge.loadGame(path)) throw Exception("Falha ao carregar Jogo")

                Ps1Bridge.setupAudio()
                isRunning = true
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = e.message
                isRunning = false
            }
        }
    }

    // --- LIMPEZA AO SAIR ---
    DisposableEffect(Unit) {
        onDispose {
            isRunning = false
            Ps1Bridge.stopAudio()
            Ps1Bridge.closeCore()

            // Deletar o arquivo temporário (ISO/BIN) para liberar espaço
            tempGamePath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // --- UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                val nativeCode = keyEvent.nativeKeyEvent.keyCode
                val retroId = mapKeyCodeToRetroId(nativeCode)
                if (retroId != null) {
                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                        inputState = inputState or (1 shl retroId)
                    } else if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                        inputState = inputState and (1 shl retroId).inv()
                    }
                    Ps1Bridge.sendInput(0, inputState)
                    true
                } else {
                    false
                }
            }
    ) {
        if (errorMessage != null) {
            Text("Erro: $errorMessage", color = Color.Red, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Default.Close, "Fechar", tint = Color.White)
            }
        } else if (isRunning) {

            // 1. VÍDEO
            AndroidView(
                factory = { ctx ->
                    GLSurfaceView(ctx).apply {
                        setEGLContextClientVersion(2)
                        setRenderer(renderer)
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        isFocusable = false
                        isFocusableInTouchMode = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. OVERLAY
            if (showOverlay) {
                EmulatorOverlay(
                    onInputChanged = { buttonId, pressed ->
                        inputState = if (pressed) inputState or (1 shl buttonId) else inputState and (1 shl buttonId).inv()
                        Ps1Bridge.sendInput(0, inputState)
                    }
                )
            }

            // 3. MENU
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.gamepad),
                    contentDescription = "Controles",
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(if (showOverlay) 0.7f else 0.4f)
                        .clickable { showOverlay = !showOverlay }
                )

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Sair",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onClose() }
                )
            }

        } else {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun EmulatorOverlay(
    onInputChanged: (Int, Boolean) -> Unit
) {
    Box(modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // GATILHOS
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 45.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RetroImageButton(R.drawable.l1, RetroInput.ID_L, onInputChanged, size = 80)
                Spacer(Modifier.width(16.dp))
                RetroImageButton(R.drawable.l2, RetroInput.ID_L2, onInputChanged, size = 80)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RetroImageButton(R.drawable.r2, RetroInput.ID_R2, onInputChanged, size = 80)
                Spacer(Modifier.width(16.dp))
                RetroImageButton(R.drawable.r1, RetroInput.ID_R, onInputChanged, size = 80)
            }
        }

        // D-PAD
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 32.dp, bottom = 75.dp)
                .size(130.dp)
        ) {
            Box(Modifier.align(Alignment.TopCenter)) { RetroImageButton(R.drawable.up, RetroInput.ID_UP, onInputChanged, size = 42) }
            Box(Modifier.align(Alignment.BottomCenter)) { RetroImageButton(R.drawable.down, RetroInput.ID_DOWN, onInputChanged, size = 42) }
            Box(Modifier.align(Alignment.CenterStart)) { RetroImageButton(R.drawable.left, RetroInput.ID_LEFT, onInputChanged, size = 42) }
            Box(Modifier.align(Alignment.CenterEnd)) { RetroImageButton(R.drawable.right, RetroInput.ID_RIGHT, onInputChanged, size = 42) }
        }

        // AÇÃO
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 70.dp)
                .size(140.dp)
        ) {
            Box(Modifier.align(Alignment.TopCenter)) { RetroImageButton(R.drawable.triangle, RetroInput.ID_X, onInputChanged, size = 48) }
            Box(Modifier.align(Alignment.BottomCenter)) { RetroImageButton(R.drawable.cross, RetroInput.ID_A, onInputChanged, size = 48) }
            Box(Modifier.align(Alignment.CenterStart)) { RetroImageButton(R.drawable.square, RetroInput.ID_Y, onInputChanged, size = 48) }
            Box(Modifier.align(Alignment.CenterEnd)) { RetroImageButton(R.drawable.circle, RetroInput.ID_B, onInputChanged, size = 48) }
        }

        // START/SELECT
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        ) {
            RetroImageButton(R.drawable.select, RetroInput.ID_SELECT, onInputChanged, size = 40)
            Spacer(Modifier.width(32.dp))
            RetroImageButton(R.drawable.start, RetroInput.ID_START, onInputChanged, size = 40)
        }
    }
}

@Composable
fun RetroImageButton(
    iconRes: Int,
    buttonId: Int,
    onInput: (Int, Boolean) -> Unit,
    size: Int = 60
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        onInput(buttonId, isPressed)
    }

    val alpha = if (isPressed) 0.4f else 0.7f

    Image(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        modifier = Modifier
            .size(size.dp)
            .alpha(alpha)
            .clickable(interactionSource = interactionSource, indication = null) { },
        contentScale = ContentScale.Fit
    )
}

// Helper de Cache
private suspend fun copyGameToCache(context: android.content.Context, uri: android.net.Uri, name: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            // 1. Cria uma pasta dedicada para evitar misturar com cache de imagens
            val cacheDir = File(context.cacheDir, "rom_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val fileName = if (name.contains(".")) name else "$name.bin"

            // 2. Salva DENTRO dessa pasta
            val tempFile = File(cacheDir, fileName)

            if (tempFile.exists() && tempFile.length() > 0) return@withContext tempFile.absolutePath

            val outputStream = FileOutputStream(tempFile)
            inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }
            tempFile.absolutePath
        } catch (e: Exception) { null }
    }
}

private fun checkForGamepad(): String? {
    val deviceIds = InputDevice.getDeviceIds()
    for (deviceId in deviceIds) {
        val device = InputDevice.getDevice(deviceId) ?: continue
        val sources = device.sources
        if ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
            return device.name
        }
    }
    return null
}

@Preview(name = "Preview PS1", widthDp = 840, heightDp = 380, showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun Ps1EmulatorPreview() {
    Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
        EmulatorOverlay(onInputChanged = { _, _ -> })
    }
}