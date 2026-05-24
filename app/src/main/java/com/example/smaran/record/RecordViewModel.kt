package com.example.smaran.record

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smaran.NetworkMonitor
import com.example.smaran.NetworkStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─── UI State ─────────────────────────────────────────────────────────────────

sealed class RecordUiState {
    object Idle         : RecordUiState()
    object Recording    : RecordUiState()
    object Transcribing : RecordUiState()
    data class Error(val message: String) : RecordUiState()
}

// ─── Navigation Events ────────────────────────────────────────────────────────

sealed class RecordEvent {
    data class NavigateToReview(
        val text: String,
        val durationSeconds: Int
    ) : RecordEvent()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<RecordUiState>(RecordUiState.Idle)
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RecordEvent>()
    val events: SharedFlow<RecordEvent> = _events.asSharedFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val networkMonitor = NetworkMonitor(application)
    val networkStatus: StateFlow<NetworkStatus> = networkMonitor.status

    // SpeechRecognizer must be created and used on the main thread
    private var recognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var startTimeMs = 0L

    // Text accumulated across multiple auto-restart sessions
    private val accumulatedText = StringBuilder()

    // True only when the user explicitly tapped Stop — not auto-silence
    @Volatile private var stoppingByUser = false

    // Last partial from the current session — fallback if final result is blank
    private var lastKnownPartial = ""

    // ── Start ─────────────────────────────────────────────────────────────────

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        if (_uiState.value is RecordUiState.Recording) return

        if (!SpeechRecognizer.isRecognitionAvailable(getApplication())) {
            _uiState.value = RecordUiState.Error(
                "Speech recognition not available. Please install the Google app."
            )
            return
        }

        if (networkMonitor.status.value == NetworkStatus.Unavailable) {
            _uiState.value = RecordUiState.Error(
                "No internet. Please turn on Wi-Fi or mobile data and try again."
            )
            return
        }

        startTimeMs = System.currentTimeMillis()
        stoppingByUser = false
        accumulatedText.clear()
        lastKnownPartial = ""
        _partialText.value = ""
        _uiState.value = RecordUiState.Recording

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }

        val sr = SpeechRecognizer.createSpeechRecognizer(getApplication())
        recognizer = sr
        sr.setRecognitionListener(listener)
        sr.startListening(recognizerIntent)
    }

    // ── Stop ──────────────────────────────────────────────────────────────────

    fun stopRecording() {
        if (_uiState.value !is RecordUiState.Recording) return
        stoppingByUser = true
        recognizer?.stopListening()
        _uiState.value = RecordUiState.Transcribing
    }

    // ── Recognition listener ──────────────────────────────────────────────────

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return

            lastKnownPartial = partial
            _partialText.value = buildString {
                if (accumulatedText.isNotEmpty()) append(accumulatedText).append(" ")
                append(partial)
            }
        }

        override fun onResults(results: Bundle?) {
            val segment = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.trim() ?: ""

            // If final result is blank, fall back to the last partial we saw
            val effective = if (segment.isNotBlank()) segment else lastKnownPartial
            lastKnownPartial = ""

            if (stoppingByUser) {
                // User pressed stop — append and finalise
                if (effective.isNotBlank()) {
                    if (accumulatedText.isNotEmpty()) accumulatedText.append(" ")
                    accumulatedText.append(effective)
                }
                finalize()
            } else {
                // Auto-silence — save segment and restart session
                if (effective.isNotBlank()) {
                    if (accumulatedText.isNotEmpty()) accumulatedText.append(" ")
                    accumulatedText.append(effective)
                    _partialText.value = accumulatedText.toString()
                }
                recognizerIntent?.let { recognizer?.startListening(it) }
            }
        }

        override fun onError(error: Int) {
            // Auto-silence with no speech — just restart silently
            if (!stoppingByUser && isRestartableError(error)) {
                lastKnownPartial = ""
                recognizerIntent?.let { recognizer?.startListening(it) }
                return
            }

            // User pressed stop but STT failed to return a final result —
            // salvage accumulated sentences + any partial we still have
            if (stoppingByUser && isRestartableError(error)) {
                if (lastKnownPartial.isNotBlank()) {
                    if (accumulatedText.isNotEmpty()) accumulatedText.append(" ")
                    accumulatedText.append(lastKnownPartial)
                    lastKnownPartial = ""
                }
                finalize()
                return
            }

            // Fatal error
            destroyRecognizer()
            _partialText.value = ""
            lastKnownPartial = ""
            stoppingByUser = false
            _uiState.value = RecordUiState.Error(sttErrorMessage(error))
        }
    }

    // ── Finalize ──────────────────────────────────────────────────────────────

    private fun finalize() {
        val finalText    = accumulatedText.toString().trim()
        val durationSecs = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()

        destroyRecognizer()
        _partialText.value = ""
        lastKnownPartial = ""
        stoppingByUser = false

        if (finalText.isBlank()) {
            _uiState.value = RecordUiState.Error("Nothing was heard. Please try again.")
        } else {
            viewModelScope.launch {
                _events.emit(RecordEvent.NavigateToReview(finalText, durationSecs))
            }
            _uiState.value = RecordUiState.Idle
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun destroyRecognizer() {
        recognizer?.destroy()
        recognizer = null
        recognizerIntent = null
    }

    fun clearError() {
        _uiState.value = RecordUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        destroyRecognizer()
        networkMonitor.unregister()
    }

    // Errors that mean "nothing heard right now" — safe to restart silently
    private fun isRestartableError(error: Int) =
        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
        error == SpeechRecognizer.ERROR_NO_MATCH

    private fun sttErrorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_NETWORK         -> "Network error. Check your connection and try again."
        SpeechRecognizer.ERROR_AUDIO           -> "Microphone error. Please try again."
        SpeechRecognizer.ERROR_SERVER          -> "Server error. Please try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT  -> "No speech detected. Tap to try again."
        SpeechRecognizer.ERROR_NO_MATCH        -> "Couldn't understand. Please speak more clearly."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy. Please try again."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
        else -> "Something went wrong. Please try again."
    }
}