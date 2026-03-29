#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <android/log.h>
#include <cerrno>

#define LOG_TAG "BuildEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Forward declaration
extern int execute_shell_command(const char* command, char* output, int output_size, const char* working_dir, const char* env_vars);

static int ensure_gradlew_executable(const char* project_path) {
    char gradlew_path[1024];
    snprintf(gradlew_path, sizeof(gradlew_path), "%s/gradlew", project_path);

    struct stat st;
    if (stat(gradlew_path, &st) == 0) {
        // Try chmod first
        if (chmod(gradlew_path, 0755) == 0) {
            LOGI("chmod 755 applied to %s", gradlew_path);
            return 0;
        }
        LOGI("chmod failed, will use sh fallback for %s", gradlew_path);
    }
    return 1; // Use sh fallback
}

int start_build(const char* project_path, const char* java_home, const char* sdk_path, const char* build_type) {
    if (!project_path || !java_home || !build_type) {
        LOGE("Invalid build parameters");
        return -1;
    }

    LOGI("Build started: project=%s, jdk=%s, type=%s", project_path, java_home, build_type);

    // Determine the Gradle task based on build type
    const char* gradle_task;
    if (strcmp(build_type, "debug") == 0) {
        gradle_task = "assembleDebug";
    } else if (strcmp(build_type, "release") == 0) {
        gradle_task = "assembleRelease";
    } else if (strcmp(build_type, "bundle") == 0) {
        gradle_task = "bundleRelease";
    } else if (strcmp(build_type, "clean") == 0) {
        gradle_task = "clean";
    } else {
        gradle_task = build_type;
    }

    // Build environment variables string
    char env_vars[4096];
    snprintf(env_vars, sizeof(env_vars),
             "JAVA_HOME=%s\n"
             "ANDROID_HOME=%s\n"
             "ANDROID_SDK_ROOT=%s\n"
             "PATH=%s/bin:%s/platform-tools:%s/build-tools/34.0.0:$PATH\n"
             "GRADLE_USER_HOME=%s/.gradle",
             java_home, sdk_path, sdk_path,
             java_home, sdk_path, sdk_path,
             project_path);

    // Try chmod first, fallback to sh
    int use_sh = ensure_gradlew_executable(project_path);

    char command[2048];
    if (use_sh) {
        // Bypass execute restrictions by using sh
        snprintf(command, sizeof(command), "sh gradlew %s --no-daemon --stacktrace", gradle_task);
    } else {
        snprintf(command, sizeof(command), "./gradlew %s --no-daemon --stacktrace", gradle_task);
    }

    char output[65536];
    memset(output, 0, sizeof(output));

    int result = execute_shell_command(command, output, sizeof(output) - 1, project_path, env_vars);

    LOGI("Build result: %d", result);
    if (result != 0) {
        LOGE("Build failed with output:\n%s", output);
    } else {
        LOGI("Build successful");
    }

    return result;
}
