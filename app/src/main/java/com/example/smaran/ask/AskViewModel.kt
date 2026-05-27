package com.example.smaran.ask

import android.app.Application
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smaran.TokenStorage
import com.example.smaran.api.AskRequest
import com.example.smaran.BuildConfig
import com.example.smaran.api.RetrofitClient
import com.example.smaran.api.SilkRetrofitClient
import com.example.smaran.api.SilkTtsRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─── Model ────────────────────────────────────────────────────────────────────

data class QaEntry(
    val id: String             = UUID.randomUUID().toString(),
    val question: String,
    val answer: String         = "",
    val timestamp: String      = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
    val isLoading: Boolean     = true,
    val mood: String?          = null,
    val voiceDescription: String? = null,
    val speaker: String?       = null,
    val pitchShift: Int?       = null,
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class AskViewModel(application: Application) : AndroidViewModel(application) {

    private val _entries     = MutableStateFlow<List<QaEntry>>(emptyList())
    val entries: StateFlow<List<QaEntry>> = _entries.asStateFlow()

    private val _inputText   = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _playingId   = MutableStateFlow<String?>(null)
    val playingId: StateFlow<String?> = _playingId.asStateFlow()

    // Tracks which entry's audio is currently being fetched from Silk
    private val _loadingId   = MutableStateFlow<String?>(null)
    val loadingId: StateFlow<String?> = _loadingId.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentTtsJob: Job? = null

    // Cached WAV files — keyed by entry id, fetched from Silk only once per answer
    private val audioCache = mutableMapOf<String, File>()

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
                val body   = response.body()
                val answer = if (response.isSuccessful && body != null) {
                    body.answer.trim()
                } else {
                    "Server error (${response.code()}). Please try again."
                }
                val mood             = body?.mood
                val voiceDescription = body?.voiceDescription
                val speaker          = body?.speaker
                val pitchShift       = body?.pitchShift
                updateEntry(entry.id, answer, mood, voiceDescription, speaker, pitchShift)
                speakAnswer(answer, entry.id, mood, voiceDescription, speaker, pitchShift)
            } catch (e: Exception) {
                updateEntry(entry.id, "Couldn't reach Smaran. Check your connection.")
            }
        }
    }

    private fun updateEntry(
        id: String, answer: String,
        mood: String? = null, voiceDescription: String? = null,
        speaker: String? = null, pitchShift: Int? = null
    ) {
        _entries.value = _entries.value.map {
            if (it.id == id) it.copy(
                answer = answer, mood = mood,
                voiceDescription = voiceDescription, speaker = speaker,
                pitchShift = pitchShift, isLoading = false
            ) else it
        }
    }

    // ── Silk TTS ──────────────────────────────────────────────────────────────

    fun speakAnswer(
        text: String, entryId: String,
        mood: String? = null, voiceDescription: String? = null,
        speaker: String? = null, pitchShift: Int? = null
    ) {
        stopSpeaking()

        val cached = audioCache[entryId]?.takeIf { it.exists() }

        if (cached != null) {
            _playingId.value = entryId
            currentTtsJob = viewModelScope.launch {
                try { playWavFile(cached, entryId) }
                catch (e: Exception) { _playingId.value = null }
            }
        } else {
            _loadingId.value = entryId
            currentTtsJob = viewModelScope.launch {
                try {
                    val wavFile = fetchAndCache(text, entryId, mood, voiceDescription, speaker, pitchShift)
                    _loadingId.value = null
                    if (wavFile == null || !isActive) return@launch
                    _playingId.value = entryId
                    playWavFile(wavFile, entryId)
                } catch (e: CancellationException) {
                    _loadingId.value = null
                    throw e
                } catch (e: Exception) {
                    _loadingId.value = null
                }
            }
        }
    }

    private suspend fun fetchAndCache(
        text: String, entryId: String,
        mood: String?, voiceDescription: String?,
        speaker: String?, pitchShift: Int?
    ): File? = withContext(Dispatchers.IO) {
        try {
            val request = when {
                speaker != null -> SilkTtsRequest(
                    model     = "mulberry",
                    text      = text,
                    speaker   = speaker,
                    f0_up_key = pitchShift
                )
                voiceDescription != null -> SilkTtsRequest(
                    model       = "mulberry",
                    text        = text,
                    description = voiceDescription
                )
                else -> SilkTtsRequest(
                    model = "muga",
                    text  = "${moodToToneTag(mood)} $text"
                )
            }
            val response = SilkRetrofitClient.service.synthesize(
                token   = "Bearer ${BuildConfig.SILK_API_KEY}",
                request = request
            )
            if (!response.isSuccessful || response.body() == null) return@withContext null
            val bytes = response.body()!!.bytes()
            val file = File(getApplication<Application>().cacheDir, "tts_$entryId.wav")
            file.writeBytes(bytes)
            audioCache[entryId] = file
            file
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun playWavFile(file: File, entryId: String) {
        val mp = MediaPlayer()
        mediaPlayer = mp
        mp.setDataSource(file.absolutePath)
        mp.setOnCompletionListener {
            if (_playingId.value == entryId) _playingId.value = null
            it.release()
            if (mediaPlayer == it) mediaPlayer = null
        }
        mp.setOnErrorListener { _, _, _ ->
            if (_playingId.value == entryId) _playingId.value = null
            true
        }
        withContext(Dispatchers.IO) { mp.prepare() }
        mp.start()
    }

    fun stopSpeaking() {
        currentTtsJob?.cancel()
        currentTtsJob = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _playingId.value = null
        _loadingId.value = null
    }

    private fun moodToToneTag(mood: String?): String = when (mood?.lowercase()) {
        "happy"           -> "[happy]"
        "excited"         -> "[excited]"
        "sad"             -> "[sad]"
        "anxious", "fear" -> "[sad]"
        "angry"           -> "[angry]"
        "whisper"         -> "[whisper]"
        else              -> "[neutral]"
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
        currentTtsJob?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        audioCache.values.forEach { it.delete() }
        audioCache.clear()
    }
}