#include <jni.h>
#include <string>
#include <cstdlib>
#include <unistd.h>
#include <sys/wait.h>
#include <android/log.h>
#include <vector>
#include <mutex>
#include <thread>
#include <fcntl.h>
#include <cerrno>
#include <cstring>
#include <sys/stat.h>

#define LOG_TAG "NativeBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Forward declarations from other compilation units
extern int execute_shell_command(const char* command, char* output, int output_size, const char* working_dir, const char* env_vars);
extern pid_t spawn_process(const char* command, const char* working_dir, const char* env_vars, int* stdout_fd, int* stderr_fd);
extern int kill_process(pid_t pid);
extern int read_process_output(int fd, char* buffer, int buffer_size);
extern int start_build(const char* project_path, const char* java_home, const char* sdk_path, const char* build_type);

static std::mutex g_process_mutex;

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_rvkedition_androidstudiomobile_backend_cpp_NativeBridge_executeCommand(
        JNIEnv *env,
        jobject /* this */,
        jstring command,
        jstring workingDir,
        jstring envVars) {

    const char *cmd = env->GetStringUTFChars(command, nullptr);
    const char *dir = env->GetStringUTFChars(workingDir, nullptr);
    const char *envs = envVars ? env->GetStringUTFChars(envVars, nullptr) : nullptr;

    LOGI("Executing command: %s in dir: %s", cmd, dir);

    char output[65536];
    memset(output, 0, sizeof(output));

    int result = execute_shell_command(cmd, output, sizeof(output) - 1, dir, envs);

    env->ReleaseStringUTFChars(command, cmd);
    env->ReleaseStringUTFChars(workingDir, dir);
    if (envs) env->ReleaseStringUTFChars(envVars, envs);

    return env->NewStringUTF(output);
}

JNIEXPORT jlong JNICALL
Java_com_rvkedition_androidstudiomobile_backend_cpp_NativeBridge_spawnProcess(
        JNIEnv *env,
        jobject /* this */,
        jstring command,
        jstring workingDir,
        jstring envVars) {

    const char *cmd = env->GetStringUTFChars(command, nullptr);
    const char *dir = env->GetStringUTFChars(workingDir, nullptr);
    const char *envs = envVars ? env->GetStringUTFChars(envVars, nullptr) : nullptr;

    LOGI("Spawning process: %s", cmd);

    int stdout_fd = -1, stderr_fd = -1;
    std::lock_guard<std::mutex> lock(g_process_mutex);
    pid_t pid = spawn_process(cmd, dir, envs, &stdout_fd, &stderr_fd);

    env->ReleaseStringUTFChars(command, cmd);
    env->ReleaseStringUTFChars(workingDir, dir);
    if (envs) env->ReleaseStringUTFChars(envVars, envs);

    // Encode pid and file descriptors into a single long value
    // pid in lower 32 bits
    return (jlong) pid;
}

JNIEXPORT jint JNICALL
Java_com_rvkedition_androidstudiomobile_backend_cpp_NativeBridge_killProcess(
        JNIEnv *env,
        jobject /* this */,
        jlong pid) {

    std::lock_guard<std::mutex> lock(g_process_mutex);
    return kill_process((pid_t) pid);
}

JNIEXPORT jint JNICALL
Java_com_rvkedition_androidstudiomobile_backend_cpp_NativeBridge_setFilePermissions(
        JNIEnv *env,
        jobject /* this */,
        jstring filePath,
        jint mode) {

    const char *path = env->GetStringUTFChars(filePath, nullptr);
    int result = chmod(path, (mode_t) mode);
    if (result != 0) {
        LOGE("chmod failed for %s: %s", path, strerror(errno));
    }
    env->ReleaseStringUTFChars(filePath, path);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_rvkedition_androidstudiomobile_backend_cpp_NativeBridge_startBuild(
        JNIEnv *env,
        jobject /* this */,
        jstring projectPath,
        jstring javaHome,
        jstring sdkPath,
        jstring buildType) {

    const char *project = env->GetStringUTFChars(projectPath, nullptr);
    const char *jdk = env->GetStringUTFChars(javaHome, nullptr);
    const char *sdk = env->GetStringUTFChars(sdkPath, nullptr);
    const char *type = env->GetStringUTFChars(buildType, nullptr);

    LOGI("Starting build: project=%s, jdk=%s, type=%s", project, jdk, type);

    int result = start_build(project, jdk, sdk, type);

    char resultStr[256];
    snprintf(resultStr, sizeof(resultStr), "%d", result);

    env->ReleaseStringUTFChars(projectPath, project);
    env->ReleaseStringUTFChars(javaHome, jdk);
    env->ReleaseStringUTFChars(sdkPath, sdk);
    env->ReleaseStringUTFChars(buildType, type);

    return env->NewStringUTF(resultStr);
}

JNIEXPORT void JNICALL
Java_com_rvkedition_androidstudiomobile_backend_cpp_NativeBridge_setEnvironmentVariable(
        JNIEnv *env,
        jobject /* this */,
        jstring key,
        jstring value) {

    const char *k = env->GetStringUTFChars(key, nullptr);
    const char *v = env->GetStringUTFChars(value, nullptr);

    setenv(k, v, 1);
    LOGI("Set env: %s=%s", k, v);

    env->ReleaseStringUTFChars(key, k);
    env->ReleaseStringUTFChars(value, v);
}

JNIEXPORT jstring JNICALL
Java_com_rvkedition_androidstudiomobile_backend_cpp_NativeBridge_getSystemInfo(
        JNIEnv *env,
        jobject /* this */) {

    char info[4096];
    snprintf(info, sizeof(info),
             "{\n"
             "  \"arch\": \"%s\",\n"
             "  \"page_size\": %ld,\n"
             "  \"num_cpus\": %ld,\n"
             "  \"pid\": %d\n"
             "}",
#if defined(__aarch64__)
             "arm64-v8a",
#elif defined(__arm__)
             "armeabi-v7a",
#elif defined(__x86_64__)
             "x86_64",
#elif defined(__i386__)
             "x86",
#else
             "unknown",
#endif
             sysconf(_SC_PAGESIZE),
             sysconf(_SC_NPROCESSORS_ONLN),
             getpid()
    );

    return env->NewStringUTF(info);
}

} // extern "C"
