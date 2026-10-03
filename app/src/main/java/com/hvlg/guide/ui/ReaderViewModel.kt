package com.hvlg.guide.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hvlg.guide.data.ContentIndex
import com.hvlg.guide.data.ContentRepository
import com.hvlg.guide.data.FilterSpec
import com.hvlg.guide.data.SettingsRepository
import com.hvlg.guide.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderState(
    val loading: Boolean = true,
    val error: String? = null,
    val index: ContentIndex? = null,
    val query: String = "",
    val searchActive: Boolean = false,
    val spec: FilterSpec = FilterSpec(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** 已收藏的卡片 id 集合。 */
    val favoriteIds: Set<String> = emptySet(),
    /** 当前浏览的节（非搜索态只显示这一节）；搜索态为全书范围。 */
    val currentSectionId: String? = null,
    /** 冷启动时待恢复的卡片 id（一次性）。 */
    val restoreTargetId: String? = null,
    /** 已经做过「要不要恢复」的判断，UI 收到 true 后才能开始记录滚动位置。 */
    val restoreSettled: Boolean = false,
)

class ReaderViewModel(app: Application) : AndroidViewModel(app) {

    private val contentRepository = ContentRepository(app)
    private val settingsRepository = SettingsRepository(app)

    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    /** 进程启动时读到的上次位置，只在第一次设置发射时取用。 */
    private var initialLastCardId: String? = null
    private var sawSettings = false
    private var restoreDecided = false

    init {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { contentRepository.load() } }
                .onSuccess { content ->
                    _state.update { it.copy(loading = false, index = ContentIndex(content)) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "内容解析失败")
                    }
                }
            decideRestore()
        }

        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                if (!sawSettings) {
                    sawSettings = true
                    initialLastCardId = settings.lastCardId
                }
                _state.update {
                    it.copy(themeMode = settings.themeMode, favoriteIds = settings.favoriteIds)
                }
                decideRestore()
            }
        }
    }

    /** 等内容和设置都到位后，决定这次冷启动要不要跳回上次位置。 */
    private fun decideRestore() {
        if (restoreDecided) return
        val current = _state.value
        if (current.loading || !sawSettings) return
        restoreDecided = true
        val index = current.index
        val target = initialLastCardId?.takeIf { it != index?.cards?.firstOrNull()?.id }
        // 初始节：上次位置所在节，否则第一节
        val initialSection = initialLastCardId
            ?.let { index?.cardById?.get(it)?.secId }
            ?: index?.sections?.firstOrNull()?.id
        _state.update {
            it.copy(
                currentSectionId = initialSection,
                restoreTargetId = target,
                restoreSettled = true,
            )
        }
    }

    fun onSectionChange(sectionId: String) {
        _state.update { it.copy(currentSectionId = sectionId) }
    }

    fun onRestoreHandled() {
        _state.update { it.copy(restoreTargetId = null) }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun onSearchActiveChange(active: Boolean) {
        _state.update {
            it.copy(searchActive = active, query = if (active) it.query else "")
        }
    }

    fun onSpecChange(spec: FilterSpec) {
        _state.update { it.copy(spec = spec) }
    }

    fun toggleTheme() {
        val next = when (_state.value.themeMode) {
            ThemeMode.SYSTEM -> if (isSystemDark()) ThemeMode.LIGHT else ThemeMode.DARK
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
        }
        setThemeMode(next)
    }

    fun setThemeMode(mode: ThemeMode) {
        _state.update { it.copy(themeMode = mode) }
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun saveLastCardId(cardId: String) {
        viewModelScope.launch { settingsRepository.setLastCardId(cardId) }
    }

    fun toggleFavorite(cardId: String) {
        val favorite = cardId !in _state.value.favoriteIds
        viewModelScope.launch { settingsRepository.setFavorite(cardId, favorite) }
    }

    private fun isSystemDark(): Boolean {
        val uiMode = getApplication<Application>().resources.configuration.uiMode
        return (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}
