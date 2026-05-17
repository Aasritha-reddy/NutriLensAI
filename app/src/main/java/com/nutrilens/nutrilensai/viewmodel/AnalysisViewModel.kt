package com.nutrilens.nutrilensai.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutrilens.nutrilensai.repository.AnalysisResult
import com.nutrilens.nutrilensai.repository.GemmaRepository
import com.nutrilens.nutrilensai.util.OcrHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object ModelNotFound : UiState()
    object ModelLoading : UiState()
    object Ready : UiState()
    object Analyzing : UiState()
    data class Streaming(val text: String) : UiState()
    data class Result(val result: AnalysisResult) : UiState()
    data class Error(val message: String) : UiState()
}

sealed class OcrState {
    object Idle : OcrState()
    object Processing : OcrState()
    data class Success(val rawText: String) : OcrState()
    data class Error(val message: String) : OcrState()
}

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GemmaRepository(application)

    private val _uiState = MutableStateFlow<UiState>(UiState.ModelNotFound)
    val uiState: StateFlow<UiState> = _uiState

    private val _ocrState = MutableStateFlow<OcrState>(OcrState.Idle)
    val ocrState: StateFlow<OcrState> = _ocrState

    init {
        checkAndLoadModel()
    }

    fun checkAndLoadModel() {
        if (!repository.isModelAvailable()) {
            _uiState.value = UiState.ModelNotFound
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.ModelLoading
            try {
                repository.loadModel()
                _uiState.value = UiState.Ready
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load model: ${e.message}")
            }
        }
    }

    fun runOcr(uri: Uri) {
        viewModelScope.launch {
            _ocrState.value = OcrState.Processing
            try {
                val text = OcrHelper.extractText(uri, getApplication())
                _ocrState.value = OcrState.Success(text)
            } catch (e: Exception) {
                _ocrState.value = OcrState.Error("OCR failed: ${e.message}")
            }
        }
    }

    fun clearOcr() {
        _ocrState.value = OcrState.Idle
    }

    fun analyze(ingredients: String) {
        if (ingredients.isBlank()) return
        viewModelScope.launch {
            _uiState.value = UiState.Analyzing
            val accumulated = StringBuilder()
            try {
                repository.analyzeStream(ingredients).collect { chunk ->
                    accumulated.append(chunk)
                    _uiState.value = UiState.Streaming(accumulated.toString())
                }
                _uiState.value = UiState.Result(repository.parseResponse(accumulated.toString()))
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Analysis failed: ${e.message}")
            }
        }
    }

    fun reset() {
        _uiState.value = UiState.Ready
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}
