package com.rvkedition.androidstudiomobile.backend.python

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*

/**
 * WebSocket client for communicating with the Python local server.
 * Handles real-time Gradle sync, build log streaming, and command execution.
 */
class WebSocketClient private constructor() {

    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .build()
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _messages = MutableSharedFlow<JsonObject>(replay = 0, extraBufferCapacity = 100)
    val messages: SharedFlow<JsonObject> = _messages

    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1)
    val connectionState: SharedFlow<ConnectionState> = _connectionState

    var isConnected = false
        private set

    companion object {
        private val instance = WebSocketClient()
        fun getInstance(): WebSocketClient = instance
    }

    enum class ConnectionState {
        CONNECTING, CONNECTED, DISCONNECTED, FAILED
    }

    fun connect(port: Int = PythonServerService.PORT) {
        scope.launch {
            _connectionState.emit(ConnectionState.CONNECTING)
        }

        val request = Request.Builder()
            .url("ws://127.0.0.1:$port")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                scope.launch { _connectionState.emit(ConnectionState.CONNECTED) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = gson.fromJson(text, JsonObject::class.java)
                    scope.launch { _messages.emit(json) }
                } catch (e: Exception) {
                    android.util.Log.e("WebSocketClient", "Failed to parse message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                scope.launch { _connectionState.emit(ConnectionState.DISCONNECTED) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                scope.launch { _connectionState.emit(ConnectionState.FAILED) }
                // Auto-reconnect after delay
                scope.launch {
                    delay(3000)
                    connect(port)
                }
            }
        })
    }

    fun sendSync(projectPath: String) {
        val msg = JsonObject().apply {
            addProperty("type", "sync")
            addProperty("project_path", projectPath)
        }
        webSocket?.send(gson.toJson(msg))
    }

    fun sendBuild(projectPath: String, buildType: String = "debug") {
        val msg = JsonObject().apply {
            addProperty("type", "build")
            addProperty("project_path", projectPath)
            addProperty("build_type", buildType)
        }
        webSocket?.send(gson.toJson(msg))
    }

    fun sendCommand(command: String, cwd: String = "/") {
        val msg = JsonObject().apply {
            addProperty("type", "exec")
            addProperty("command", command)
            addProperty("cwd", cwd)
        }
        webSocket?.send(gson.toJson(msg))
    }

    fun sendPing() {
        val msg = JsonObject().apply {
            addProperty("type", "ping")
        }
        webSocket?.send(gson.toJson(msg))
    }

    fun disconnect() {
        webSocket?.close(1000, "Client closing")
        isConnected = false
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
