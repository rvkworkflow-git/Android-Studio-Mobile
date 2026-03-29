package com.rvkedition.androidstudiomobile.backend.build

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rvkedition.androidstudiomobile.IDEApplication
import com.rvkedition.androidstudiomobile.R
import com.rvkedition.androidstudiomobile.backend.cpp.NativeBridge
import com.rvkedition.androidstudiomobile.backend.download.DownloadManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Foreground service that manages APK build processes.
 * Uses C++ JNI backend for actual build execution.
 */
class BuildService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val nativeBridge = NativeBridge.getInstance()

    data class BuildState(
        val isBuilding: Boolean = false,
        val projectPath: String = "",
        val buildType: String = "",
        val progress: String = "",
        val success: Boolean? = null,
        val error: String? = null,
        val outputApkPath: String? = null
    )

    companion object {
        const val NOTIFICATION_ID = 2002
        const val EXTRA_PROJECT_PATH = "project_path"
        const val EXTRA_BUILD_TYPE = "build_type"
        const val EXTRA_JAVA_VERSION = "java_version"

        private val _buildState = MutableStateFlow(BuildState())
        val buildState: StateFlow<BuildState> = _buildState

        private val _buildLogs = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 500)
        val buildLogs: SharedFlow<String> = _buildLogs
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification("Build starting..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val projectPath = intent?.getStringExtra(EXTRA_PROJECT_PATH) ?: return START_NOT_STICKY
        val buildType = intent.getStringExtra(EXTRA_BUILD_TYPE) ?: "debug"
        val javaVersion = intent.getIntExtra(EXTRA_JAVA_VERSION, 17)

        startBuild(projectPath, buildType, javaVersion)
        return START_NOT_STICKY
    }

    private fun startBuild(projectPath: String, buildType: String, javaVersion: Int) {
        scope.launch {
            _buildState.value = BuildState(isBuilding = true, projectPath = projectPath, buildType = buildType)
            emitLog("Starting $buildType build for: $projectPath")

            try {
                val downloadManager = DownloadManager(applicationContext)
                val javaHome = downloadManager.getJavaHome(javaVersion)
                val sdkPath = downloadManager.getSdkPath()

                // Set JAVA_HOME dynamically
                nativeBridge.setEnvironmentVariable("JAVA_HOME", javaHome)
                nativeBridge.setEnvironmentVariable("ANDROID_HOME", sdkPath)
                nativeBridge.setEnvironmentVariable("ANDROID_SDK_ROOT", sdkPath)
                emitLog("JAVA_HOME set to: $javaHome")

                // Ensure gradlew has execute permissions
                val gradlewPath = "$projectPath/gradlew"
                if (java.io.File(gradlewPath).exists()) {
                    nativeBridge.chmod755(gradlewPath)
                    emitLog("chmod 755 applied to gradlew")
                }

                // Build environment variables
                val envVars = buildString {
                    appendLine("JAVA_HOME=$javaHome")
                    appendLine("ANDROID_HOME=$sdkPath")
                    appendLine("ANDROID_SDK_ROOT=$sdkPath")
                    appendLine("PATH=$javaHome/bin:$sdkPath/platform-tools:$sdkPath/build-tools/34.0.0:\$PATH")
                }

                // Execute build using sh to bypass execute restrictions
                val gradleTask = when (buildType) {
                    "debug" -> "assembleDebug"
                    "release" -> "assembleRelease"
                    "bundle" -> "bundleRelease"
                    "clean" -> "clean"
                    else -> buildType
                }

                emitLog("> Task :$gradleTask")
                val command = "sh gradlew $gradleTask --no-daemon --stacktrace"
                val result = nativeBridge.executeCommandAsync(command, projectPath, envVars)

                // Parse and emit build output line by line
                result.lines().forEach { line ->
                    emitLog(line)
                }

                val success = !result.contains("BUILD FAILED") && !result.contains("FAILURE:")
                val apkPath = if (success && buildType != "clean") {
                    findApk(projectPath, buildType)
                } else null

                _buildState.value = BuildState(
                    isBuilding = false,
                    projectPath = projectPath,
                    buildType = buildType,
                    success = success,
                    outputApkPath = apkPath,
                    progress = if (success) "BUILD SUCCESSFUL" else "BUILD FAILED"
                )

                emitLog(if (success) "BUILD SUCCESSFUL" else "BUILD FAILED")

            } catch (e: Exception) {
                emitLog("BUILD ERROR: ${e.message}")
                _buildState.value = BuildState(
                    isBuilding = false,
                    projectPath = projectPath,
                    buildType = buildType,
                    success = false,
                    error = e.message
                )
            }

            stopSelf()
        }
    }

    private fun findApk(projectPath: String, buildType: String): String? {
        val buildDir = java.io.File("$projectPath/app/build/outputs/apk/$buildType")
        if (buildDir.exists()) {
            return buildDir.listFiles()?.firstOrNull { it.name.endsWith(".apk") }?.absolutePath
        }
        return null
    }

    private suspend fun emitLog(message: String) {
        _buildLogs.emit(message)
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, IDEApplication.CHANNEL_BUILD)
            .setContentTitle("Building Project")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_build)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
