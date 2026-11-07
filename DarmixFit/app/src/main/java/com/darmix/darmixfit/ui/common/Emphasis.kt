package com.darmix.darmixfit.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private const val OPEN = '«'
private const val CLOSE = '»'

fun emphasized(text: String, style: SpanStyle = SpanStyle(fontWeight = FontWeight.ExtraBold)): AnnotatedString =
    buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val start = text.indexOf(OPEN, index)
            if (start < 0) {
                append(text.substring(index))
                break
            }
            val end = text.indexOf(CLOSE, start + 1)
            if (end < 0) {
                append(text.substring(index))
                break
            }
            append(text.substring(index, start))
            withStyle(style) { append(text.substring(start + 1, end)) }
            index = end + 1
        }
    }
