package com.example.smaran.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smaran.api.MemoryRepository
import com.example.smaran.api.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─── UI State ─────────────────────────────────────────────────────────────────

sealed class ReviewUiState {
    object Idle    : ReviewUiState()
    object Sending : ReviewUiState()
    object Sent    : ReviewUiState()
    data class Error(val message: String) : ReviewUiState()
}

// ─── Events ───────────────────────────────────────────────────────────────────

sealed class ReviewEvent {
    object NavigateBack : ReviewEvent()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class ReviewViewModel : ViewModel() {

    private val repository = MemoryRepository()

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Idle)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ReviewEvent>()
    val events: SharedFlow<ReviewEvent> = _events.asSharedFlow()

    fun sendMemory(text: String, durationSeconds: Int) {
        if (_uiState.value is ReviewUiState.Sending) return
        if (text.isBlank()) {
            _uiState.value = ReviewUiState.Error("Text is empty — nothing to send")
            return
        }

        _uiState.value = ReviewUiState.Sending

        viewModelScope.launch {
            when (val result = repository.saveMemory(text, durationSeconds)) {
                is Result.Success -> {
                    _uiState.value = ReviewUiState.Sent
                    delay(1200)
                    _events.emit(ReviewEvent.NavigateBack)
                }
                is Result.Error -> {
                    _uiState.value = ReviewUiState.Error(result.message)
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = ReviewUiState.Idle
    }
}