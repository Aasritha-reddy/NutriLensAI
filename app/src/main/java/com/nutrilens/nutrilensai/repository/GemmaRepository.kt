package com.nutrilens.nutrilensai.repository

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.nutrilens.nutrilensai.util.AssetReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

data class AnalysisResult(val verdict: String, val explanation: String)

class GemmaRepository(private val context: Context) {

    private var engine: Engine? = null

    fun modelFile(): File =
        File(context.getExternalFilesDir(null), "gemma-4-E2B-it.litertlm")

    fun isModelAvailable(): Boolean = modelFile().exists()

    suspend fun loadModel() = withContext(Dispatchers.IO) {
        val config = EngineConfig(
            modelPath = modelFile().absolutePath,
            backend = Backend.CPU()
        )
        val e = Engine(config)
        e.initialize()
        engine = e
    }

    fun analyzeStream(ingredients: String): Flow<String> = flow {
        val e = engine ?: error("Model not loaded")
        val healthReport = AssetReader.readHealthReport(context)

        val systemInstruction = """
You are a clinical nutritionist AI. Given a patient's health profile and a food product's ingredient list, determine if it is safe for the patient to consume.
Respond in this EXACT format with no extra text:
VERDICT: [SAFE / CAUTION / AVOID]
REASON: [2-3 sentences mentioning specific ingredients or nutrients of concern for this patient]
        """.trimIndent()

        val userMessage = """
PATIENT HEALTH PROFILE:
$healthReport

PRODUCT INGREDIENTS:
$ingredients
        """.trimIndent()

        val convConfig = ConversationConfig(
            systemInstruction = Contents.of(systemInstruction),
            samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8)
        )

        e.createConversation(convConfig).use { conversation ->
            conversation.sendMessageAsync(userMessage)
                .catch { throw it }
                .collect { chunk -> emit(chunk.toString()) }
        }
    }.flowOn(Dispatchers.IO)

    fun parseResponse(response: String): AnalysisResult {
        val verdictLine = response.lines().firstOrNull { it.startsWith("VERDICT:") }
        val reasonLine = response.lines().firstOrNull { it.startsWith("REASON:") }

        val verdict = verdictLine?.removePrefix("VERDICT:")?.trim()
            ?.uppercase()
            ?.let {
                when {
                    it.contains("SAFE") -> "SAFE"
                    it.contains("AVOID") -> "AVOID"
                    else -> "CAUTION"
                }
            } ?: "CAUTION"

        val explanation = reasonLine?.removePrefix("REASON:")?.trim()
            ?: response.trim().ifEmpty { "Unable to parse the model response." }

        return AnalysisResult(verdict, explanation)
    }

    fun close() {
        engine?.close()
        engine = null
    }
}
