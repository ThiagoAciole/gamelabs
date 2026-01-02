package com.example.gamelabs.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamelabs.R
import com.example.gamelabs.model.Console
import com.example.gamelabs.ui.theme.GamelabsTheme
import com.example.gamelabs.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenConsole: (Console) -> Unit,
    onOpenSettings: () -> Unit // Parâmetro adicionado para configurações
) {
    val context = LocalContext.current
    // Força orientação Paisagem
    LaunchedEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    val consoles = Console.entries
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val pagerState = rememberPagerState(pageCount = { consoles.size })
    val selectedIndex = pagerState.currentPage

    // Ícones dos consoles
    val consoleIcons = listOf(
        R.drawable.ps1_banner,
        R.drawable.ps2_banner,
        R.drawable.psp_banner,
        R.drawable.android_banner
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    Image(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = "Gamelabs Logo",
                        modifier = Modifier
                            .height(22.dp)
                            .padding(start = 16.dp)
                    )
                },
                actions = {
                    // Botão de menu configurado para abrir configurações
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Configurações",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = {
            BottomActionsBarHome(
                onConfirm = { onOpenConsole(consoles[selectedIndex]) },
                onSettings = onOpenSettings // Atalho visual
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    // Apenas processar eventos de tecla pressionada (Down)
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when {
                            keyEvent.isDpadLeft() || keyEvent.isL1() -> {
                                scope.launch {
                                    val prevPage = if (pagerState.currentPage == 0) consoles.size - 1 else pagerState.currentPage - 1
                                    pagerState.animateScrollToPage(prevPage)
                                }
                                return@onPreviewKeyEvent true
                            }
                            keyEvent.isDpadRight() || keyEvent.isR1() -> {
                                scope.launch {
                                    val nextPage = if (pagerState.currentPage == consoles.size - 1) 0 else pagerState.currentPage + 1
                                    pagerState.animateScrollToPage(nextPage)
                                }
                                return@onPreviewKeyEvent true
                            }
                            keyEvent.isConfirmButton() -> {
                                onOpenConsole(consoles[selectedIndex])
                                return@onPreviewKeyEvent true
                            }
                            // Detectar botão Y no controle ou tecla Y no teclado
                            keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BUTTON_Y ||
                                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_Y -> {
                                onOpenSettings()
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) { page ->
                    val console = consoles[page]
                    val iconRes = consoleIcons.getOrElse(page) { R.drawable.ps1_banner }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onOpenConsole(console) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = console.displayName,
                            modifier = Modifier.size(220.dp).padding(bottom = 16.dp)
                        )
                    }
                }

                // Setas laterais
                NavigationArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, Alignment.CenterStart) {
                    scope.launch {
                        val prev = if (pagerState.currentPage == 0) consoles.size - 1 else pagerState.currentPage - 1
                        pagerState.animateScrollToPage(prev)
                    }
                }
                NavigationArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, Alignment.CenterEnd) {
                    scope.launch {
                        val next = if (pagerState.currentPage == consoles.size - 1) 0 else pagerState.currentPage + 1
                        pagerState.animateScrollToPage(next)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.NavigationArrow(icon: ImageVector, align: Alignment, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.align(align).padding(16.dp).size(64.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun BottomActionsBarHome(
    onConfirm: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton("A", "Confirmar", onConfirm)
        Spacer(Modifier.width(24.dp))
        ActionButton("Y", "Configurações", onSettings)
    }
}

@Composable
private fun ActionButton(key: String, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(shape = RoundedCornerShape(100), color = Color.Gray) {
            Text(
                text = key,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}
@Preview(
    name = "Galaxy S24 (Manual)",
    showBackground = true,
    widthDp = 900,
    heightDp = 400
)
@Composable
private fun HomeScreenPreview() {
    GamelabsTheme {
        HomeScreen(
            onOpenConsole = {},
            onOpenSettings = {}
        )
    }
}