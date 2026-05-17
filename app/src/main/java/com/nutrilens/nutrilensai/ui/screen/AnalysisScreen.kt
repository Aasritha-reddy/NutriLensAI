package com.nutrilens.nutrilensai.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutrilens.nutrilensai.ui.theme.*
import com.nutrilens.nutrilensai.viewmodel.AnalysisViewModel
import com.nutrilens.nutrilensai.viewmodel.OcrState
import com.nutrilens.nutrilensai.viewmodel.UiState
import java.io.File

// ─── Main Screen ──────────────────────────────────────────────────────────────

@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel = viewModel()) {
    val uiState  by viewModel.uiState.collectAsState()
    val ocrState by viewModel.ocrState.collectAsState()
    var ingredientsText by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(ocrState) {
        if (ocrState is OcrState.Success) ingredientsText = (ocrState as OcrState.Success).rawText
    }

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photoUri?.let { viewModel.runOcr(it) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { val u = createPhotoUri(context); photoUri = u; cameraLauncher.launch(u) }
    }
    val onScanClick: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val u = createPhotoUri(context); photoUri = u; cameraLauncher.launch(u)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val isBusy = uiState is UiState.Analyzing || uiState is UiState.Streaming

    Scaffold(topBar = { NutriTopBar() }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            when (val state = uiState) {
                is UiState.ModelNotFound ->
                    ModelNotFoundCard(
                        modelPath = "/sdcard/Android/data/com.nutrilens.nutrilensai/files/gemma-4-E2B-it.litertlm",
                        onRetry = viewModel::checkAndLoadModel,
                        modifier = Modifier.padding(20.dp)
                    )

                is UiState.ModelLoading -> ModelLoadingCard()

                else -> Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    SectionLabel(text = "Scan Label", icon = Icons.Rounded.CameraAlt)
                    ScanCard(
                        ocrState = ocrState,
                        enabled  = !isBusy,
                        onScan   = onScanClick,
                        onClear  = viewModel::clearOcr
                    )

                    SectionLabel(text = "Ingredient List", icon = Icons.AutoMirrored.Rounded.ListAlt)
                    IngredientCard(
                        value     = ingredientsText,
                        onChange  = { ingredientsText = it },
                        onAnalyze = { viewModel.analyze(ingredientsText) },
                        enabled   = !isBusy
                    )

                    when (state) {
                        is UiState.Analyzing -> AnalyzingCard()
                        is UiState.Streaming -> StreamingCard(text = state.text)
                        is UiState.Result -> {
                            SectionLabel(text = "Verdict", icon = Icons.Rounded.HealthAndSafety)
                            VerdictCard(
                                verdict     = state.result.verdict,
                                explanation = state.result.explanation,
                                onReset     = viewModel::reset
                            )
                        }
                        is UiState.Error -> NutriErrorCard(state.message, viewModel::reset)
                        else -> {}
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun createPhotoUri(context: android.content.Context): Uri {
    val file = File(context.externalCacheDir ?: context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NutriTopBar() {
    TopAppBar(
        modifier = Modifier.background(Brush.horizontalGradient(listOf(NutriGreen, NutriGreenDark))),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("N", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("NutriLens AI", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = (-0.3).sp)
                    Text("Smart Ingredient Checker", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

// ─── Section Label ───────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = NutriGreen, modifier = Modifier.size(16.dp))
        Text(
            text = text.uppercase(),
            color = NutriGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp
        )
    }
}

// ─── Scan Card ───────────────────────────────────────────────────────────────

@Composable
private fun ScanCard(ocrState: OcrState, enabled: Boolean, onScan: () -> Unit, onClear: () -> Unit) {
    NutriCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Tap-to-scan area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (ocrState is OcrState.Success) 80.dp else 150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NutriGreenLight)
                    .clickable(enabled = enabled && ocrState !is OcrState.Processing, onClick = onScan),
                contentAlignment = Alignment.Center
            ) {
                when (ocrState) {
                    is OcrState.Processing -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = NutriGreen)
                        Text("Extracting text…", color = NutriGreen, fontWeight = FontWeight.SemiBold)
                    }
                    is OcrState.Success -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = NutriGreen, modifier = Modifier.size(20.dp))
                        Text("Scan complete — tap to rescan", color = NutriGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CameraAlt, null, tint = NutriGreen, modifier = Modifier.size(36.dp))
                        }
                        Text("Tap to Scan Ingredient Label", color = NutriGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("AI will extract the text automatically", color = NutriTextSecondary, fontSize = 12.sp)
                    }
                }
            }

            // OCR success log
            if (ocrState is OcrState.Success) {
                OcrLogBox(text = ocrState.rawText, onClear = onClear)
            }

            // OCR error
            if (ocrState is OcrState.Error) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NutriAvoidLight)
                        .padding(12.dp)
                ) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = NutriAvoid, modifier = Modifier.size(18.dp))
                    Text(ocrState.message, color = NutriAvoidDark, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun OcrLogBox(text: String, onClear: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(NutriGreenLight)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.AutoMirrored.Rounded.TextSnippet, null, tint = NutriGreen, modifier = Modifier.size(16.dp))
                Text("Extracted Text", color = NutriGreenDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text("Clear", color = NutriGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Text(
                text = text.ifEmpty { "(no text detected)" },
                modifier = Modifier
                    .padding(14.dp)
                    .heightIn(max = 160.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = NutriTextPrimary
            )
        }
    }
}

// ─── Ingredient Card ─────────────────────────────────────────────────────────

@Composable
private fun IngredientCard(value: String, onChange: (String) -> Unit, onAnalyze: () -> Unit, enabled: Boolean) {
    NutriCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 130.dp),
                placeholder = {
                    Text(
                        "Paste or type the ingredients list here…\n\ne.g. Sugar, Modified Starch, Sodium Chloride, Peanut Oil, Artificial Flavors",
                        color = NutriTextHint,
                        fontSize = 13.sp
                    )
                },
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                maxLines = 12,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = NutriGreen,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedLabelColor    = NutriGreen,
                    cursorColor          = NutriGreen,
                    disabledBorderColor  = Color(0xFFF3F4F6),
                    disabledTextColor    = NutriTextSecondary
                )
            )
            GradientButton(
                text    = if (enabled) "Analyze Ingredients" else "Analyzing…",
                icon    = Icons.Rounded.Science,
                onClick = onAnalyze,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─── Gradient Button ─────────────────────────────────────────────────────────

@Composable
private fun GradientButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val gradient = if (enabled)
        Brush.horizontalGradient(listOf(NutriGreen, NutriGreenAccent))
    else
        Brush.horizontalGradient(listOf(NutriTextHint, NutriTextHint))

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(gradient)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(
                text = text,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ─── Analyzing Card ──────────────────────────────────────────────────────────

@Composable
private fun AnalyzingCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "analyzing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )
    NutriCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(scale)
                    .background(NutriGreenLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Science, null, tint = NutriGreen, modifier = Modifier.size(36.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Analyzing Ingredients", style = MaterialTheme.typography.titleMedium, color = NutriTextPrimary)
                Text("Our AI is reading your ingredients…", style = MaterialTheme.typography.bodySmall, color = NutriTextSecondary)
            }
        }
    }
}

