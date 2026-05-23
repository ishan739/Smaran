package com.example.smaran

object AudioConverter {
    /** Convert 16-bit PCM shorts (from AudioRecord) to float32 in [-1, 1] for Whisper. */
    fun shortsToFloat(input: ShortArray): FloatArray =
        FloatArray(input.size) { i -> input[i] / 32768.0f }
}
