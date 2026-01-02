package com.example.gamelabs.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.example.gamelabs.R
import com.example.gamelabs.data.AndroidGameScanner
import com.example.gamelabs.data.saveGameCover
import com.example.gamelabs.model.Console
import com.example.gamelabs.model.Game
import com.example.gamelabs.util.isConfirmButton
import com.example.gamelabs.util.isL1
import com.example.gamelabs.util.isR1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidGamesScreen(
    onBack: () -> Unit,
    onPlayGame: (Game) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("android_games_prefs", Context.MODE_PRIVATE) }

    var games by remember { mutableStateOf<List<Game>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGameTitle by remember { mutableStateOf("") }

    // Função otimizada para carregar e sincronizar capas
    fun loadSelectedGames() {
        scope.launch(Dispatchers.IO) {
            val selectedPackages = prefs.getStringSet("selected_packages", emptySet()) ?: emptySet()
            // O scanner agora já busca as capas na pasta Gamelabs/ANDROID/Covers
            val allApps = AndroidGameScanner.scanAllInstalledApps(context)
            val filtered = allApps.filter { selectedPackages.contains(it.uri.host) }

            withContext(Dispatchers.Main) {
                games = filtered
                isLoading = false
                if (games.isNotEmpty()) {
                    // Mantém o título do jogo atual ou reseta para o primeiro
                    selectedGameTitle = games.find { it.name == selectedGameTitle }?.name ?: games[0].name
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadSelectedGames()
    }

    BackHandler(onBack = onBack)

    if (showAddDialog) {
        AppSelectorDialog(
            onDismiss = { showAddDialog = false },
            onSave = { selectedSet ->
                prefs.edit { putStringSet("selected_packages", selectedSet) }
                loadSelectedGames()
                showAddDialog = false
            },
            initialSelection = prefs.getStringSet("selected_packages", emptySet()) ?: emptySet()
        )
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(60.dp),
                title = {
                    Text(text = "Android Games", style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.voltar_description))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(60.dp)) {
                        Icon(Icons.Default.Add, "Selecionar Apps", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = { BottomActionsBar(onBack = onBack) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF7C4DFF))
            } else {
                AndroidGamesContent(
                    games = games,
                    selectedGameTitle = selectedGameTitle,
                    onSelectedGameChange = { selectedGameTitle = it },
                    onPlayGame = onPlayGame,
                    onRescan = { loadSelectedGames() },
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
fun AppSelectorDialog(
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit,
    initialSelection: Set<String>
) {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf<List<Game>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    val selectedPackages = remember { mutableStateListOf<String>().apply { addAll(initialSelection) } }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            allApps = AndroidGameScanner.scanAllInstalledApps(context)
            isLoadingApps = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Selecionar Aplicativos", color = Color.White) },
        text = {
            Column(modifier = Modifier.height(450.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar app...") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF7C4DFF)
                    )
                )

                if (isLoadingApps) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF7C4DFF))
                    }
                } else {
                    val filteredApps = allApps.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredApps) { app ->
                            val pkgName = app.uri.host ?: ""
                            val isChecked = selectedPackages.contains(pkgName)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedPackages.remove(pkgName)
                                        else selectedPackages.add(pkgName)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val icon = remember(pkgName) {
                                    try { context.packageManager.getApplicationIcon(pkgName) }
                                    catch (e: Exception) { null }
                                }

                                AsyncImage(
                                    model = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )

                                Text(
                                    text = app.name,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF7C4DFF))
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedPackages.toSet()) }) {
                Text("Salvar", color = Color(0xFF7C4DFF), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@Composable
fun AndroidGamesContent(
    games: List<Game>,
    selectedGameTitle: String,
    onSelectedGameChange: (String) -> Unit,
    onPlayGame: (Game) -> Unit,
    onRescan: () -> Unit,
    onBack: () -> Unit
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
                val success = saveGameCover(context, Console.ANDROID, gameBeingEdited!!, uri)
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
        modifier = Modifier.fillMaxSize().onPreviewKeyEvent { keyEvent ->
            when {
                keyEvent.isL1() -> { focusManager.moveFocus(FocusDirection.Left); true }
                keyEvent.isR1() -> { focusManager.moveFocus(FocusDirection.Right); true }
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
            modifier = Modifier.padding(2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        if (games.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nenhum jogo selecionado.", color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text("Toque no botão + para adicionar seus apps.", color = Color.DarkGray, fontSize = 12.sp)
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.weight(1f).padding(top = 30.dp)
            ) {
                itemsIndexed(games) { index, game ->
                    AndroidGameCard(
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
private fun AndroidGameCard(
    game: Game,
    focusRequester: FocusRequester? = null,
    onFocus: () -> Unit,
    onOpenGame: () -> Unit,
    onChangeCover: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val appIcon = remember(game.uri) {
        val packageName = game.uri.host
        if (packageName != null) {
            try { context.packageManager.getApplicationIcon(packageName) }
            catch (e: Exception) { null }
        } else null
    }

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
                    onOpenGame()
                    true
                } else if (keyEvent.nativeKeyEvent.keyCode == 99 ||
                    keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_X) {
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
                    model = game.coverUri, // A URI agora tem o timestamp do arquivo
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
            } else if (appIcon != null) {
                AsyncImage(
                    model = appIcon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(20.dp)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.gamepad),
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(50.dp).align(Alignment.Center)
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
    }
}

@Composable
private fun BottomActionsBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
        label = "ButtonColor"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(interactionSource, null) { onClick() }
    ) {
        Surface(shape = RoundedCornerShape(50), color = backgroundColor, modifier = Modifier.size(24.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = key, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(text = label, color = Color.White, modifier = Modifier.padding(start = 8.dp), fontSize = 12.sp)
    }
}