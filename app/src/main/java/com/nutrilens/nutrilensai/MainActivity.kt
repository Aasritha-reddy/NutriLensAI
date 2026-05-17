package com.nutrilens.nutrilensai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nutrilens.nutrilensai.ui.screen.AnalysisScreen
import com.nutrilens.nutrilensai.ui.theme.NutriLensAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriLensAITheme {
                AnalysisScreen()
            }
        }
    }
}