// ─── Streaming Card ──────────────────────────────────────────────────────────

@Composable
private fun StreamingCard(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "alpha"
    )
    NutriCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(9.dp).background(NutriGreen.copy(alpha = dotAlpha), CircleShape))
                Text("Generating response…", color = NutriGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            HorizontalDivider(color = NutriDivider)
            Text(
                text = text,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = NutriTextPrimary
            )
        }
    }
}

// ─── Verdict Card ────────────────────────────────────────────────────────────

private data class VerdictConfig(
    val gradient: List<Color>,
    val icon: ImageVector,
    val label: String,
    val sublabel: String,
    val bodyTitle: String
)

@Composable
private fun VerdictCard(verdict: String, explanation: String, onReset: () -> Unit) {
    val config = when (verdict) {
        "SAFE"  -> VerdictConfig(listOf(NutriSafe, NutriSafeAccent),       Icons.Rounded.CheckCircle,     "Safe to Consume",    "This product suits your health profile",      "Why is this safe for you?")
        "AVOID" -> VerdictConfig(listOf(NutriAvoid, NutriAvoidAccent),     Icons.Rounded.Cancel,          "Avoid This Product", "Not recommended based on your health report", "Why should you avoid this?")
        else    -> VerdictConfig(listOf(NutriCaution, NutriCautionAccent), Icons.Rounded.WarningAmber,    "Use With Caution",   "Moderate consumption may be okay",            "What should you watch out for?")
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors    = CardDefaults.cardColors(containerColor = NutriSurface)
    ) {
        // Gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(config.gradient),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .background(Color.White.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(config.icon, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(config.label,    color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = (-0.2).sp)
                    Text(config.sublabel, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }
            }
        }

        // Body
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(config.bodyTitle, style = MaterialTheme.typography.titleSmall, color = NutriTextPrimary)
            Text(explanation, style = MaterialTheme.typography.bodyMedium, color = NutriTextSecondary, lineHeight = 22.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp),
                border = BorderStroke(1.5.dp, NutriGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NutriGreen)
            ) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Check Another Product", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Model Loading Card ──────────────────────────────────────────────────────

