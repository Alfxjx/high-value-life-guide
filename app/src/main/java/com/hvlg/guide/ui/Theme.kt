package com.hvlg.guide.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.hvlg.guide.data.ThemeMode

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3451B2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF18794E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9F2E5),
    onSecondaryContainer = Color(0xFF002114),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFEFF0F3),
    onSurfaceVariant = Color(0xFF5A5C63),
    outline = Color(0xFFB9BBC2),
    outlineVariant = Color(0xFFE2E2E3),
    error = Color(0xFFB8272C),
    scrim = Color(0x99000000),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA8B1FF),
    onPrimary = Color(0xFF1A2678),
    primaryContainer = Color(0xFF2C3A8F),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFF3DD68C),
    onSecondary = Color(0xFF00391F),
    secondaryContainer = Color(0xFF13412C),
    onSecondaryContainer = Color(0xFFC7F2DD),
    background = Color(0xFF1B1B1F),
    onBackground = Color(0xFFE4E1E6),
    surface = Color(0xFF202127),
    onSurface = Color(0xFFE4E1E6),
    surfaceVariant = Color(0xFF2B2B2F),
    onSurfaceVariant = Color(0xFFA4A8AE),
    outline = Color(0xFF6A6D75),
    outlineVariant = Color(0xFF2E2E32),
    error = Color(0xFFF66F81),
    scrim = Color(0xCC000000),
)

/** Material3 之外的语义色：说人话高亮块、等级徽章、性价比色点、搜索命中底色。 */
data class HvlgPalette(
    val plainContainer: Color,
    val plainBorder: Color,
    val tagText: Color,
    val link: Color,
    val highlight: Color,
    val gradeA: Color,
    val gradeAContainer: Color,
    val gradeB: Color,
    val gradeBContainer: Color,
    val gradeC: Color,
    val gradeCContainer: Color,
    val ratioHighest: Color,
    val ratioHighestContainer: Color,
    val ratioHigh: Color,
    val ratioHighContainer: Color,
    val ratioNormal: Color,
    val ratioNormalContainer: Color,
)

private val LightPalette = HvlgPalette(
    plainContainer = Color(0xFFEDEEFF),
    plainBorder = Color(0xFF3451B2),
    tagText = Color(0xFF5A5C63),
    link = Color(0xFF3451B2),
    highlight = Color(0x59EAB308),
    gradeA = Color(0xFF18794E),
    gradeAContainer = Color(0xFFDFF3E8),
    gradeB = Color(0xFF915930),
    gradeBContainer = Color(0xFFFAF0DC),
    gradeC = Color(0xFF565A5F),
    gradeCContainer = Color(0xFFECEDF0),
    ratioHighest = Color(0xFF3451B2),
    ratioHighestContainer = Color(0xFFE4E7FF),
    ratioHigh = Color(0xFF18794E),
    ratioHighContainer = Color(0xFFDFF3E8),
    ratioNormal = Color(0xFF565A5F),
    ratioNormalContainer = Color(0xFFECEDF0),
)

private val DarkPalette = HvlgPalette(
    plainContainer = Color(0xFF23263A),
    plainBorder = Color(0xFFA8B1FF),
    tagText = Color(0xFFA4A8AE),
    link = Color(0xFFA8B1FF),
    highlight = Color(0x66EAB308),
    gradeA = Color(0xFF3DD68C),
    gradeAContainer = Color(0xFF16301F),
    gradeB = Color(0xFFF9B44E),
    gradeBContainer = Color(0xFF322616),
    gradeC = Color(0xFFA4A8AE),
    gradeCContainer = Color(0xFF2A2A30),
    ratioHighest = Color(0xFFA8B1FF),
    ratioHighestContainer = Color(0xFF2A2F52),
    ratioHigh = Color(0xFF3DD68C),
    ratioHighContainer = Color(0xFF16301F),
    ratioNormal = Color(0xFFA4A8AE),
    ratioNormalContainer = Color(0xFF2A2A30),
)

val LocalHvlgPalette = staticCompositionLocalOf { LightPalette }

object HvlgTheme {
    val palette: HvlgPalette
        @Composable get() = LocalHvlgPalette.current
}

@Composable
fun HvlgTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalHvlgPalette provides if (dark) DarkPalette else LightPalette) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            content = content,
        )
    }
}
