package com.rvkedition.androidstudiomobile.backend.download

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Auto-download initialization engine.
 * Downloads, extracts, and configures core SDKs, binaries, and branding assets
 * into internal storage to avoid FAT32 execute restrictions.
 */
class DownloadManager(private val context: Context) {

    data class DownloadItem(
        val name: String,
        val url: String,
        val extractTo: String,
        val required: Boolean = true
    )

    data class DownloadProgress(
        val currentItem: String = "",
        val currentIndex: Int = 0,
        val totalItems: Int = 0,
        val bytesDownloaded: Long = 0,
        val totalBytes: Long = 0,
        val phase: Phase = Phase.IDLE,
        val error: String? = null
    )

    enum class Phase {
        IDLE, DOWNLOADING, EXTRACTING, CONFIGURING, COMPLETE, ERROR
    }

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress

    private val filesDir = context.filesDir.absolutePath

    private val downloadItems = listOf(
        DownloadItem(
            "SDK Tools",
            "https://www.dropbox.com/scl/fi/3ysun0jhiu9frbq8bklnh/rvk-sdk-tools.zip?rlkey=t8mj9vksh2m64g45y2yq490vg&st=065kbp3t&dl=1",
            "$filesDir/sdk-tools"
        ),
        DownloadItem(
            "JDK 17",
            "https://www.dropbox.com/scl/fi/jga2lx146hnel1ya2oxym/rvk-jdk17.zip?rlkey=9vtahoq4l6vyl9c461czq0lp8&st=vboenjhb&dl=1",
            "$filesDir/jdk17"
        ),
        DownloadItem(
            "JDK 21",
            "https://www.dropbox.com/scl/fi/e9qrujvyjyvdo7lawy3oa/rvk-jdk21.zip?rlkey=v6s9r1ia72l4i0ovfgiunpt6z&st=qranog7g&dl=1",
            "$filesDir/jdk21"
        ),
        DownloadItem(
            "NDK",
            "https://www.dropbox.com/scl/fi/7iw11jdx4nhein5t7jmne/rvk-ndk.zip?rlkey=8pkfi60270j7lysv6l2sbbbs7&st=c8qsgtpd&dl=1",
            "$filesDir/ndk"
        ),
        DownloadItem(
            "Gradle 8.14.4",
            "https://www.dropbox.com/scl/fi/eh6u3dv0vyfyvyrd11n1f/rvk-gradle-8.14.4.zip?rlkey=cszyti4f307qy0o0h7xe9u0et&st=zc5eluoc&dl=1",
            "$filesDir/gradle-8.14.4"
        ),
        DownloadItem(
            "Gradle 9.3.0",
            "https://www.dropbox.com/scl/fi/s1sqyzqtteums31yz1es5/rvk-gradle-9.3.0.zip?rlkey=ivgqg6b2kexgl3ylenckdv145&st=9pabtglh&dl=1",
            "$filesDir/gradle-9.3.0"
        ),
        DownloadItem(
            "Gradle 9.4.0",
            "https://www.dropbox.com/scl/fi/73xs9relzom0ktxsblga7/rvk-gradle-9.4.0.zip?rlkey=b3uvsceldc5692d0xebee9gt7&st=eklvpj3e&dl=1",
            "$filesDir/gradle-9.4.0"
        ),
        DownloadItem(
            "Flutter SDK",
            "https://www.dropbox.com/scl/fi/qfrzvql7w5jh27rh53dd9/rvk-flutter.zip?rlkey=r94cm1muuq4l485vzccxjmfjk&st=k3bdmzg4&dl=1",
            "$filesDir/flutter"
        ),
        DownloadItem(
            "Node.js",
            "https://www.dropbox.com/scl/fi/zk9gheaqkxj3gcy1urcvr/rvk-nodejs.zip?rlkey=vrm2gu8xdksamcnvha7xhvtly&st=0iqfkysw&dl=1",
            "$filesDir/nodejs"
        ),
        DownloadItem(
            "Python",
            "https://www.dropbox.com/scl/fi/jgvlipu2pgf9zt84j1ton/rvk-python.zip?rlkey=t3tbzk5uyn8216c57yqat5974&st=5xfh6rv8&dl=1",
            "$filesDir/python"
        )
    )

    fun isInitialized(): Boolean {
        // Check if critical components exist
        return File("$filesDir/jdk17/bin").exists() &&
                File("$filesDir/sdk-tools").exists()
    }

