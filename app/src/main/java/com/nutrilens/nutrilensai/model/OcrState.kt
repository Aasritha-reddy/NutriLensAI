package com.nutrilens.nutrilensai.model

import androidx.compose.runtime.Immutable

@Immutable
sealed class OcrState {
    object Idle : OcrState()
    object Processing : OcrState()
    data class Success(val extractedText: String) : OcrState()
    data class Error(val errorMessage: String) : OcrState()
}
