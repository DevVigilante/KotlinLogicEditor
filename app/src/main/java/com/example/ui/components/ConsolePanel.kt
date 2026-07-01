package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConsolePanel(
    consoleOutput: String,
    variables: Map<String, Any>,
    stepDelayMs: Long,
    onSetStepDelay: (Long) -> Unit,
    onClearConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val consoleScrollState = rememberScrollState()

    // Automatically scroll to the bottom of the console whenever new outputs arrive
    LaunchedEffect(consoleOutput) {
        if (consoleOutput.isNotEmpty()) {
            consoleScrollState.animateScrollTo(consoleScrollState.maxValue)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper section: Speed Control & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                Text("Walkthrough Speed:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = onClearConsole,
                modifier = Modifier.testTag("clear_console_btn")
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Console", tint = Color(0xFFEF4444))
            }
        }

        // Speed Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val speedOptions = listOf(
                0L to "Instant",
                100L to "Fast",
                500L to "Trace",
                1200L to "Slo-Mo"
            )

            speedOptions.forEach { (delay, label) ->
                val isSelected = stepDelayMs == delay
                FilterChip(
                    selected = isSelected,
                    onClick = { onSetStepDelay(delay) },
                    label = { Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color.LightGray
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Split Section: Console and Live Variables
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Variables Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "📦 Active Variables Scope Tracker",
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (variables.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No variables active in scope yet.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        variables.forEach { (name, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "val ",
                                        color = Color(0xFFF59E0B),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = name,
                                        color = Color(0xFF8B5CF6),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = when (value) {
                                        is String -> "\"$value\" (String)"
                                        is Boolean -> "$value (Boolean)"
                                        is Number -> "$value (Int)"
                                        else -> value.toString()
                                    },
                                    color = Color(0xFF10B981),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Terminal/Console Output Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(Color(0xFF090D16), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "📺 Output Console (Terminal stdout)",
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (consoleOutput.isEmpty()) "Console empty. Tap Play to execute." else consoleOutput,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (consoleOutput.contains("Error")) Color(0xFFEF4444) else Color(0xFF38BDF8),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(consoleScrollState)
                    )
                }
            }
        }
    }
}
