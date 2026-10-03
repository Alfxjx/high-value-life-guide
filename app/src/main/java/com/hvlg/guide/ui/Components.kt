package com.hvlg.guide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hvlg.guide.data.Ratio

private val PillShape = RoundedCornerShape(percent = 50)

@Composable
private fun Pill(
    text: String,
    contentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
) {
    Box(
        modifier = modifier
            .background(containerColor, PillShape)
            .border(1.dp, containerColor, PillShape)
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun GradeBadge(grade: String) {
    val palette = HvlgTheme.palette
    val (fg, bg) = when (grade) {
        "A" -> palette.gradeA to palette.gradeAContainer
        "B" -> palette.gradeB to palette.gradeBContainer
        else -> palette.gradeC to palette.gradeCContainer
    }
    Pill(text = "$grade 级", contentColor = fg, containerColor = bg)
}

@Composable
fun RatioBadge(ratio: String) {
    val palette = HvlgTheme.palette
    val (fg, bg) = when (Ratio.of(ratio)) {
        Ratio.HIGHEST -> palette.ratioHighest to palette.ratioHighestContainer
        Ratio.HIGH -> palette.ratioHigh to palette.ratioHighContainer
        Ratio.NORMAL -> palette.ratioNormal to palette.ratioNormalContainer
    }
    Pill(text = "性价比 $ratio", contentColor = fg, containerColor = bg)
}

@Composable
fun TagChip(text: String) {
    val palette = HvlgTheme.palette
    Pill(
        text = text,
        contentColor = palette.tagText,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
fun RatioDot(ratio: String, modifier: Modifier = Modifier) {
    val palette = HvlgTheme.palette
    val color = when (Ratio.of(ratio)) {
        Ratio.HIGHEST -> palette.ratioHighest
        Ratio.HIGH -> palette.ratioHigh
        Ratio.NORMAL -> palette.ratioNormal
    }
    Box(modifier.size(7.dp).background(color, CircleShape))
}
