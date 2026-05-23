#include <jni.h>
#include <string>
#include <android/log.h>
#include "whisper.h"

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

// WhisperContext.Companion.nativeLoadModel(modelPath: String): Long
JNIEXPORT jlong JNICALL
Java_com_example_smaran_WhisperContext_nativeLoadModel(
        JNIEnv *env, jclass /*clazz*/, jstring model_path) {

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("Loading model: %s", path);

    whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;

    whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(model_path, path);

    if (!ctx) {
        LOGE("whisper_init_from_file_with_params failed");
        return 0L;
    }

    LOGI("Model ready — %s", whisper_print_system_info());
    return reinterpret_cast<jlong>(ctx);
}

// WhisperContext.Companion.nativeTranscribe(ctxPtr, samples, language): String
JNIEXPORT jstring JNICALL
Java_com_example_smaran_WhisperContext_nativeTranscribe(
        JNIEnv *env, jclass /*clazz*/,
        jlong ctx_ptr, jfloatArray samples, jstring language) {

    if (!ctx_ptr) return env->NewStringUTF("");

    auto *ctx = reinterpret_cast<whisper_context *>(ctx_ptr);

    jsize    n  = env->GetArrayLength(samples);
    jfloat  *pcm = env->GetFloatArrayElements(samples, nullptr);
    const char *lang = env->GetStringUTFChars(language, nullptr);

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime   = false;
    params.print_progress   = false;
    params.print_timestamps = false;
    params.print_special    = false;
    params.translate        = false;
    params.language         = lang;   // "auto" lets Whisper detect; or "hi", "en", etc.
    params.n_threads        = 4;
    params.no_context       = true;
    params.single_segment   = false;

    int rc = whisper_full(ctx, params, pcm, n);

    env->ReleaseFloatArrayElements(samples, pcm, JNI_ABORT);
    env->ReleaseStringUTFChars(language, lang);

    if (rc != 0) {
        LOGE("whisper_full returned %d", rc);
        return env->NewStringUTF("");
    }

    std::string out;
    const int n_seg = whisper_full_n_segments(ctx);
    for (int i = 0; i < n_seg; ++i) {
        const char *text = whisper_full_get_segment_text(ctx, i);
        if (text) out += text;
    }

    return env->NewStringUTF(out.c_str());
}

// WhisperContext.Companion.nativeFree(ctxPtr)
JNIEXPORT void JNICALL
Java_com_example_smaran_WhisperContext_nativeFree(
        JNIEnv *env, jclass /*clazz*/, jlong ctx_ptr) {

    if (ctx_ptr) {
        whisper_free(reinterpret_cast<whisper_context *>(ctx_ptr));
        LOGI("Context freed");
    }
}

} // extern "C"
