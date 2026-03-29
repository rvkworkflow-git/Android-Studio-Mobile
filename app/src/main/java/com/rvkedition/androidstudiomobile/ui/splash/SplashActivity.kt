package com.rvkedition.androidstudiomobile.ui.splash

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rvkedition.androidstudiomobile.R
import com.rvkedition.androidstudiomobile.backend.download.DownloadManager
import com.rvkedition.androidstudiomobile.ui.MainActivity
import com.rvkedition.androidstudiomobile.utils.ThemeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check if permission was granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            proceedToInit()
        }
    }

    private val legacyPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            proceedToInit()
        }
    }

    private var shouldProceed = mutableStateOf(false)
    private var initProgress = mutableStateOf<DownloadManager.DownloadProgress?>(null)
    private var showPermissionDialog = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SplashScreen(
                shouldProceed = shouldProceed.value,
                initProgress = initProgress.value,
                showPermissionDialog = showPermissionDialog.value,
                onGrantPermission = { requestStoragePermission() },
                onProceed = { navigateToMain() }
            )
        }

        checkPermissionsAndInit()
    }

    private fun checkPermissionsAndInit() {
        if (hasStoragePermission()) {
            proceedToInit()
        } else {
            showPermissionDialog.value = true
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        showPermissionDialog.value = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            storagePermissionLauncher.launch(intent)
        } else {
            legacyPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun proceedToInit() {
        shouldProceed.value = true
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@Composable
fun SplashScreen(
    shouldProceed: Boolean,
    initProgress: DownloadManager.DownloadProgress?,
    showPermissionDialog: Boolean,
    onGrantPermission: () -> Unit,
    onProceed: () -> Unit
) {
    val logoAlpha = remember { Animatable(0f) }
    val sloganAlpha = remember { Animatable(0f) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var initializationStarted by remember { mutableStateOf(false) }
    var initializationComplete by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf<DownloadManager.DownloadProgress?>(null) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, animationSpec = tween(1200, easing = EaseOutCubic))
        delay(400)
        sloganAlpha.animateTo(1f, animationSpec = tween(800, easing = EaseOutCubic))
    }

    LaunchedEffect(shouldProceed) {
        if (shouldProceed && !initializationStarted) {
            initializationStarted = true
            val downloadManager = DownloadManager(context)

            if (downloadManager.isInitialized()) {
                delay(1500) // Brief splash display
                onProceed()
            } else {
                scope.launch {
                    try {
                        downloadManager.initializeAll { progress ->
                            currentProgress = progress
                        }
                        initializationComplete = true
                        delay(500)
                        onProceed()
                    } catch (e: Exception) {
                        // If download fails, still proceed - components can be downloaded later
                        delay(1000)
                        onProceed()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2B2B2B)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Android Studio Mobile",
                modifier = Modifier
                    .size(120.dp)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "Android Studio Mobile",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Slogan
            Text(
                text = "Presented By RVK EDITION",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF4A88C7),
                modifier = Modifier.alpha(sloganAlpha.value),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Progress indicator
            if (currentProgress != null && !initializationComplete) {
                val progress = currentProgress!!
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (progress.phase) {
                            DownloadManager.Phase.DOWNLOADING -> "Downloading ${progress.currentItem}..."
                            DownloadManager.Phase.EXTRACTING -> "Extracting ${progress.currentItem}..."
                            DownloadManager.Phase.CONFIGURING -> "Configuring environment..."
                            DownloadManager.Phase.COMPLETE -> "Ready!"
                            DownloadManager.Phase.ERROR -> "Error: ${progress.error}"
                            else -> "Initializing..."
                        },
                        fontSize = 13.sp,
                        color = Color(0xFFBBBBBB)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = if (progress.totalItems > 0) progress.currentIndex.toFloat() / progress.totalItems else 0f,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(4.dp),
                        color = Color(0xFF4A88C7),
                        trackColor = Color(0xFF515151)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${progress.currentIndex}/${progress.totalItems}",
                        fontSize = 11.sp,
                        color = Color(0xFF808080)
                    )
                }
            } else if (!shouldProceed) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color(0xFF4A88C7),
                    strokeWidth = 3.dp
                )
            }
        }

        // Version at bottom
        Text(
            text = "v1.0.0",
            fontSize = 12.sp,
            color = Color(0xFF808080),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        // Permission Dialog
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Storage Permission Required") },
                text = {
                    Text("Android Studio Mobile needs full storage access to manage project files, SDKs, and build outputs.")
                },
                confirmButton = {
                    TextButton(onClick = onGrantPermission) {
                        Text("Grant Permission")
                    }
                },
                containerColor = Color(0xFF3C3F41),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFBBBBBB)
            )
        }
    }
}
