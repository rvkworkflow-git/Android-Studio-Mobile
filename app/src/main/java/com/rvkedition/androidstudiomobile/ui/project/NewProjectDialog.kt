package com.rvkedition.androidstudiomobile.ui.project

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rvkedition.androidstudiomobile.utils.ProjectCreator
import com.rvkedition.androidstudiomobile.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onProjectCreated: (String) -> Unit
) {
    var projectName by remember { mutableStateOf("MyApplication") }
    var packageName by remember { mutableStateOf("com.example.myapp") }
    var selectedType by remember { mutableStateOf(ProjectCreator.ProjectType.ANDROID_KOTLIN) }
    var useCompose by remember { mutableStateOf(true) }
    var minSdk by remember { mutableIntStateOf(26) }
    var isCreating by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val projectTypes = listOf(
        ProjectCreator.ProjectType.ANDROID_KOTLIN to "Android (Kotlin)",
        ProjectCreator.ProjectType.ANDROID_JAVA to "Android (Java)",
        ProjectCreator.ProjectType.PYTHON to "Python",
        ProjectCreator.ProjectType.REACT_NATIVE to "React Native",
        ProjectCreator.ProjectType.CPP to "C++",
        ProjectCreator.ProjectType.FLUTTER to "Flutter (Dart)"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = ThemeManager.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = "Create New Project",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeManager.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Project Type Selection
                    Text("Project Type", fontSize = 13.sp, color = ThemeManager.onSurface.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))

                    projectTypes.forEach { (type, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(
                                    if (selectedType == type) ThemeManager.treeSelection.copy(alpha = 0.3f)
                                    else Color.Transparent
                                )
                                .clickable { selectedType = type }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ThemeManager.primary,
                                    unselectedColor = ThemeManager.onSurface.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontSize = 13.sp, color = ThemeManager.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Project Name
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project Name", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = ThemeManager.onSurface),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeManager.primary,
                            unfocusedBorderColor = ThemeManager.border,
                            cursorColor = ThemeManager.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Package Name (for Android/Flutter)
                    if (selectedType in listOf(
                            ProjectCreator.ProjectType.ANDROID_KOTLIN,
                            ProjectCreator.ProjectType.ANDROID_JAVA,
                            ProjectCreator.ProjectType.FLUTTER
                        )
                    ) {
                        OutlinedTextField(
                            value = packageName,
                            onValueChange = { packageName = it },
                            label = { Text("Package Name", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = ThemeManager.onSurface),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemeManager.primary,
                                unfocusedBorderColor = ThemeManager.border,
                                cursorColor = ThemeManager.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Compose Toggle (Android Kotlin only)
                    if (selectedType == ProjectCreator.ProjectType.ANDROID_KOTLIN) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = useCompose,
                                onCheckedChange = { useCompose = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ThemeManager.primary,
                                    uncheckedColor = ThemeManager.onSurface.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use Jetpack Compose", fontSize = 13.sp, color = ThemeManager.onSurface)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Min SDK
                        Text("Minimum SDK: API $minSdk", fontSize = 12.sp, color = ThemeManager.onSurface.copy(alpha = 0.7f))
                        Slider(
                            value = minSdk.toFloat(),
                            onValueChange = { minSdk = it.toInt() },
                            valueRange = 21f..34f,
                            steps = 12,
                            colors = SliderDefaults.colors(
                                thumbColor = ThemeManager.primary,
                                activeTrackColor = ThemeManager.primary
                            )
                        )
                    }

                    // Error message
                    errorMsg?.let {
                        Text(it, fontSize = 12.sp, color = Color(0xFFFF6B68), modifier = Modifier.padding(top = 8.dp))
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ThemeManager.onSurface.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (projectName.isBlank()) {
                                errorMsg = "Project name is required"
                                return@Button
                            }
                            isCreating = true
                            errorMsg = null

                            scope.launch {
                                val location = Environment.getExternalStorageDirectory().absolutePath + "/AndroidStudioMobile/Projects"
                                val config = ProjectCreator.ProjectConfig(
                                    name = projectName,
                                    packageName = packageName,
                                    type = selectedType,
                                    location = location,
                                    minSdk = minSdk,
                                    useCompose = useCompose
                                )

                                val success = withContext(Dispatchers.IO) {
                                    ProjectCreator.createProject(config)
                                }

                                isCreating = false
                                if (success) {
                                    onProjectCreated("$location/$projectName")
                                } else {
                                    errorMsg = "Failed to create project. Directory may already exist."
                                }
                            }
                        },
                        enabled = !isCreating,
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.primary)
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Create")
                    }
                }
            }
        }
    }
}
