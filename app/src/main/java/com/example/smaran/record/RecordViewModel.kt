package com.example.smaran.record

import android.Manifest
import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smaran.AudioConverter
import com.example.smaran.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── UI State (StateFlow) ─────────────────────────────────────────────────────
// Represents what the screen looks like right now.

sealed class RecordUiState {
    object Idle        : RecordUiState()
    object Recording   : RecordUiState()
    object Transcribing: RecordUiState()
    data class Error(val message: String) : RecordUiState()
}

// ─── Navigation Events (SharedFlow) ──────────────────────────────────────────
// One-time events — fire once, don't survive recomposition.

sealed class RecordEvent {
    data class NavigateToReview(
        val text: String,
        val durationSeconds: Int
    ) : RecordEvent()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    // StateFlow — UI observes this for screen state
    private val _uiState = MutableStateFlow<RecordUiState>(RecordUiState.Idle)
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    // SharedFlow — one-time navigation events
    private val _events = MutableSharedFlow<RecordEvent>()
    val events: SharedFlow<RecordEvent> = _events.asSharedFlow()

    private var whisperContext: WhisperContext? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val recordedSamples = mutableListOf<Short>()
    private var startTimeMs = 0L
    private val sampleRate = 16000
    @Volatile private var keepRecording = false

    init {
        initWhisper()
    }

    private fun initWhisper() {
        viewModelScope.launch {
            whisperContext = WhisperContext.createFromAssets(getApplication())
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        if (_uiState.value is RecordUiState.Recording) return

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        recordedSamples.clear()
        startTimeMs = System.currentTimeMillis()
        keepRecording = true
        _uiState.value = RecordUiState.Recording

        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            val buffer = ShortArray(bufferSize)
            audioRecord?.startRecording()
            while (keepRecording) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: break
                if (read > 0) recordedSamples.addAll(buffer.take(read))
                else if (read < 0) break
            }
        }
    }

    fun stopRecording() {
        if (_uiState.value !is RecordUiState.Recording) return

        val durationSeconds = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()

        keepRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        _uiState.value = RecordUiState.Transcribing

        viewModelScope.launch {
            recordingJob?.join()

            try {
                val floatAudio = withContext(Dispatchers.Default) {
                    AudioConverter.shortsToFloat(recordedSamples.toShortArray())
                }

                val text = whisperContext?.transcribe(floatAudio, language = "auto")
                    ?: throw Exception("Whisper not initialised")

                // Emit navigation event via SharedFlow
                _events.emit(
                    RecordEvent.NavigateToReview(
                        text = text,
                        durationSeconds = durationSeconds
                    )
                )

                _uiState.value = RecordUiState.Idle

            } catch (e: Exception) {
                _uiState.value = RecordUiState.Error(e.message ?: "Transcription failed")
            }
        }
    }

    fun clearError() {
        _uiState.value = RecordUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        audioRecord?.release()
        whisperContext?.close()
    }
}