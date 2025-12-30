package com.example.gamelabs.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamelabs.R
import com.example.gamelabs.model.Console
import com.example.gamelabs.ui.theme.GamelabsTheme
import com.example.gamelabs.util.isConfirmButton
import com.example.gamelabs.util.isDpadLeft
import com.example.gamelabs.util.isDpadRight
import com.example.gamelabs.util.isL1
import com.example.gamelabs.util.isR1
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenConsole: (Console) -> Unit
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

    // Ícones dos consoles (certifique-se que existem no Drawable)
    val consoleIcons = listOf(
        R.drawable.ps1_banner,
        R.drawable.ps2_banner,
        R.drawable.psp_banner
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
                            .padding(start = 16.dp) // Afasta um pouco da borda esquerda
                    )
                },
                actions = {
                    // 2. MENU NA DIREITA
                    IconButton(onClick = { /* TODO: Ação do menu */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = {
            BottomActionsBarHome(
                onConfirm = { onOpenConsole(consoles[selectedIndex]) }
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
                    when {
                        keyEvent.isDpadLeft() || keyEvent.isL1() -> {
                            scope.launch {
                                val prevPage = if (pagerState.currentPage == 0) consoles.size - 1 else pagerState.currentPage - 1
                                pagerState.animateScrollToPage(prevPage)
                            }
                            true
                        }
                        keyEvent.isDpadRight() || keyEvent.isR1() -> {
                            scope.launch {
                                val nextPage = if (pagerState.currentPage == consoles.size - 1) 0 else pagerState.currentPage + 1
                                pagerState.animateScrollToPage(nextPage)
                            }
                            true
                        }
                        keyEvent.isConfirmButton() -> {
                            onOpenConsole(consoles[selectedIndex])
                            true
                        }
                        else -> false
                    }
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
                    // Fallback seguro se não houver ícone suficiente na lista
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

                // Setas visuais laterais
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
private fun BoxScope.NavigationArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, align: Alignment, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.align(align).padding(16.dp).size(64.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun BottomActionsBarHome(onConfirm: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Exibe apenas o botão de confirmar, já que removemos a busca
        ActionButton("A", "Confirmar", onConfirm)
    }
}

@Composable
private fun ActionButton(key: String, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClick() }) {
        Surface(shape = RoundedCornerShape(100), color = Color.Gray) {
            Text(text = key, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold, color = Color.Black)
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
        HomeScreen(onOpenConsole = {})
    }
}