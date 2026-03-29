#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <unistd.h>
#include <sys/wait.h>
#include <signal.h>
#include <android/log.h>
#include <fcntl.h>
#include <cerrno>
#include <vector>
#include <mutex>
#include <map>

#define LOG_TAG "ProcessManager"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct ProcessInfo {
    pid_t pid;
    int stdout_fd;
    int stderr_fd;
    bool running;
};

static std::mutex g_map_mutex;
static std::map<pid_t, ProcessInfo> g_processes;

pid_t spawn_process(const char* command, const char* working_dir, const char* env_vars, int* stdout_fd, int* stderr_fd) {
    int stdout_pipe[2];
    int stderr_pipe[2];

    if (pipe(stdout_pipe) == -1 || pipe(stderr_pipe) == -1) {
        LOGE("Failed to create pipes: %s", strerror(errno));
        return -1;
    }

    pid_t pid = fork();
    if (pid == -1) {
        LOGE("Failed to fork: %s", strerror(errno));
        close(stdout_pipe[0]); close(stdout_pipe[1]);
        close(stderr_pipe[0]); close(stderr_pipe[1]);
        return -1;
    }

    if (pid == 0) {
        // Child process
        close(stdout_pipe[0]);
        close(stderr_pipe[0]);

        dup2(stdout_pipe[1], STDOUT_FILENO);
        dup2(stderr_pipe[1], STDERR_FILENO);
        close(stdout_pipe[1]);
        close(stderr_pipe[1]);

        if (working_dir && strlen(working_dir) > 0) {
            chdir(working_dir);
        }

        if (env_vars && strlen(env_vars) > 0) {
            char* env_copy = strdup(env_vars);
            char* saveptr = nullptr;
            char* token = strtok_r(env_copy, "\n", &saveptr);
            while (token != nullptr) {
                putenv(strdup(token));
                token = strtok_r(nullptr, "\n", &saveptr);
            }
            free(env_copy);
        }

        execl("/system/bin/sh", "sh", "-c", command, nullptr);
        execl("/bin/sh", "sh", "-c", command, nullptr);
        _exit(127);
    }

    // Parent process
    close(stdout_pipe[1]);
    close(stderr_pipe[1]);

    // Make file descriptors non-blocking
    fcntl(stdout_pipe[0], F_SETFL, O_NONBLOCK);
    fcntl(stderr_pipe[0], F_SETFL, O_NONBLOCK);

    *stdout_fd = stdout_pipe[0];
    *stderr_fd = stderr_pipe[0];

    ProcessInfo info;
    info.pid = pid;
    info.stdout_fd = stdout_pipe[0];
    info.stderr_fd = stderr_pipe[0];
    info.running = true;

    {
        std::lock_guard<std::mutex> lock(g_map_mutex);
        g_processes[pid] = info;
    }

    LOGI("Spawned process PID: %d", pid);
    return pid;
}

int kill_process(pid_t pid) {
    std::lock_guard<std::mutex> lock(g_map_mutex);

    auto it = g_processes.find(pid);
    if (it != g_processes.end()) {
        close(it->second.stdout_fd);
        close(it->second.stderr_fd);
        it->second.running = false;
        g_processes.erase(it);
    }

    int result = kill(pid, SIGTERM);
    if (result != 0) {
        LOGE("Failed to kill process %d: %s", pid, strerror(errno));
        // Try SIGKILL as fallback
        result = kill(pid, SIGKILL);
    }

    // Reap the child
    int status;
    waitpid(pid, &status, WNOHANG);

    return result;
}

int read_process_output(int fd, char* buffer, int buffer_size) {
    int bytes_read = read(fd, buffer, buffer_size - 1);
    if (bytes_read > 0) {
        buffer[bytes_read] = '\0';
    } else if (bytes_read == 0) {
        buffer[0] = '\0';
    } else {
        if (errno == EAGAIN || errno == EWOULDBLOCK) {
            buffer[0] = '\0';
            return 0;
        }
        return -1;
    }
    return bytes_read;
}
