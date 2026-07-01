package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

@Composable
fun KotlinCodeViewer(
    code: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val highlightedCode = rememberHighlightedCode(code)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        // Mock IDE Header Frame
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // MacOS Window Controls Styling
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Main.kt (Kotlin compiler preview)",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            // Copy to Clipboard
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(code))
                    Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(28.dp).testTag("copy_code_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Code",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Main Code Editor Gutter & Text
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0B0F19), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .padding(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Line Numbers Gutter
                val linesCount = code.lines().size
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..linesCount) {
                        Text(
                            text = i.toString(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(end = 12.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))

                // Scrollable Code Display
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = highlightedCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Custom High-fidelity Jetpack Compose Syntax Highlighter.
 */
@Composable
fun rememberHighlightedCode(code: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = code.lines()
        for (idx in lines.indices) {
            val line = lines[idx]
            highlightLine(line, this)
            if (idx < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun highlightLine(line: String, builder: AnnotatedString.Builder) {
    if (line.trim().startsWith("//")) {
        // Comment Line
        builder.withStyle(style = SpanStyle(color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)) {
            append(line)
        }
        return
    }

    // Split words with simple tokenizing, or apply regex replacements in order.
    // For extreme reliability in a recursive parser, let's find matched groups and append them.
    val combinedPattern = Pattern.compile(
        "(?<keyword>\\b(fun|var|if|else|repeat|while|todo)\\b)" +
        "|(?<string>\"[^\"]*\")" +
        "|(?<number>\\b\\d+\\b)" +
        "|(?<brace>[{}()=])"
    )

    val matcher = combinedPattern.matcher(line)
    var lastIdx = 0
    while (matcher.find()) {
        // Add preceding unmatched string
        if (matcher.start() > lastIdx) {
            builder.append(line.substring(lastIdx, matcher.start()))
        }

        val keyword = matcher.group("keyword")
        val string = matcher.group("string")
        val number = matcher.group("number")
        val brace = matcher.group("brace")

        when {
            keyword != null -> {
                builder.withStyle(style = SpanStyle(color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)) {
                    append(keyword)
                }
            }
            string != null -> {
                builder.withStyle(style = SpanStyle(color = Color(0xFF22D3EE))) {
                    append(string)
                }
            }
            number != null -> {
                builder.withStyle(style = SpanStyle(color = Color(0xFFEF4444))) {
                    append(number)
                }
            }
            brace != null -> {
                builder.withStyle(style = SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)) {
                    append(brace)
                }
            }
        }
        lastIdx = matcher.end()
    }

    if (lastIdx < line.length) {
        builder.append(line.substring(lastIdx))
    }
}
