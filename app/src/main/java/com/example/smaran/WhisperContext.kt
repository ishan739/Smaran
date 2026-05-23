package com.example.smaran

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class WhisperContext private constructor(
    private val ptr: Long
) : AutoCloseable {

    private val closed = AtomicBoolean(false)
    private val transcribeMutex = Mutex()

    /**
     * Transcribe 16 kHz mono float32 PCM audio.
     *
     * audioData must be:
     * - mono
     * - 16 kHz
     * - FloatArray values in [-1.0, 1.0]
     *
     * language:
     * - "auto"
     * - "en"
     * - "hi"
     */
    suspend fun transcribe(
        audioData: FloatArray,
        language: String = "auto"
    ): String = transcribeMutex.withLock {
        check(!closed.get()) { "WhisperContext is already closed" }
        check(audioData.isNotEmpty()) { "audioData is empty" }

        withContext(Dispatchers.Default) {
            nativeTranscribe(ptr, audioData, language).trim()
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            nativeFree(ptr)
        }
    }

    companion object {
        private const val TAG = "WhisperContext"

        // If model is directly in app/src/main/assets/
        private const val MODEL_ASSET = "ggml-base-q5_1.bin"

        // Copied destination file name inside internal storage
        private const val MODEL_FILE = "ggml-base-q5_1.bin"

        init {
            System.loadLibrary("whisper_jni")
        }

        suspend fun createFromAssets(context: Context): WhisperContext? =
            withContext(Dispatchers.IO) {
                val dest = File(context.filesDir, MODEL_FILE)

                if (!dest.exists()) {
                    Log.i(TAG, "Copying Whisper model from assets")

                    val tmp = File(context.filesDir, "$MODEL_FILE.tmp")
                    if (tmp.exists()) tmp.delete()

                    context.assets.open(MODEL_ASSET).use { src ->
                        tmp.outputStream().use { dst ->
                            src.copyTo(dst)
                        }
                    }

                    if (!tmp.renameTo(dest)) {
                        Log.e(TAG, "Failed to move temp model file into place")
                        tmp.delete()
                        return@withContext null
                    }
                }

                val nativePtr = nativeLoadModel(dest.absolutePath)

                if (nativePtr == 0L) {
                    Log.e(TAG, "nativeLoadModel returned 0. Check Logcat native logs.")
                    null
                } else {
                    WhisperContext(nativePtr)
                }
            }

        @JvmStatic
        private external fun nativeLoadModel(modelPath: String): Long

        @JvmStatic
        private external fun nativeTranscribe(
            ctxPtr: Long,
            samples: FloatArray,
            language: String
        ): String

        @JvmStatic
        private external fun nativeFree(ctxPtr: Long)
    }
}