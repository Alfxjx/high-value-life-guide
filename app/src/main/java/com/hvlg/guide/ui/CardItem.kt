package com.hvlg.guide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hvlg.guide.data.GuideCard

private val TAG_ORDER = listOf("口径", "钱", "时间", "毅力", "收益")
private val FavoriteColor = Color(0xFFD81B60)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardItem(
    card: GuideCard,
    query: String,
    plainOnly: Boolean,
    sourcesExpanded: Boolean,
    onToggleSources: () -> Unit,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = HvlgTheme.palette
    val scheme = MaterialTheme.colorScheme

    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = scheme.onSurfaceVariant)) {
                        append("${card.num}  ")
                    }
                    append(highlightMatches(card.title, query, palette.highlight))
                },
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Favorite
                    else Icons.Filled.FavoriteBorder,
                    contentDescription = if (favorite) "取消收藏" else "收藏",
                    tint = if (favorite) FavoriteColor else scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GradeBadge(card.grade)
            RatioBadge(card.ratio)
            for (key in TAG_ORDER) {
                val value = card.tags[key] ?: continue
                TagChip("$key $value")
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(palette.plainBorder),
            )
            Text(
                text = highlightMatches(card.plain, query, palette.highlight),
                modifier = Modifier
                    .background(palette.plainContainer)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 15.sp,
                lineHeight = 26.sp,
                color = scheme.onSurface,
            )
        }

        if (!plainOnly) {
            Spacer(Modifier.height(8.dp))
            Column {
                FieldRow("成本", card.cost, query, palette.highlight, isNote = false)
                FieldRow("收益", card.benefit, query, palette.highlight, isNote = false)
                FieldRow("备注", card.note, query, palette.highlight, isNote = true)
            }

            if (card.sources.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                SourceSection(
                    card = card,
                    expanded = sourcesExpanded,
                    onToggle = onToggleSources,
                    onOpenUrl = onOpenUrl,
                )
            }
        }
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    query: String,
    highlight: Color,
    isNote: Boolean,
) {
    val palette = HvlgTheme.palette
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = if (isNote) palette.gradeB else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = highlightMatches(value, query, highlight),
                fontSize = 13.5.sp,
                lineHeight = 23.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SourceSection(
    card: GuideCard,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val palette = HvlgTheme.palette
    val withLinks = card.sources.count { it.url != null }
    val title = if (withLinks > 0) "来源（${card.sources.size} 条文献）" else "来源"

    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowDown
                else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (expanded) "收起来源" else "展开来源",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier.padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                card.sources.forEach { source ->
                    Column {
                        Text(
                            text = source.text,
                            fontSize = 12.5.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val url = source.url
                        if (url != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = url,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenUrl(url) }
                                    .padding(vertical = 2.dp),
                                fontSize = 12.5.sp,
                                lineHeight = 20.sp,
                                color = palette.link,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