@Composable
private fun ModelLoadingCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = NutriGreen, strokeWidth = 4.dp, modifier = Modifier.size(56.dp))
            Text("Loading AI Model", style = MaterialTheme.typography.titleMedium, color = NutriTextPrimary)
            Text("This may take up to 15 seconds…", style = MaterialTheme.typography.bodySmall, color = NutriTextSecondary)
        }
    }
}

// ─── Model Not Found Card ────────────────────────────────────────────────────

@Composable
private fun ModelNotFoundCard(modelPath: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    NutriCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Icon header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(NutriGreenLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SmartToy, null, tint = NutriGreen, modifier = Modifier.size(36.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Gemma Model Required", style = MaterialTheme.typography.titleLarge, color = NutriTextPrimary)
                Text("Download the model and place it on your device to get started.", style = MaterialTheme.typography.bodyMedium, color = NutriTextSecondary)
            }
            // Path box
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Model path:", style = MaterialTheme.typography.labelSmall, color = NutriTextSecondary)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NutriBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = modelPath,
                        modifier = Modifier.padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NutriTextPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
            // Steps
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SetupStep(number = "1", text = "Visit huggingface.co/litert-community")
                SetupStep(number = "2", text = "Download gemma-4-E2B-it-litert-lm")
                SetupStep(number = "3", text = "Copy to the path shown above")
                SetupStep(number = "4", text = "Tap Retry below")
            }
            GradientButton(
                text    = "Retry",
                icon    = Icons.Rounded.Refresh,
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SetupStep(number: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(NutriGreenLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = NutriGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = NutriTextSecondary)
    }
}

// ─── Error Card ──────────────────────────────────────────────────────────────

@Composable
private fun NutriErrorCard(message: String, onDismiss: () -> Unit) {
    NutriCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(NutriAvoidLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ErrorOutline, null, tint = NutriAvoid, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Something went wrong", style = MaterialTheme.typography.titleSmall, color = NutriAvoidDark)
                Text(message, style = MaterialTheme.typography.bodySmall, color = NutriTextSecondary)
                TextButton(onClick = onDismiss, contentPadding = PaddingValues(0.dp)) {
                    Text("Dismiss", color = NutriAvoid, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ─── Shared Card Shell ───────────────────────────────────────────────────────

@Composable
private fun NutriCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 3.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors    = CardDefaults.cardColors(containerColor = NutriSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}
