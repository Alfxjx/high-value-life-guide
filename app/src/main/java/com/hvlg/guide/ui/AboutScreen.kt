package com.hvlg.guide.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hvlg.guide.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val scheme = MaterialTheme.colorScheme

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
                title = { Text("关于", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                "高性价比人生指南",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "版本 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
                fontSize = 13.sp,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "完全离线 · 全文搜索 · 收藏 · 阅读位置记忆 · 不申请任何权限",
                fontSize = 13.sp,
                color = scheme.onSurfaceVariant,
            )

            AboutSection(
                title = "原书",
                body = "《高性价比人生指南》，按「性价比」排序的循证生活建议合集，每条写清花掉什么、换回什么、证据多硬，只引期刊论文和官方文件。内容以 Unlicense 协议释出，属公有领域。",
                linkText = "github.com/eternity4719/HowToLiveBetter",
                linkUrl = "https://github.com/eternity4719/HowToLiveBetter",
                onOpenUrl = uriHandler::openUri,
            )
            AboutSection(
                title = "网页版",
                body = "cdyforever 制作的单文件 HTML 阅读页，把原书全部内容渲染成可搜索、零依赖、断网可读的页面。本 App 的内容即构建期解析自该页面。",
                linkText = "github.com/cdyforever/how-to-live-better",
                linkUrl = "https://github.com/cdyforever/how-to-live-better",
                onOpenUrl = uriHandler::openUri,
            )
            AboutSection(
                title = "本 App",
                body = "xujianxiang 制作的 Android 原生客户端：内容离线内置，提供侧边目录、收藏夹、阅读位置记忆、全文搜索与明暗主题。内容随上游每日自动更新并重打包。",
                linkText = "github.com/Alfxjx/high-value-life-guide",
                linkUrl = "https://github.com/Alfxjx/high-value-life-guide",
                onOpenUrl = uriHandler::openUri,
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = scheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            Text(
                "书中内容给的是通用口径，不替代医生、律师、会计等专业人士的意见。",
                fontSize = 12.5.sp,
                lineHeight = 20.sp,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    body: String,
    linkText: String,
    linkUrl: String,
    onOpenUrl: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Spacer(Modifier.height(20.dp))
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
    Spacer(Modifier.height(6.dp))
    Text(body, fontSize = 13.5.sp, lineHeight = 22.sp, color = scheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = scheme.primary,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(linkText)
            }
        },
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(linkUrl) },
    )
}
