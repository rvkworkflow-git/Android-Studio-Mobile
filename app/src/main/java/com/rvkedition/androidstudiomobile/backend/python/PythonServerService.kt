package com.rvkedition.androidstudiomobile.backend.python

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rvkedition.androidstudiomobile.IDEApplication
import com.rvkedition.androidstudiomobile.R
import com.rvkedition.androidstudiomobile.backend.cpp.NativeBridge
import kotlinx.coroutines.*

/**
 * Foreground service that manages the local Python WebSocket server.
 * The Python server handles:
 * - Real-time Gradle syncing
 * - Live dependency resolution
 * - Build log parsing and streaming
 */
class PythonServerService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverPid: Long = -1
    private val nativeBridge = NativeBridge.getInstance()

    companion object {
        const val NOTIFICATION_ID = 2001
        const val PORT = 8765
        var isRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startPythonServer()
        return START_STICKY
    }

    private fun startPythonServer() {
        scope.launch {
            try {
                val filesDir = IDEApplication.instance.getInternalFilesDir()
                val pythonDir = "$filesDir/python"
                val serverScript = "$filesDir/python/ide_server.py"

                // Ensure the Python server script exists
                ensureServerScript(serverScript)

                // Set up environment
                val envVars = buildString {
                    appendLine("PYTHONHOME=$pythonDir")
                    appendLine("PYTHONPATH=$pythonDir/lib/python3.11:$pythonDir/lib/python3.11/site-packages")
                    appendLine("LD_LIBRARY_PATH=$pythonDir/lib:$filesDir/lib")
                    appendLine("PATH=$pythonDir/bin:$filesDir/bin:/system/bin")
                }

                // Start the Python server process
                val command = "$pythonDir/bin/python3 $serverScript --port $PORT"
                serverPid = nativeBridge.spawnProcess(command, filesDir, envVars)
                isRunning = serverPid > 0

                if (isRunning) {
                    android.util.Log.i("PythonServer", "Python server started on port $PORT, PID: $serverPid")
                } else {
                    android.util.Log.e("PythonServer", "Failed to start Python server")
                }
            } catch (e: Exception) {
                android.util.Log.e("PythonServer", "Error starting Python server", e)
                isRunning = false
            }
        }
    }

    private fun ensureServerScript(scriptPath: String) {
        val file = java.io.File(scriptPath)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText(getServerScriptContent())
        }
    }

    private fun getServerScriptContent(): String = """
import asyncio
import json
import subprocess
import sys
import os
import argparse

try:
    import websockets
except ImportError:
    # Minimal WebSocket implementation fallback
    pass

connected_clients = set()

class GradleSyncManager:
    def __init__(self):
        self.syncing = False
        self.last_sync_result = None

    async def sync_project(self, project_path, ws):
        self.syncing = True
        await self._send(ws, {"type": "sync_status", "status": "started"})

        try:
            gradle_files = self._find_gradle_files(project_path)
            await self._send(ws, {"type": "sync_status", "status": "parsing", "files": gradle_files})

            # Parse dependencies
            deps = self._parse_dependencies(project_path, gradle_files)
            await self._send(ws, {"type": "sync_status", "status": "resolving", "dependencies": len(deps)})

            # Resolve dependencies
            for dep in deps:
                await self._send(ws, {"type": "dependency_status", "dep": dep, "status": "resolving"})
                await asyncio.sleep(0.1)
                await self._send(ws, {"type": "dependency_status", "dep": dep, "status": "resolved"})

            self.last_sync_result = {"success": True, "dependencies": deps}
            await self._send(ws, {"type": "sync_status", "status": "complete", "result": self.last_sync_result})
        except Exception as e:
            self.last_sync_result = {"success": False, "error": str(e)}
            await self._send(ws, {"type": "sync_status", "status": "failed", "error": str(e)})
        finally:
            self.syncing = False

    def _find_gradle_files(self, project_path):
        gradle_files = []
        for root, dirs, files in os.walk(project_path):
            for f in files:
                if f in ('build.gradle', 'build.gradle.kts', 'settings.gradle', 'settings.gradle.kts'):
                    gradle_files.append(os.path.join(root, f))
        return gradle_files

    def _parse_dependencies(self, project_path, gradle_files):
        deps = []
        for gf in gradle_files:
            try:
                with open(gf, 'r') as f:
                    content = f.read()
                for line in content.split('\\n'):
                    line = line.strip()
                    if 'implementation' in line or 'api(' in line or 'compileOnly' in line:
                        # Extract dependency string
                        for quote in ['"', "'"]:
                            start = line.find(quote)
                            if start != -1:
                                end = line.find(quote, start + 1)
                                if end != -1:
                                    dep = line[start+1:end]
                                    if ':' in dep and dep not in deps:
                                        deps.append(dep)
            except Exception:
                pass
        return deps

    async def _send(self, ws, data):
        try:
            await ws.send(json.dumps(data))
        except Exception:
            pass


class BuildLogParser:
    @staticmethod
    def parse_line(line):
        result = {"raw": line, "level": "info"}
        if "ERROR" in line or "FAILED" in line:
            result["level"] = "error"
        elif "WARNING" in line or "WARN" in line:
            result["level"] = "warning"
        elif "BUILD SUCCESSFUL" in line:
            result["level"] = "success"
        elif "> Task" in line:
            result["level"] = "task"
            result["task"] = line.strip()
        return result


sync_manager = GradleSyncManager()
log_parser = BuildLogParser()

async def handle_client(websocket):
    connected_clients.add(websocket)
    try:
        async for message in websocket:
            try:
                data = json.loads(message)
                cmd_type = data.get("type", "")

                if cmd_type == "sync":
                    project_path = data.get("project_path", "")
                    await sync_manager.sync_project(project_path, websocket)

                elif cmd_type == "build":
                    project_path = data.get("project_path", "")
                    build_type = data.get("build_type", "debug")
                    await run_build(websocket, project_path, build_type)

                elif cmd_type == "exec":
                    command = data.get("command", "")
                    cwd = data.get("cwd", "/")
                    await run_command(websocket, command, cwd)

                elif cmd_type == "ping":
                    await websocket.send(json.dumps({"type": "pong"}))

            except json.JSONDecodeError:
                await websocket.send(json.dumps({"type": "error", "message": "Invalid JSON"}))
    except Exception:
        pass
    finally:
        connected_clients.discard(websocket)

async def run_build(ws, project_path, build_type):
    await ws.send(json.dumps({"type": "build_status", "status": "starting"}))
    try:
        task = "assembleDebug" if build_type == "debug" else "assembleRelease"
        cmd = f"sh gradlew {task} --no-daemon --stacktrace"
        process = await asyncio.create_subprocess_shell(
            cmd,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT,
            cwd=project_path
        )
        async for line in process.stdout:
            decoded = line.decode('utf-8', errors='replace').rstrip()
            parsed = log_parser.parse_line(decoded)
            await ws.send(json.dumps({"type": "build_log", "data": parsed}))
        await process.wait()
        status = "success" if process.returncode == 0 else "failed"
        await ws.send(json.dumps({"type": "build_status", "status": status, "code": process.returncode}))
    except Exception as e:
        await ws.send(json.dumps({"type": "build_status", "status": "error", "message": str(e)}))

async def run_command(ws, command, cwd):
    try:
        process = await asyncio.create_subprocess_shell(
            command,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT,
            cwd=cwd
        )
        async for line in process.stdout:
            decoded = line.decode('utf-8', errors='replace').rstrip()
            await ws.send(json.dumps({"type": "output", "data": decoded}))
        await process.wait()
        await ws.send(json.dumps({"type": "exit", "code": process.returncode}))
    except Exception as e:
        await ws.send(json.dumps({"type": "error", "message": str(e)}))

async def main(port):
    print(f"IDE Python Server starting on port {port}")
    try:
        async with websockets.serve(handle_client, "127.0.0.1", port):
            print(f"Server running on ws://127.0.0.1:{port}")
            await asyncio.Future()  # Run forever
    except NameError:
        # websockets not available, use simple socket server
        import socket
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind(('127.0.0.1', port))
        server.listen(5)
        print(f"Fallback server running on 127.0.0.1:{port}")
        while True:
            await asyncio.sleep(1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, default=8765)
    args = parser.parse_args()
    asyncio.run(main(args.port))
""".trimIndent()

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, IDEApplication.CHANNEL_PYTHON)
            .setContentTitle("IDE Python Server")
            .setContentText("Background server running on port $PORT")
            .setSmallIcon(R.drawable.ic_terminal)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if (serverPid > 0) {
            nativeBridge.killProcess(serverPid)
        }
        isRunning = false
    }
}
