package com.rvkedition.androidstudiomobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rvkedition.androidstudiomobile.backend.build.BuildService
import com.rvkedition.androidstudiomobile.utils.ThemeManager
import kotlinx.coroutines.launch

@Composable
fun BuildOutputView(modifier: Modifier = Modifier) {
    val buildState by BuildService.buildState.collectAsState()
    val buildLogs = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        BuildService.buildLogs.collect { log ->
            buildLogs.add(log)
            scope.launch {
                if (buildLogs.isNotEmpty()) {
                    listState.animateScrollToItem(buildLogs.size - 1)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThemeManager.terminalBg)
    ) {
        // Build status header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeManager.toolbarBg)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (buildState.isBuilding) "Building..." else "Build Output",
                fontSize = 12.sp,
                color = ThemeManager.onSurface,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.weight(1f))

            buildState.success?.let { success ->
                Text(
                    text = if (success) "BUILD SUCCESSFUL" else "BUILD FAILED",
                    fontSize = 12.sp,
                    color = if (success) Color(0xFF6A8759) else Color(0xFFFF6B68),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Build log output
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(buildLogs) { line ->
                val color = when {
                    line.contains("ERROR") || line.contains("FAILED") -> Color(0xFFFF6B68)
                    line.contains("WARNING") || line.contains("WARN") -> Color(0xFFBBB529)
                    line.contains("BUILD SUCCESSFUL") -> Color(0xFF6A8759)
                    line.startsWith("> Task") -> Color(0xFF4A88C7)
                    else -> ThemeManager.onSurface
                }

                Text(
                    text = line,
                    fontSize = 11.sp,
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
