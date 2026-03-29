package com.rvkedition.androidstudiomobile.ui.filetree

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rvkedition.androidstudiomobile.utils.FileUtils
import com.rvkedition.androidstudiomobile.utils.ThemeManager
import java.io.File

@Composable
fun FileTreePanel(
    rootPath: String,
    modifier: Modifier = Modifier,
    onFileSelected: (File) -> Unit
) {
    var fileTree by remember(rootPath) {
        mutableStateOf(FileUtils.buildFileTree(File(rootPath)))
    }
    var flatList by remember(fileTree) {
        mutableStateOf(FileUtils.flattenTree(fileTree))
    }
    var selectedPath by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(ThemeManager.surfaceVariant)
    ) {
        // Project header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(ThemeManager.toolbarBg)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = ThemeManager.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Project",
                fontSize = 11.sp,
                color = ThemeManager.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(flatList) { node ->
                FileTreeItem(
                    node = node,
                    isSelected = node.file.absolutePath == selectedPath,
                    onClick = {
                        if (node.isDirectory) {
                            node.isExpanded = !node.isExpanded
                            flatList = FileUtils.flattenTree(fileTree)
                        } else {
                            selectedPath = node.file.absolutePath
                            onFileSelected(node.file)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FileTreeItem(
    node: FileUtils.FileNode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(if (isSelected) ThemeManager.treeSelection else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = (12 + node.depth * 16).dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.isDirectory) {
            Icon(
                if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = ThemeManager.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                if (node.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = ThemeManager.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = FileUtils.getFileIcon(node.name),
                fontSize = 12.sp,
                modifier = Modifier.width(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = node.name,
            fontSize = 12.sp,
            color = if (isSelected) androidx.compose.ui.graphics.Color.White else ThemeManager.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
