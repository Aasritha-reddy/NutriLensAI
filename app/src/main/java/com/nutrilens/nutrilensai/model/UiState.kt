package com.nutrilens.nutrilensai.model

import androidx.compose.runtime.Immutable

@Immutable
sealed class UiState {
    object ModelNotFound : UiState()
    object ModelLoading : UiState()
    object Ready : UiState()
    object Analyzing : UiState()
    data class Streaming(val partialResponse: String) : UiState()
    data class Result(val analysisResult: AnalysisResult) : UiState()
    data class Error(val errorMessage: String) : UiState()
}
