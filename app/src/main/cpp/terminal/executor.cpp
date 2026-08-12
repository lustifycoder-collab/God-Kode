#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include <sys/wait.h>
#include <unistd.h>

#define LOG_TAG "ExecutorNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jint JNICALL
Java_com_dev_godkode_terminal_Executor_runBinary(
    JNIEnv* env,
    jobject /* this */,
    jstring binaryPath,
    jobjectArray argsArray,
    jobjectArray envArray,
    jstring linkerPath) {
    // Guard against null jstrings coming from Java.
    if (binaryPath == nullptr || linkerPath == nullptr) {
        LOGE("runBinary: binaryPath or linkerPath is null");
        return EINVAL;
    }
    if (argsArray == nullptr || envArray == nullptr) {
        LOGE("runBinary: argsArray or envArray is null");
        return EINVAL;
    }

    const char* binary = env->GetStringUTFChars(binaryPath, 0);
    const char* linker = env->GetStringUTFChars(linkerPath, 0);
    if (binary == nullptr || linker == nullptr) {
        if (binary) env->ReleaseStringUTFChars(binaryPath, binary);
        if (linker) env->ReleaseStringUTFChars(linkerPath, linker);
        LOGE("runBinary: GetStringUTFChars returned null (OOM)");
        return ENOMEM;
    }

    // RAII: free all strdup'd strings on every return path.
    std::vector<char*> args;
    std::vector<char*> envp;
    bool binaryStringsHeld = true; // binary/linker still have UTF chars held
    auto cleanup = [&]() {
        if (binaryStringsHeld) {
            env->ReleaseStringUTFChars(binaryPath, binary);
            env->ReleaseStringUTFChars(linkerPath, linker);
            binaryStringsHeld = false;
        }
        for (char* a : args) free(a);
        for (char* e : envp) free(e);
    };

    // Convert argsArray to a C-style array.
    jsize argsLen = env->GetArrayLength(argsArray);
    args.push_back(strdup(linker));   // argv[0]: linker path
    args.push_back(strdup(binary));   // argv[1]: binary path
    for (int i = 0; i < argsLen; i++) {
        auto arg = (jstring) env->GetObjectArrayElement(argsArray, i);
        if (arg == nullptr) {
            args.push_back(strdup(""));
            continue;
        }
        const char* argStr = env->GetStringUTFChars(arg, 0);
        if (argStr == nullptr) {
            env->DeleteLocalRef(arg);
            LOGE("runBinary: GetStringUTFChars returned null for arg %d (OOM)", i);
            cleanup();
            return ENOMEM;
        }
        args.push_back(strdup(argStr));
        env->ReleaseStringUTFChars(arg, argStr);
        env->DeleteLocalRef(arg);
    }
    args.push_back(nullptr); // null-terminate

    // Convert envArray to a C-style array.
    jsize envLen = env->GetArrayLength(envArray);
    for (int i = 0; i < envLen; i++) {
        auto envVar = (jstring) env->GetObjectArrayElement(envArray, i);
        if (envVar == nullptr) {
            envp.push_back(strdup(""));
            continue;
        }
        const char* envStr = env->GetStringUTFChars(envVar, 0);
        if (envStr == nullptr) {
            env->DeleteLocalRef(envVar);
            LOGE("runBinary: GetStringUTFChars returned null for env %d (OOM)", i);
            cleanup();
            return ENOMEM;
        }
        envp.push_back(strdup(envStr));
        env->ReleaseStringUTFChars(envVar, envStr);
        env->DeleteLocalRef(envVar);
    }
    envp.push_back(nullptr); // null-terminate

    // Create a pipe for capturing output.
    int pipefd[2];
    if (pipe(pipefd) < 0) {
        LOGE("Failed to create pipe: %s", strerror(errno));
        cleanup();
        return errno;
    }

    // Fork and execute.
    pid_t pid = fork();
    if (pid < 0) {
        // Fork failed — handle BEFORE touching the pipe as a parent,
        // otherwise read() below could block forever.
        LOGE("Fork failed with error: %s", strerror(errno));
        close(pipefd[0]);
        close(pipefd[1]);
        cleanup();
        return errno;
    }

    if (pid == 0) {
        // Child process.
        close(pipefd[0]);               // close read end
        dup2(pipefd[1], STDOUT_FILENO);// redirect stdout
        dup2(pipefd[1], STDERR_FILENO);// redirect stderr
        close(pipefd[1]);              // close write end after duplication

        execve(linker, args.data(), envp.data());
        LOGE("execve failed with error: %s", strerror(errno));
        _exit(errno); // exit with errno if execve fails
    }

    // Parent process.
    close(pipefd[1]); // close write end

    // Read the output from the pipe.
    char buffer[1024];
    ssize_t bytesRead;
    while ((bytesRead = read(pipefd[0], buffer, sizeof(buffer) - 1)) > 0) {
        buffer[bytesRead] = '\0';
        LOGI("%s", buffer);
    }
    close(pipefd[0]);

    cleanup();

    // Wait for the child process.
    int status;
    waitpid(pid, &status, 0);
    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    } else if (WIFSIGNALED(status)) {
        LOGE("Child process terminated by signal: %d", WTERMSIG(status));
        return 128 + WTERMSIG(status);
    }

    return -1; // unexpected exit condition
}
