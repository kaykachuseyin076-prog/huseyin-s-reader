package com.example.ui.reader.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    lineHeightSp: Float = 22f
) {
    val lines = markdown.replace("\r\n", "\n").split("\n")

    Column(modifier = modifier.fillMaxWidth()) {
        for ((index, rawLine) in lines.withIndex()) {
            val line = rawLine.trimEnd()
            when {
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                line.startsWith("### ") -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("### ")),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        lineHeight = (lineHeightSp + 4).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                line.startsWith("## ") -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("## ")),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        lineHeight = (lineHeightSp + 6).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                line.startsWith("# ") -> {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("# ")),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        lineHeight = (lineHeightSp + 8).sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") || line.trimStart().startsWith("• ") -> {
                    val content = line.trimStart().removePrefix("- ").removePrefix("* ").removePrefix("• ")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(textColor.copy(alpha = 0.7f))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = parseInlineMarkdown(content),
                            fontSize = 14.sp,
                            color = textColor,
                            lineHeight = lineHeightSp.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Regex("^\\s*\\d+\\.\\s+.*").matches(line) -> {
                    val dotIdx = line.indexOf('.')
                    val number = line.substring(0, dotIdx + 1).trim()
                    val content = line.substring(dotIdx + 1).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = number,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = parseInlineMarkdown(content),
                            fontSize = 14.sp,
                            color = textColor,
                            lineHeight = lineHeightSp.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        fontSize = 14.sp,
                        color = textColor,
                        lineHeight = lineHeightSp.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Parses bold (**text**), italic (*text*), and code (`text`) inline markdown spans.
 */
fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Bold: **text**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        val boldText = text.substring(i + 2, end)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(boldText)
                        pop()
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Inline Code: `text`
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        val codeText = text.substring(i + 1, end)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x22000000),
                                fontSize = 13.sp
                            )
                        )
                        append(codeText)
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Italic: *text* (when not followed or preceded by *)
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1 && (end + 1 >= text.length || text[end + 1] != '*')) {
                        val italicText = text.substring(i + 1, end)
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(italicText)
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
