package com.rvkedition.androidstudiomobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class IDEApplication : Application() {

    companion object {
        const val CHANNEL_BUILD = "build_channel"
        const val CHANNEL_PYTHON = "python_server_channel"
        lateinit var instance: IDEApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val buildChannel = NotificationChannel(
                CHANNEL_BUILD,
                "Build Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows build progress notifications"
            }

            val pythonChannel = NotificationChannel(
                CHANNEL_PYTHON,
                "Python Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Python background server notifications"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(buildChannel)
            manager.createNotificationChannel(pythonChannel)
        }
    }

    fun getInternalFilesDir(): String = filesDir.absolutePath

    fun getInternalCacheDir(): String = cacheDir.absolutePath
}
