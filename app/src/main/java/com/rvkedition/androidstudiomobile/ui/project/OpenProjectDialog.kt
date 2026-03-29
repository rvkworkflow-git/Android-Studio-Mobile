package com.rvkedition.androidstudiomobile.ui.project

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rvkedition.androidstudiomobile.utils.ThemeManager
import java.io.File

@Composable
fun OpenProjectDialog(
    onDismiss: () -> Unit,
    onProjectOpened: (String) -> Unit
) {
    var currentPath by remember {
        mutableStateOf(
            Environment.getExternalStorageDirectory().absolutePath + "/AndroidStudioMobile/Projects"
        )
    }
    var files by remember { mutableStateOf(listFiles(currentPath)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(containerColor = ThemeManager.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Open Project",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeManager.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Current path
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThemeManager.surfaceVariant)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = ThemeManager.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentPath,
                        fontSize = 11.sp,
                        color = ThemeManager.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigate up button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clickable {
                            val parent = File(currentPath).parent
                            if (parent != null) {
                                currentPath = parent
                                files = listFiles(currentPath)
                            }
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Go up",
                        tint = ThemeManager.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("..", fontSize = 13.sp, color = ThemeManager.onSurface)
                }

                // File list
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(files) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clickable {
                                    if (file.isDirectory) {
                                        // Check if this is a project directory
                                        val isProject = File(file, "build.gradle").exists() ||
                                                File(file, "build.gradle.kts").exists() ||
                                                File(file, "pubspec.yaml").exists() ||
                                                File(file, "package.json").exists() ||
                                                File(file, "CMakeLists.txt").exists() ||
                                                File(file, "main.py").exists()

                                        if (isProject) {
                                            onProjectOpened(file.absolutePath)
                                        } else {
                                            currentPath = file.absolutePath
                                            files = listFiles(currentPath)
                                        }
                                    }
                                }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = if (file.isDirectory) ThemeManager.primary else ThemeManager.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = file.name,
                                fontSize = 13.sp,
                                color = ThemeManager.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Cancel button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ThemeManager.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

private fun listFiles(path: String): List<File> {
    val dir = File(path)
    dir.mkdirs()
    return dir.listFiles()
        ?.filter { !it.name.startsWith(".") }
        ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        ?: emptyList()
}
