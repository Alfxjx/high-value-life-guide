package com.hvlg.guide.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hvlg.guide.data.ContentIndex
import com.hvlg.guide.data.FilterSpec
import com.hvlg.guide.data.GuideCard
import com.hvlg.guide.data.SearchScope
import com.hvlg.guide.data.Section
import com.hvlg.guide.data.ThemeMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private enum class SubPage { NONE, ABOUT, FAVORITES }

private sealed interface RowItem {
    val key: String

    data class Header(val section: Section) : RowItem {
        override val key: String get() = "header-${section.id}"
    }

    data class CardRow(val card: GuideCard) : RowItem {
        override val key: String get() = card.id
    }
}

@Composable
fun ReaderApp(viewModel: ReaderViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HvlgTheme(themeMode = state.themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val index = state.index
            when {
                state.error != null -> MessagePane("内容读取失败：${state.error}")
                state.loading || index == null -> LoadingPane()
                else -> ReaderScreen(state = state, index = index, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun LoadingPane() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessagePane(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
private fun ReaderScreen(
    state: ReaderState,
    index: ContentIndex,
    viewModel: ReaderViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    val rows = remember(index, state.currentSectionId, state.searchActive, state.query, state.spec) {
        buildRows(index, state.currentSectionId, state.searchActive, state.query, state.spec)
    }
    val rowIndexByKey = remember(rows) { rows.withIndex().associate { (i, r) -> r.key to i } }
    val shownCards = remember(rows) { rows.count { it is RowItem.CardRow } }
    val totalCards = index.cards.size
    val currentSection = index.sectionById[state.currentSectionId]
    val (prevSection, nextSection) = remember(index, state.currentSectionId) {
        val i = index.sections.indexOfFirst { it.id == state.currentSectionId }
        index.sections.getOrNull(i - 1) to index.sections.getOrNull(i + 1)
    }

    var expandedSources by remember { mutableStateOf(emptySet<String>()) }
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }
    var pendingScrollId by remember { mutableStateOf<String?>(null) }
    var pendingScrollTop by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var subPage by remember { mutableStateOf(SubPage.NONE) }
    var tocExpanded by remember { mutableStateOf(true) }
    var positionTracking by remember { mutableStateOf(false) }
    var autoExpandedOnOpen by remember { mutableStateOf(false) }
    var skipSearchFocusOnce by remember { mutableStateOf(false) }
    val tocListState = rememberLazyListState()

    val openSection: (com.hvlg.guide.data.Section) -> Unit = { section ->
        focusManager.clearFocus()
        viewModel.onSearchActiveChange(false)
        if (section.id != state.currentSectionId) {
            viewModel.onSectionChange(section.id)
            pendingScrollTop = true
        }
    }

    val currentCardId by remember(rows) {
        derivedStateOf {
            var i = listState.firstVisibleItemIndex
            while (i < rows.size && rows[i] !is RowItem.CardRow) i++
            (rows.getOrNull(i) as? RowItem.CardRow)?.card?.id
        }
    }

    // ---- 冷启动恢复上次阅读位置 ----
    LaunchedEffect(state.restoreSettled, state.restoreTargetId) {
        if (!state.restoreSettled) return@LaunchedEffect
        val target = state.restoreTargetId
        if (target != null) {
            val targetIndex = rowIndexByKey[target]
            if (targetIndex != null && targetIndex > 0) {
                listState.scrollToItem(targetIndex)
                index.cardById[target]?.secId?.let { expandedSections[it] = true }
                positionTracking = true
                val sectionNum = index.cardById[target]
                    ?.let { index.sectionById[it.secId]?.num }
                val result = snackbarHostState.showSnackbar(
                    message = "已回到上次位置 · 第 ${sectionNum ?: "?"} 节",
                    actionLabel = "回到开头",
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    positionTracking = false
                    listState.animateScrollToItem(0)
                    delay(400)
                    positionTracking = true
                }
            }
            viewModel.onRestoreHandled()
        }
        positionTracking = true
    }

    // ---- 滚动停止约 500ms 后记录首个可见卡片 ----
    LaunchedEffect(positionTracking, rows) {
        if (!positionTracking) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .debounce(500)
            .collect { position ->
                var i = position
                while (i < rows.size && rows[i] !is RowItem.CardRow) i++
                (rows.getOrNull(i) as? RowItem.CardRow)?.let {
                    viewModel.saveLastCardId(it.card.id)
                }
            }
    }

    // ---- 抽屉里点条目后滚动 ----
    LaunchedEffect(pendingScrollId, rows) {
        val id = pendingScrollId ?: return@LaunchedEffect
        val position = rowIndexByKey[id] ?: return@LaunchedEffect
        listState.animateScrollToItem(position)
        pendingScrollId = null
    }

    val openCard: (GuideCard) -> Unit = { card ->
        // 目标被当前筛选挡住时先放开筛选（保留展示模式与范围），再切到目标所在节
        if (!state.spec.matches(card)) {
            viewModel.onSpecChange(
                state.spec.copy(
                    grade = null, ratio = null, caliber = null,
                    money = null, time = null, grit = null, gain = null,
                ),
            )
        }
        if (state.searchActive) {
            viewModel.onQueryChange("")
            viewModel.onSearchActiveChange(false)
        }
        if (card.secId != state.currentSectionId) viewModel.onSectionChange(card.secId)
        pendingScrollId = card.id
    }

    // ---- 切节后滚回顶部 ----
    LaunchedEffect(pendingScrollTop, rows) {
        if (!pendingScrollTop) return@LaunchedEffect
        listState.scrollToItem(0)
        pendingScrollTop = false
    }

    // ---- 抽屉打开时：展开目录分组，只展开当前节，并把它滚进视口 ----
    if (drawerState.isOpen && !autoExpandedOnOpen) {
        autoExpandedOnOpen = true
        tocExpanded = true
        expandedSections.clear()
        state.currentSectionId?.let { expandedSections[it] = true }
    }
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            // 节列表前有 4 项（头部/收藏夹/关于/目录组头），前面的节收起时各占 1 项；
            // 滚到当前节前一项，留一点上下文；第 1 节时回到顶部，露出导航入口
            val i = index.sections.indexOfFirst { it.id == state.currentSectionId }
            if (i >= 0) tocListState.scrollToItem(if (i == 0) 0 else i + 3)
        } else {
            autoExpandedOnOpen = false
        }
    }

    BackHandler(enabled = state.searchActive && !drawerState.isOpen) {
        focusManager.clearFocus()
        viewModel.onSearchActiveChange(false)
    }

    when (subPage) {
        SubPage.ABOUT -> {
            BackHandler { subPage = SubPage.NONE }
            AboutScreen(onBack = { subPage = SubPage.NONE })
            return
        }
        SubPage.FAVORITES -> {
            BackHandler { subPage = SubPage.NONE }
            FavoritesScreen(
                cards = index.cards.filter { it.id in state.favoriteIds },
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenUrl = { url -> openInBrowser(context, url) },
                onBack = { subPage = SubPage.NONE },
            )
            return
        }
        SubPage.NONE -> Unit
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(0.85f),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                TocContent(
                    sections = index.sections,
                    cardsBySection = index.cardsBySection,
                    totalCards = totalCards,
                    currentCardId = currentCardId,
                    currentSectionId = state.currentSectionId,
                    expandedSections = expandedSections.filterValues { it }.keys,
                    favoriteCount = state.favoriteIds.size,
                    tocExpanded = tocExpanded,
                    listState = tocListState,
                    onToggleToc = { tocExpanded = !tocExpanded },
                    onToggleSection = { id ->
                        expandedSections[id] = expandedSections[id] != true
                    },
                    onSectionClick = { section ->
                        scope.launch {
                            drawerState.close()
                            expandedSections[section.id] = true
                            openSection(section)
                        }
                    },
                    onCardClick = { card ->
                        scope.launch {
                            drawerState.close()
                            focusManager.clearFocus()
                            viewModel.onSearchActiveChange(false)
                            openCard(card)
                        }
                    },
                    onFavoritesClick = {
                        scope.launch { drawerState.close() }
                        subPage = SubPage.FAVORITES
                    },
                    onAboutClick = {
                        scope.launch { drawerState.close() }
                        subPage = SubPage.ABOUT
                    },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                ReaderTopBar(
                    title = "高性价比人生指南",
                    sectionLabel = if (state.searchActive) {
                        null
                    } else {
                        currentSection?.let { "第 ${it.num} 节 · ${it.title}" }
                    },
                    searchActive = state.searchActive,
                    query = state.query,
                    themeMode = state.themeMode,
                    skipSearchFocus = skipSearchFocusOnce,
                    onSkipSearchFocusConsumed = { skipSearchFocusOnce = false },
                    onMenu = { scope.launch { drawerState.open() } },
                    onSearchToggle = { viewModel.onSearchActiveChange(true) },
                    onThemeToggle = { viewModel.toggleTheme() },
                    onQueryChange = viewModel::onQueryChange,
                    onExitSearch = {
                        focusManager.clearFocus()
                        viewModel.onSearchActiveChange(false)
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                val showFab by remember {
                    derivedStateOf {
                        val info = listState.layoutInfo
                        val visible = info.visibleItemsInfo.size
                        visible > 0 && listState.firstVisibleItemIndex > visible * 3
                    }
                }
                AnimatedVisibility(
                    visible = showFab,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch { listState.animateScrollToItem(0) }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "回到顶部")
                    }
                }
            },
        ) { innerPadding ->
            Column(Modifier.padding(innerPadding)) {
                FilterBar(
                    spec = state.spec,
                    countText = when {
                        state.searchActive -> {
                            val scopeTotal = if (state.spec.scope == SearchScope.SECTION) {
                                currentSection?.count ?: totalCards
                            } else {
                                totalCards
                            }
                            "$shownCards / $scopeTotal"
                        }
                        !state.spec.isDefault -> "本节 $shownCards 条"
                        else -> null
                    },
                    onOpenSheet = { showFilterSheet = true },
                    onTogglePlainOnly = {
                        viewModel.onSpecChange(state.spec.copy(plainOnly = !state.spec.plainOnly))
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (rows.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(
                            text = when {
                                state.query.isNotBlank() -> "没有匹配的条目"
                                !state.spec.isDefault -> "没有符合筛选条件的条目"
                                else -> "没有可显示的条目"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        items(rows, key = { it.key }) { row ->
                            when (row) {
                                is RowItem.Header -> SectionHeader(row.section)
                                is RowItem.CardRow -> {
                                    CardItem(
                                        card = row.card,
                                        query = state.query,
                                        plainOnly = state.spec.plainOnly,
                                        sourcesExpanded = row.card.id in expandedSources,
                                        onToggleSources = {
                                            expandedSources = if (row.card.id in expandedSources) {
                                                expandedSources - row.card.id
                                            } else {
                                                expandedSources + row.card.id
                                            }
                                        },
                                        favorite = row.card.id in state.favoriteIds,
                                        onToggleFavorite = {
                                            viewModel.toggleFavorite(row.card.id)
                                        },
                                        onOpenUrl = { url -> openInBrowser(context, url) },
                                    )
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                            }
                        }
                        if (!state.searchActive && rows.isNotEmpty()) {
                            item(key = "section-nav") {
                                SectionNavFooter(
                                    prev = prevSection,
                                    next = nextSection,
                                    onOpen = openSection,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterSheet(
            spec = state.spec,
            onSpecChange = viewModel::onSpecChange,
            onDismiss = {
                showFilterSheet = false
                // 选了「全书」范围后，信息流也应切到全书（进入搜索态、不弹键盘）
                if (state.spec.scope == SearchScope.ALL && !state.searchActive) {
                    skipSearchFocusOnce = true
                    viewModel.onSearchActiveChange(true)
                }
            },
        )
    }
}

@Composable
private fun FilterBar(
    spec: FilterSpec,
    countText: String?,
    onOpenSheet: () -> Unit,
    onTogglePlainOnly: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = spec.activeCount > 0,
            onClick = onOpenSheet,
            label = {
                Text(
                    if (spec.activeCount > 0) "筛选 · ${spec.activeCount}" else "筛选",
                    fontSize = 13.sp,
                )
            },
        )
        FilterChip(
            selected = spec.plainOnly,
            onClick = onTogglePlainOnly,
            label = { Text("只说人话", fontSize = 13.sp) },
        )
        Spacer(Modifier.weight(1f))
        if (countText != null) {
            Text(
                text = countText,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    spec: FilterSpec,
    onSpecChange: (FilterSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .heightIn(max = maxHeight)
                .padding(horizontal = 20.dp),
        ) {
            // 标题与操作按钮固定在顶部，不随内容滚动
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "筛选条件",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { onSpecChange(FilterSpec(scope = spec.scope)) },
                        enabled = spec.activeCount > 0,
                    ) {
                        Text("重置条件")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("完成")
                    }
                }
            }
            Spacer(Modifier.height(4.dp))

            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 20.dp),
            ) {
                // 搜索范围
                Text("搜索范围", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = spec.scope == SearchScope.ALL,
                        onClick = { onSpecChange(spec.copy(scope = SearchScope.ALL)) },
                        label = { Text("全书", fontSize = 12.5.sp) },
                    )
                    FilterChip(
                        selected = spec.scope == SearchScope.SECTION,
                        onClick = { onSpecChange(spec.copy(scope = SearchScope.SECTION)) },
                        label = { Text("本章节", fontSize = 12.5.sp) },
                    )
                }
                Text(
                    "全书：跨所有章节查找并展示；本章节：仅当前节",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                SheetDimRow("证据等级", FilterSpec.GRADES, spec.grade) { onSpecChange(spec.copy(grade = it)) }
                SheetDimRow("性价比", FilterSpec.RATIOS, spec.ratio) { onSpecChange(spec.copy(ratio = it)) }
                SheetDimRow("口径", FilterSpec.CALIBERS, spec.caliber) { onSpecChange(spec.copy(caliber = it)) }
                SheetDimRow("钱", FilterSpec.MONEY, spec.money) { onSpecChange(spec.copy(money = it)) }
                SheetDimRow("时间", FilterSpec.TIME, spec.time) { onSpecChange(spec.copy(time = it)) }
                SheetDimRow("毅力", FilterSpec.GRIT, spec.grit) { onSpecChange(spec.copy(grit = it)) }
                SheetDimRow("收益", FilterSpec.GAIN, spec.gain) { onSpecChange(spec.copy(gain = it)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SheetDimRow(
    label: String,
    options: List<String>,
    current: String?,
    onSelect: (String?) -> Unit,
) {
    Spacer(Modifier.height(14.dp))
    Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        FilterChip(
            selected = current == null,
            onClick = { onSelect(null) },
            label = { Text("全部", fontSize = 12.5.sp) },
        )
        for (option in options) {
            FilterChip(
                selected = current == option,
                onClick = { onSelect(if (current == option) null else option) },
                label = { Text(option, fontSize = 12.5.sp) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    title: String,
    sectionLabel: String?,
    searchActive: Boolean,
    query: String,
    themeMode: ThemeMode,
    skipSearchFocus: Boolean,
    onSkipSearchFocusConsumed: () -> Unit,
    onMenu: () -> Unit,
    onSearchToggle: () -> Unit,
    onThemeToggle: () -> Unit,
    onQueryChange: (String) -> Unit,
    onExitSearch: () -> Unit,
) {
    val palette = HvlgTheme.palette
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searchActive) {
        if (searchActive) {
            if (skipSearchFocus) {
                onSkipSearchFocusConsumed()
            } else {
                focusRequester.requestFocus()
            }
        }
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        navigationIcon = {
            if (searchActive) {
                IconButton(onClick = onExitSearch) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "退出搜索",
                    )
                }
            } else {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Filled.Menu, contentDescription = "目录")
                }
            }
        },
        title = {
            if (searchActive) {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = "搜索标题 / 说人话 / 成本 / 收益 / 备注",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                )
            } else {
                Column {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (sectionLabel != null) {
                        Text(
                            text = sectionLabel,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        actions = {
            if (searchActive) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "清空搜索")
                }
            } else {
                IconButton(onClick = onSearchToggle) {
                    Icon(Icons.Filled.Search, contentDescription = "搜索")
                }
                val isDark = themeMode == ThemeMode.DARK
                IconButton(onClick = onThemeToggle) {
                    ThemeModeIcon(
                        dark = isDark,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(22.dp)
                            .semantics { contentDescription = "切换明暗主题" },
                    )
                }
            }
        },
    )
}

@Composable
private fun SectionHeader(section: Section) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${section.num}. ${section.title}",
                modifier = Modifier.weight(1f),
                fontSize = 19.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${section.count} 条",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = section.intro,
            fontSize = 13.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun buildRows(
    index: ContentIndex,
    sectionId: String?,
    searching: Boolean,
    query: String,
    spec: FilterSpec,
): List<RowItem> {
    val needle = query.trim()
    // 搜索默认全书；选了「本章节」则搜索也只查当前节。非搜索态恒为当前节。
    val sections = if (searching && spec.scope == SearchScope.ALL) {
        index.sections
    } else {
        index.sections.filter { it.id == sectionId }
    }
    val rows = ArrayList<RowItem>()
    for (section in sections) {
        val cards = index.cardsBySection[section.id].orEmpty().filter { card ->
            spec.matches(card) && matchesQuery(card, needle)
        }
        if (cards.isEmpty()) continue
        rows.add(RowItem.Header(section))
        for (card in cards) rows.add(RowItem.CardRow(card))
    }
    return rows
}

private fun matchesQuery(card: GuideCard, needle: String): Boolean {
    if (needle.isEmpty()) return true
    return card.title.contains(needle, ignoreCase = true) ||
        card.plain.contains(needle, ignoreCase = true) ||
        card.cost.contains(needle, ignoreCase = true) ||
        card.benefit.contains(needle, ignoreCase = true) ||
        card.note.contains(needle, ignoreCase = true)
}

private fun openInBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "没有可以打开这个链接的应用", Toast.LENGTH_SHORT).show()
        }
}

@Composable
private fun SectionNavFooter(
    prev: Section?,
    next: Section?,
    onOpen: (Section) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (prev != null) {
            TextButton(
                onClick = { onOpen(prev) },
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text(
                    "← 上一节：${prev.num}. ${prev.title}",
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (next != null) {
            TextButton(
                onClick = { onOpen(next) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    "下一节：${next.num}. ${next.title} →",
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
