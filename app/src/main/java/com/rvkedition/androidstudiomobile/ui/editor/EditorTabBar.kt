package com.rvkedition.androidstudiomobile.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rvkedition.androidstudiomobile.ui.OpenFile
import com.rvkedition.androidstudiomobile.utils.ThemeManager

@Composable
fun EditorTabBar(
    files: List<OpenFile>,
    activeIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(ThemeManager.toolbarBg)
            .horizontalScroll(rememberScrollState())
    ) {
        files.forEachIndexed { index, file ->
            val isActive = index == activeIndex
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .background(if (isActive) ThemeManager.tabActive else ThemeManager.tabInactive)
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (if (file.modified) "* " else "") + file.file.name,
                    fontSize = 11.sp,
                    color = if (isActive) ThemeManager.onSurface else ThemeManager.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close tab",
                    tint = ThemeManager.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onTabClosed(index) }
                )
            }

            // Tab separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(ThemeManager.border)
            )
        }
    }
}
