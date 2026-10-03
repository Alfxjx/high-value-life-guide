package com.hvlg.guide.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/** 把 text 里所有命中 query 的片段刷上底色，大小写不敏感。 */
fun highlightMatches(text: String, query: String, background: Color): AnnotatedString {
    val needle = query.trim()
    if (needle.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        append(text)
        var from = 0
        while (from <= text.length - needle.length) {
            val hit = text.indexOf(needle, from, ignoreCase = true)
            if (hit < 0) break
            addStyle(SpanStyle(background = background), hit, hit + needle.length)
            from = hit + needle.length
        }
    }
}
