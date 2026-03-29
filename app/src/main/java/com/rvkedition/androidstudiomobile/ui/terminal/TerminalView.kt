package com.rvkedition.androidstudiomobile.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rvkedition.androidstudiomobile.backend.cpp.NativeBridge
import com.rvkedition.androidstudiomobile.backend.python.WebSocketClient
import com.rvkedition.androidstudiomobile.utils.ThemeManager
import kotlinx.coroutines.launch

@Composable
fun TerminalView(
    workingDir: String,
    modifier: Modifier = Modifier
) {
    var commandInput by remember { mutableStateOf("") }
    val outputLines = remember { mutableStateListOf<TerminalLine>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val nativeBridge = remember { NativeBridge.getInstance() }
    var currentDir by remember { mutableStateOf(workingDir) }

    // Listen for WebSocket messages
    LaunchedEffect(Unit) {
        WebSocketClient.getInstance().messages.collect { message ->
            val type = message.get("type")?.asString
            when (type) {
                "output" -> {
                    val data = message.get("data")?.asString ?: ""
                    outputLines.add(TerminalLine(data, TerminalLine.Type.OUTPUT))
                }
                "exit" -> {
                    val code = message.get("code")?.asInt ?: -1
                    outputLines.add(TerminalLine("Process exited with code $code", TerminalLine.Type.INFO))
                }
                "error" -> {
                    val msg = message.get("message")?.asString ?: "Unknown error"
                    outputLines.add(TerminalLine("Error: $msg", TerminalLine.Type.ERROR))
                }
                "build_log" -> {
                    val data = message.getAsJsonObject("data")
                    val raw = data?.get("raw")?.asString ?: ""
                    val level = data?.get("level")?.asString ?: "info"
                    val lineType = when (level) {
                        "error" -> TerminalLine.Type.ERROR
                        "warning" -> TerminalLine.Type.WARNING
                        "success" -> TerminalLine.Type.SUCCESS
                        else -> TerminalLine.Type.OUTPUT
                    }
                    outputLines.add(TerminalLine(raw, lineType))
                }
            }
            if (outputLines.isNotEmpty()) {
                scope.launch { listState.animateScrollToItem(outputLines.size - 1) }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThemeManager.terminalBg)
    ) {
        // Terminal header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(ThemeManager.toolbarBg)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Terminal",
                fontSize = 11.sp,
                color = ThemeManager.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = currentDir,
                fontSize = 10.sp,
                color = ThemeManager.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { outputLines.clear() },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = ThemeManager.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Output area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(outputLines) { line ->
                Text(
                    text = line.text,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = when (line.type) {
                        TerminalLine.Type.COMMAND -> Color(0xFF6A8759)
                        TerminalLine.Type.OUTPUT -> ThemeManager.onSurface
                        TerminalLine.Type.ERROR -> Color(0xFFFF6B68)
                        TerminalLine.Type.WARNING -> Color(0xFFBBB529)
                        TerminalLine.Type.SUCCESS -> Color(0xFF6A8759)
                        TerminalLine.Type.INFO -> Color(0xFF4A88C7)
                    },
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        // Command input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeManager.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\$ ",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF6A8759)
            )

            BasicTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = ThemeManager.onSurface
                ),
                cursorBrush = SolidColor(ThemeManager.primary),
                modifier = Modifier.weight(1f),
                singleLine = true,
                onTextLayout = {},
            )

            // Execute button
            TextButton(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        val cmd = commandInput.trim()
                        outputLines.add(TerminalLine("\$ $cmd", TerminalLine.Type.COMMAND))
                        commandInput = ""

                        // Handle cd command
                        if (cmd.startsWith("cd ")) {
                            val newDir = cmd.removePrefix("cd ").trim()
                            val resolved = if (newDir.startsWith("/")) newDir else "$currentDir/$newDir"
                            val dir = java.io.File(resolved).canonicalFile
                            if (dir.exists() && dir.isDirectory) {
                                currentDir = dir.absolutePath
                                outputLines.add(TerminalLine(currentDir, TerminalLine.Type.INFO))
                            } else {
                                outputLines.add(TerminalLine("cd: no such directory: $newDir", TerminalLine.Type.ERROR))
                            }
                        } else {
                            // Execute via JNI
                            scope.launch {
                                try {
                                    val result = nativeBridge.executeCommandAsync(cmd, currentDir)
                                    result.lines().forEach { line ->
                                        if (line.isNotEmpty()) {
                                            outputLines.add(TerminalLine(line, TerminalLine.Type.OUTPUT))
                                        }
                                    }
                                } catch (e: Exception) {
                                    outputLines.add(TerminalLine("Error: ${e.message}", TerminalLine.Type.ERROR))
                                }
                                if (outputLines.isNotEmpty()) {
                                    listState.animateScrollToItem(outputLines.size - 1)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Run", fontSize = 11.sp, color = ThemeManager.primary)
            }
        }
    }
}

data class TerminalLine(
    val text: String,
    val type: Type
) {
    enum class Type {
        COMMAND, OUTPUT, ERROR, WARNING, SUCCESS, INFO
    }
}
