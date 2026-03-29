#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <unistd.h>
#include <sys/wait.h>
#include <android/log.h>
#include <fcntl.h>
#include <cerrno>

#define LOG_TAG "ShellExecutor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

int execute_shell_command(const char* command, char* output, int output_size, const char* working_dir, const char* env_vars) {
    int pipe_fd[2];
    if (pipe(pipe_fd) == -1) {
        LOGE("Failed to create pipe: %s", strerror(errno));
        snprintf(output, output_size, "Error: Failed to create pipe: %s", strerror(errno));
        return -1;
    }

    pid_t pid = fork();
    if (pid == -1) {
        LOGE("Failed to fork: %s", strerror(errno));
        close(pipe_fd[0]);
        close(pipe_fd[1]);
        snprintf(output, output_size, "Error: Failed to fork: %s", strerror(errno));
        return -1;
    }

    if (pid == 0) {
        // Child process
        close(pipe_fd[0]); // Close read end

        // Redirect stdout and stderr to pipe
        dup2(pipe_fd[1], STDOUT_FILENO);
        dup2(pipe_fd[1], STDERR_FILENO);
        close(pipe_fd[1]);

        // Change working directory
        if (working_dir && strlen(working_dir) > 0) {
            if (chdir(working_dir) != 0) {
                fprintf(stderr, "Failed to chdir to %s: %s\n", working_dir, strerror(errno));
                _exit(1);
            }
        }

        // Set environment variables (format: KEY1=VALUE1\nKEY2=VALUE2)
        if (env_vars && strlen(env_vars) > 0) {
            char* env_copy = strdup(env_vars);
            char* saveptr = nullptr;
            char* token = strtok_r(env_copy, "\n", &saveptr);
            while (token != nullptr) {
                putenv(strdup(token)); // putenv takes ownership
                token = strtok_r(nullptr, "\n", &saveptr);
            }
            free(env_copy);
        }

        // Execute command via sh
        execl("/system/bin/sh", "sh", "-c", command, nullptr);
        // Fallback
        execl("/bin/sh", "sh", "-c", command, nullptr);
        fprintf(stderr, "Failed to exec: %s\n", strerror(errno));
        _exit(127);
    }

    // Parent process
    close(pipe_fd[1]); // Close write end

    int total_read = 0;
    int bytes_read;
    while ((bytes_read = read(pipe_fd[0], output + total_read, output_size - total_read - 1)) > 0) {
        total_read += bytes_read;
        if (total_read >= output_size - 1) break;
    }
    output[total_read] = '\0';
    close(pipe_fd[0]);

    int status;
    waitpid(pid, &status, 0);

    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    }
    return -1;
}
