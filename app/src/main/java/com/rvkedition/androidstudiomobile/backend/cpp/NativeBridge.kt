package com.rvkedition.androidstudiomobile.backend.cpp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JNI bridge to native C++ backend.
 * Handles shell execution, process management, and APK build engine.
 */
class NativeBridge {

    companion object {
        init {
            System.loadLibrary("native-bridge")
        }

        private val instance = NativeBridge()

        fun getInstance(): NativeBridge = instance
    }

    // Execute a shell command synchronously and return output
    external fun executeCommand(command: String, workingDir: String, envVars: String?): String

    // Spawn a background process, returns PID
    external fun spawnProcess(command: String, workingDir: String, envVars: String?): Long

    // Kill a process by PID
    external fun killProcess(pid: Long): Int

    // Set file permissions (chmod)
    external fun setFilePermissions(filePath: String, mode: Int): Int

    // Start APK build
    external fun startBuild(projectPath: String, javaHome: String, sdkPath: String, buildType: String): String

    // Set environment variable
    external fun setEnvironmentVariable(key: String, value: String)

    // Get system info (arch, cpus, etc.)
    external fun getSystemInfo(): String

    // Kotlin coroutine wrappers
    suspend fun executeCommandAsync(command: String, workingDir: String, envVars: String? = null): String {
        return withContext(Dispatchers.IO) {
            executeCommand(command, workingDir, envVars)
        }
    }

    suspend fun startBuildAsync(projectPath: String, javaHome: String, sdkPath: String, buildType: String): String {
        return withContext(Dispatchers.IO) {
            startBuild(projectPath, javaHome, sdkPath, buildType)
        }
    }

    fun chmod755(filePath: String): Boolean {
        return setFilePermissions(filePath, 493) == 0 // 0755 octal = 493 decimal
    }
}
