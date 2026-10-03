package com.hvlg.guide.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hvlg.guide.data.GuideCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    cards: List<GuideCard>,
    onToggleFavorite: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var expandedSources by remember { mutableStateOf(emptySet<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surface,
                    titleContentColor = scheme.onSurface,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Text(
                        if (cards.isEmpty()) "收藏夹" else "收藏夹（${cards.size}）",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        },
    ) { padding ->
        if (cards.isEmpty()) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "还没有收藏。\n在条目右上角点 ♡ 即可收藏。",
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    color = scheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 48.dp),
            ) {
                items(cards, key = { it.id }) { card ->
                    CardItem(
                        card = card,
                        query = "",
                        plainOnly = false,
                        sourcesExpanded = card.id in expandedSources,
                        onToggleSources = {
                            expandedSources = if (card.id in expandedSources) {
                                expandedSources - card.id
                            } else {
                                expandedSources + card.id
                            }
                        },
                        favorite = true,
                        onToggleFavorite = { onToggleFavorite(card.id) },
                        onOpenUrl = onOpenUrl,
                    )
                    HorizontalDivider(color = scheme.outlineVariant)
                }
            }
        }
    }
}