    suspend fun initializeAll(onProgress: (DownloadProgress) -> Unit = {}) {
        val total = downloadItems.size

        for ((index, item) in downloadItems.withIndex()) {
            if (File(item.extractTo).exists() && File(item.extractTo).list()?.isNotEmpty() == true) {
                // Already downloaded and extracted
                val progress = DownloadProgress(
                    currentItem = item.name,
                    currentIndex = index + 1,
                    totalItems = total,
                    phase = Phase.COMPLETE
                )
                _progress.value = progress
                onProgress(progress)
                continue
            }

            try {
                // Download phase
                val progress1 = DownloadProgress(
                    currentItem = item.name,
                    currentIndex = index + 1,
                    totalItems = total,
                    phase = Phase.DOWNLOADING
                )
                _progress.value = progress1
                onProgress(progress1)

                val zipFile = File(context.cacheDir, "${item.name.replace(" ", "_")}.zip")
                downloadFile(item.url, zipFile) { downloaded, totalSize ->
                    val p = progress1.copy(bytesDownloaded = downloaded, totalBytes = totalSize)
                    _progress.value = p
                    onProgress(p)
                }

                // Extract phase
                val progress2 = DownloadProgress(
                    currentItem = item.name,
                    currentIndex = index + 1,
                    totalItems = total,
                    phase = Phase.EXTRACTING
                )
                _progress.value = progress2
                onProgress(progress2)

                extractZip(zipFile, File(item.extractTo))
                zipFile.delete()

                // Set execute permissions on binaries
                setExecutePermissions(File(item.extractTo))

            } catch (e: Exception) {
                val errorProgress = DownloadProgress(
                    currentItem = item.name,
                    currentIndex = index + 1,
                    totalItems = total,
                    phase = Phase.ERROR,
                    error = e.message
                )
                _progress.value = errorProgress
                onProgress(errorProgress)

                if (item.required) {
                    throw e
                }
            }
        }

        // Configure phase
        val configProgress = DownloadProgress(
            currentItem = "Configuring environment",
            currentIndex = total,
            totalItems = total,
            phase = Phase.CONFIGURING
        )
        _progress.value = configProgress
        onProgress(configProgress)

        configureEnvironment()

        val completeProgress = DownloadProgress(
            currentItem = "Complete",
            currentIndex = total,
            totalItems = total,
            phase = Phase.COMPLETE
        )
        _progress.value = completeProgress
        onProgress(completeProgress)
    }

    private suspend fun downloadFile(
        urlString: String,
        outputFile: File,
        onProgress: (Long, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true

        try {
            connection.connect()
            val totalSize = connection.contentLengthLong
            var downloaded = 0L

            connection.inputStream.buffered().use { input ->
                FileOutputStream(outputFile).buffered().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        onProgress(downloaded, totalSize)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun extractZip(zipFile: File, outputDir: File) = withContext(Dispatchers.IO) {
        outputDir.mkdirs()
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(outputDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).buffered().use { output ->
                        zis.copyTo(output)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun setExecutePermissions(dir: File) {
        dir.walkTopDown().forEach { file ->
            if (file.isFile && (file.name.endsWith(".sh") ||
                        file.parentFile?.name == "bin" ||
                        file.name == "gradlew" ||
                        file.name == "gradle" ||
                        file.name == "java" ||
                        file.name == "javac" ||
                        file.name == "python3" ||
                        file.name == "node" ||
                        file.name == "flutter" ||
                        file.name == "dart" ||
                        file.name == "npm" ||
                        file.name == "npx")
            ) {
                file.setExecutable(true, false)
            }
        }
    }

    private fun configureEnvironment() {
        // Create local.properties equivalent
        val localProps = File("$filesDir/local.properties")
        localProps.writeText(buildString {
            appendLine("sdk.dir=$filesDir/sdk-tools")
            appendLine("ndk.dir=$filesDir/ndk")
            appendLine("java.home=$filesDir/jdk17")
        })
    }

    fun getJavaHome(version: Int = 17): String {
        return when (version) {
            21 -> "$filesDir/jdk21"
            else -> "$filesDir/jdk17"
        }
    }

    fun getSdkPath(): String = "$filesDir/sdk-tools"

    fun getNdkPath(): String = "$filesDir/ndk"

    fun getGradlePath(version: String = "8.14.4"): String = "$filesDir/gradle-$version"

    fun getFlutterPath(): String = "$filesDir/flutter"

    fun getNodePath(): String = "$filesDir/nodejs"

    fun getPythonPath(): String = "$filesDir/python"
}
