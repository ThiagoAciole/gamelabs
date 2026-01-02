package com.example.gamelabs.screen

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.example.gamelabs.R
import com.example.gamelabs.data.saveGameCover
import com.example.gamelabs.data.scanGames
import com.example.gamelabs.model.Console
import com.example.gamelabs.model.Game
import com.example.gamelabs.util.isConfirmButton
import com.example.gamelabs.util.isL1
import com.example.gamelabs.util.isR1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var selectedGameTitle by remember { mutableStateOf("") }

    // SharedPreferences para salvar o caminho customizado
    val prefs = remember { context.getSharedPreferences("game_paths", Context.MODE_PRIVATE) }

    // Launcher para escolher a pasta
    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Persistir permissão de leitura para o futuro
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: Exception) {
                Log.e("GamesScreen", "Erro ao persistir permissão: ${e.message}")
            }

            // Salvar URI nas preferências
            prefs.edit().putString(console.name, uri.toString()).apply()

            // Forçar rescaneamento
            isLoading = true
        }
    }

    BackHandler(onBack = onBack)

    fun performScan() {
        scope.launch(Dispatchers.IO) {
            isLoading = true
            try {
                // Scan usando a função unificada
                val result = scanGames(context, console)

                withContext(Dispatchers.Main) {
                    games = result
                    isLoading = false
                    if (games.isNotEmpty() && selectedGameTitle.isEmpty()) {
                        selectedGameTitle = games[0].name
                    }
                    if (games.isEmpty()) selectedGameTitle = ""
                }
            } catch (e: Exception) {
                Log.e("GamesScreen", "Erro no scan: ${e.message}")
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    LaunchedEffect(console, isLoading) {
        if(isLoading) performScan()
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(60.dp),
                title = {
                    Text(
                        text = console.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.voltar_description)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { dirPickerLauncher.launch(null) },
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Selecionar Pasta de Jogos",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = { BottomActionsBar(onBack = onBack) }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding()
            )
        ) {
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
                    console = console // <--- CORREÇÃO 1: Passando o objeto Console
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
    console: Console // <--- CORREÇÃO 2: Recebendo Console em vez de String
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
                // Agora 'console' existe aqui corretamente
                val success = saveGameCover(
                    context,
                    console,
                    gameBeingEdited!!,
                    uri
                )

                if (success) {
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
        Text(
            text = selectedGameTitle,
            color = Color(0xFF7C4DFF),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (games.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.notfound),
                        contentDescription = "Nenhum jogo encontrado",
                        modifier = Modifier
                            .size(200.dp),
                        alpha = 0.7f
                    )
                    Text("Nenhum jogo encontrado.", color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text("Toque em + no topo para selecionar uma pasta.", color = Color.DarkGray, fontSize = 12.sp)
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 30.dp)
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

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
                    color = Color.White,
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

// Removi a função privada saveCoverToFile pois agora o GameScanner cuida disso

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
    val mockGames = listOf(
        Game("Crash Bandicoot 3", Uri.EMPTY, null),
        Game("Tekken 3", Uri.EMPTY, null)
    )

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(60.dp),
                title = { Text("PlayStation 1", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = {}, modifier = Modifier.size(60.dp)) {
                        Icon(Icons.Default.Add, "Add", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = { BottomActionsBar(onBack = {}) }
    ) { padding ->
        Box(modifier = Modifier
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
            .fillMaxSize()) {
            GamesScreenContent(
                games = mockGames,
                selectedGameTitle = "Crash Bandicoot 3",
                onSelectedGameChange = {},
                onPlayGame = {},
                onRescan = {},
                onBack = {},
                console = Console.PS1 // <--- CORREÇÃO 3: Passando Console na Preview
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Games Screen (Empty/Not Found)",
    widthDp = 891,
    heightDp = 411,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun GamesScreenPreviewEmpty() {
    // Não precisamos de mockGames aqui, pois queremos testar o estado vazio

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(60.dp),
                title = { Text("PlayStation 1", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = {}, modifier = Modifier.size(60.dp)) {
                        Icon(Icons.Default.Add, "Add", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = { BottomActionsBar(onBack = {}) }
    ) { padding ->
        Box(modifier = Modifier
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
            .fillMaxSize()) {

            GamesScreenContent(
                games = emptyList(),
                selectedGameTitle = "",
                onSelectedGameChange = {},
                onPlayGame = {},
                onRescan = {},
                onBack = {},
                console = Console.PS1
            )
        }
    }
}