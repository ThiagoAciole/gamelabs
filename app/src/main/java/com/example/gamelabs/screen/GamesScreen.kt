package com.example.gamelabs.screen

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.example.gamelabs.R
import com.example.gamelabs.data.scanGamesFromFolder
import com.example.gamelabs.model.Console
import com.example.gamelabs.model.Game
import com.example.gamelabs.util.FolderManager
import com.example.gamelabs.util.isConfirmButton
import com.example.gamelabs.util.isL1
import com.example.gamelabs.util.isR1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    console: Console,
    onBack: () -> Unit,
    onPlayGame: (Game) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var games by remember { mutableStateOf<List<Game>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedGameTitle by remember { mutableStateOf("") } // Título no topo

    BackHandler(onBack = onBack)

    fun performScan() {
        scope.launch(Dispatchers.IO) {
            isLoading = true
            try {
                // Agora o scan usa o FolderManager internamente, passamos apenas o context e console
                val result = scanGamesFromFolder(context, console)
                withContext(Dispatchers.Main) {
                    games = result
                    isLoading = false
                    if (games.isNotEmpty()) selectedGameTitle = games[0].name
                }
            } catch (e: Exception) {
                Log.e("GamesScreen", "Erro no scan: ${e.message}")
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    LaunchedEffect(console) { performScan() }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(console.displayName, color = Color.White, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = { BottomActionsBar(onBack = onBack) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF7C4DFF))
            } else {
                GamesScreenContent(
                    games = games,
                    selectedGameTitle = selectedGameTitle,
                    onSelectedGameChange = { selectedGameTitle = it },
                    onPlayGame = onPlayGame,
                    onRescan = { performScan() },
                    onBack = onBack,
                    consoleName = console.name
                )
            }
        }
    }
}

@Composable
fun GamesScreenContent(
    games: List<Game>,
    selectedGameTitle: String,
    onSelectedGameChange: (String) -> Unit,
    onPlayGame: (Game) -> Unit,
    onRescan: () -> Unit,
    onBack: () -> Unit,
    consoleName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val itemZeroRequester = remember { FocusRequester() }
    var gameBeingEdited by remember { mutableStateOf<Game?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && gameBeingEdited != null) {
            scope.launch(Dispatchers.IO) {
                val rootFile = FolderManager.getConsoleRootDir(consoleName)
                if (saveCoverToFile(context, gameBeingEdited!!, uri, rootFile)) {
                    withContext(Dispatchers.Main) { onRescan() }
                }
            }
        }
    }

    LaunchedEffect(games) {
        if (games.isNotEmpty()) {
            delay(100)
            itemZeroRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
                when {
                    keyEvent.isL1() -> {
                        focusManager.moveFocus(FocusDirection.Left); true
                    }

                    keyEvent.isR1() -> {
                        focusManager.moveFocus(FocusDirection.Right); true
                    }
                    // Botão B do controle para Voltar
                    keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK ||
                            keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BUTTON_B -> {
                        if (keyEvent.type == KeyEventType.KeyUp) onBack()
                        true
                    }

                    else -> false
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título do jogo selecionado (Exibindo em cima como solicitado)
        Text(
            text = selectedGameTitle,
            color = Color(0xFF7C4DFF),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(1.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (games.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Nenhum jogo encontrado em Games/$consoleName", color = Color.Gray)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(games) { index, game ->
                    GameCard(
                        game = game,
                        focusRequester = if (index == 0) itemZeroRequester else null,
                        onFocus = { onSelectedGameChange(game.name) },
                        onOpenGame = { onPlayGame(game) },
                        onChangeCover = {
                            gameBeingEdited = game
                            imagePickerLauncher.launch("image/*")
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameCard(
    game: Game,
    focusRequester: FocusRequester? = null,
    onFocus: () -> Unit,
    onOpenGame: () -> Unit,
    onChangeCover: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .width(140.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .focusable()
            .combinedClickable(
                onClick = onOpenGame,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onChangeCover()
                }
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.isConfirmButton()) {
                    onOpenGame(); true
                } else if (keyEvent.nativeKeyEvent.keyCode == 99 || keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_X) {
                    if (keyEvent.type == KeyEventType.KeyUp) onChangeCover()
                    true
                } else false
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Capa com proporção de DVD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A))
                .border(
                    width = if (isFocused) 4.dp else 1.dp,
                    color = if (isFocused) Color(0xFF7C4DFF) else Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            if (game.coverUri != null && game.coverUri != Uri.EMPTY) {
                AsyncImage(
                    model = game.coverUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback icon se não tiver capa ou for preview
                Icon(
                    painter = painterResource(R.drawable.gamepad),
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Nome embaixo do card
        Text(
            text = game.name,
            color = if (isFocused) Color.White else Color.Gray,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
    }
}


@Composable
private fun BottomActionsBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton("A", "Jogar")
        ActionButton("X", "Alterar Capa")
        ActionButton("B", "Voltar", onClick = onBack)
    }
}

@Composable
private fun ActionButton(key: String, label: String, onClick: () -> Unit = {}) {
    // Fonte de interação para detectar o toque
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animação da cor: Roxo se pressionado, Cinza se solto
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFF7C4DFF) else Color.DarkGray,
        label = "ButtonColorAnimation"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = backgroundColor,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = key,
                    color = if (isPressed) Color.White else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = label,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 12.sp
        )
    }
}

private fun saveCoverToFile(
    context: Context,
    game: Game,
    sourceUri: Uri,
    rootFile: File
): Boolean {
    return try {
        val coversFolder = File(rootFile, "Covers")
        if (!coversFolder.exists()) coversFolder.mkdirs()
        val targetFile = File(coversFolder, "${game.name}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        }
        true
    } catch (e: Exception) {
        false
    }
}
// ================= PREVIEW =================

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Games Screen (Landscape)",
    widthDp = 891,
    heightDp = 411,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun GamesScreenPreview() {
    // Dados Mockados
    val mockGames = listOf(
        Game("Crash Bandicoot 3", Uri.EMPTY, null),
        Game("Tekken 3", Uri.EMPTY, null),
        Game("Gran Turismo 2", Uri.EMPTY, null),
        Game("Silent Hill", Uri.EMPTY, null),
        Game("Castlevania SOTN", Uri.EMPTY, null)
    )

    // Adicionamos o Scaffold AQUI na preview para simular a tela completa
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PlayStation 1", color = Color.White, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = {
            BottomActionsBar(onBack = {})
        }
    ) { padding ->
        // O Box aplica o padding do Scaffold para o conteúdo não ficar embaixo das barras
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            GamesScreenContent(
                games = mockGames,
                selectedGameTitle = "Crash Bandicoot 3",
                onSelectedGameChange = {},
                onPlayGame = {},
                onRescan = {},
                onBack = {},
                consoleName = "PS1"
            )
        }
    }
}