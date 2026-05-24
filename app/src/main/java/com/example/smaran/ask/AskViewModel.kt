package com.example.smaran.ask

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smaran.TokenStorage
import com.example.smaran.api.AskRequest
import com.example.smaran.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─── Model ────────────────────────────────────────────────────────────────────

data class QaEntry(
    val id: String      = UUID.randomUUID().toString(),
    val question: String,
    val answer: String  = "",
    val timestamp: String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
    val isLoading: Boolean = true,
    val mood: String?   = null,
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class AskViewModel(application: Application) : AndroidViewModel(application) {

    private val _entries    = MutableStateFlow<List<QaEntry>>(emptyList())
    val entries: StateFlow<List<QaEntry>> = _entries.asStateFlow()

    private val _inputText  = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _playingId  = MutableStateFlow<String?>(null)
    val playingId: StateFlow<String?> = _playingId.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("en", "IN")
                ttsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { _playingId.value = null }
                    override fun onError(utteranceId: String?) { _playingId.value = null }
                })
            }
        }
    }

    fun onInputChange(text: String) { _inputText.value = text }

    fun sendQuestion(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val entry = QaEntry(question = trimmed)
        _entries.value = _entries.value + entry
        _inputText.value = ""

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.askMemory(
                    token   = TokenStorage.bearerToken,
                    request = AskRequest(query = trimmed)
                )
                val body = response.body()
                val answer = if (response.isSuccessful && body != null) {
                    body.answer.trim()
                } else {
                    "Server error (${response.code()}). Please try again."
                }
                val mood = body?.mood
                updateEntry(entry.id, answer, mood)
                speakAnswer(answer, entry.id, mood)
            } catch (e: Exception) {
                updateEntry(entry.id, "Couldn't reach Smaran. Check your connection.")
            }
        }
    }

    private fun updateEntry(id: String, answer: String, mood: String? = null) {
        _entries.value = _entries.value.map {
            if (it.id == id) it.copy(answer = answer, mood = mood, isLoading = false) else it
        }
    }

    // ── TTS — adjusts tone based on mood ─────────────────────────────────────

    fun speakAnswer(text: String, entryId: String, mood: String? = null) {
        if (!ttsReady) return
        tts?.stop()
        _playingId.value = entryId
        when (mood?.lowercase()) {
            "excited"         -> { tts?.setSpeechRate(1.1f); tts?.setPitch(1.1f) }
            "sad", "anxious"  -> { tts?.setSpeechRate(0.9f); tts?.setPitch(0.9f) }
            else              -> { tts?.setSpeechRate(1.0f); tts?.setPitch(1.0f) }
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, entryId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _playingId.value = null
    }

    // ── STT for voice input ───────────────────────────────────────────────────

    fun startVoiceInput() {
        if (_isListening.value) return
        if (!SpeechRecognizer.isRecognitionAvailable(getApplication())) return

        _isListening.value = true
        val sr = SpeechRecognizer.createSpeechRecognizer(getApplication())
        recognizer = sr

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(type: Int, p: Bundle?) {}

            override fun onPartialResults(partial: Bundle?) {
                partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.let { _inputText.value = it }
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim() ?: ""
                if (text.isNotBlank()) _inputText.value = text
                destroyRecognizer()
                _isListening.value = false
            }

            override fun onError(error: Int) {
                destroyRecognizer()
                _isListening.value = false
            }
        })

        sr.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
        )
    }

    fun stopVoiceInput() {
        recognizer?.stopListening()
        _isListening.value = false
    }

    private fun destroyRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }

    override fun onCleared() {
        super.onCleared()
        destroyRecognizer()
        tts?.stop()
        tts?.shutdown()
    }
}