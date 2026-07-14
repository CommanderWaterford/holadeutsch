package com.holadeutsch.app.ui.quiz

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.holadeutsch.app.data.repo.SessionOutcome
import com.holadeutsch.app.domain.AnswerResult
import com.holadeutsch.app.domain.Direction
import com.holadeutsch.app.domain.Question
import com.holadeutsch.app.domain.SentenceToken
import com.holadeutsch.app.ui.AppViewModelProvider
import com.holadeutsch.app.ui.components.ArticleText
import com.holadeutsch.app.ui.components.SpeakerButton
import com.holadeutsch.app.ui.components.articleColor
import com.holadeutsch.app.ui.components.correctColor
import com.holadeutsch.app.ui.components.partialColor
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onFinished: (SessionOutcome, Int, Int, List<Int>) -> Unit,
    onExit: () -> Unit,
    viewModel: QuizViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val ui by viewModel.ui.collectAsState()
    val haptic = LocalHapticFeedback.current
    SuccessSoundEffect(ui.successSoundEvent)

    LaunchedEffect(ui.outcome) {
        ui.outcome?.let { onFinished(it, ui.correctCount, ui.questions.size, ui.wrongWordIds) }
    }
    LaunchedEffect(ui.lastResult) {
        if (ui.lastResult != null && ui.hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (ui.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            ui.current?.let { question ->
                QuizContent(
                    question = question,
                    ui = ui,
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun QuizContent(
    question: Question,
    ui: QuizUiState,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LinearProgressIndicator(
            progress = {
                (ui.index + if (ui.answered) 1 else 0) / ui.questions.size.toFloat()
            },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Pregunta ${ui.index + 1} de ${ui.questions.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AnimatedContent(
            targetState = ui.index,
            transitionSpec = {
                (slideInHorizontally(tween(300)) { it / 2 } + fadeIn(tween(300))) togetherWith
                    fadeOut(tween(120))
            },
            label = "question"
        ) { index ->
            val q = ui.questions.getOrNull(index) ?: return@AnimatedContent
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PromptCard(q, viewModel)

                when (q) {
                    is Question.MultipleChoice ->
                        OptionsList(q.options, q.correctIndex, ui, viewModel::answerChoice)

                    is Question.ArticleChoice ->
                        OptionsList(q.options, q.correctIndex, ui, viewModel::answerChoice)

                    is Question.Typed -> {
                        OutlinedTextField(
                            value = ui.typedInput,
                            onValueChange = viewModel::onTypedChange,
                            enabled = !ui.answered,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Tu respuesta") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { viewModel.submitTyped() })
                        )
                        ui.hint?.let { hint ->
                            Text(
                                hint,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (!ui.answered) {
                            Button(
                                onClick = viewModel::submitTyped,
                                enabled = ui.typedInput.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Comprobar")
                            }
                            if (ui.hint == null) {
                                TextButton(
                                    onClick = viewModel::revealHint,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("💡 Dame una pista")
                                }
                            }
                        }
                    }

                    is Question.SentenceBuilder -> SentenceBuilderContent(
                        question = q,
                        ui = ui,
                        onAdd = viewModel::addSentenceToken,
                        onRemove = viewModel::removeSentenceToken,
                        onMove = viewModel::moveSentenceToken,
                        onSubmit = viewModel::submitSentence
                    )
                }
            }
        }

        AnimatedVisibility(visible = ui.answered) {
            ui.lastResult?.let { result ->
                FeedbackPanel(question, result, ui, viewModel)
            }
        }
    }
}

@Composable
private fun PromptCard(question: Question, viewModel: QuizViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (question) {
                is Question.MultipleChoice -> {
                    val deToEs = question.direction == Direction.DE_TO_ES
                    Text(
                        if (deToEs) "¿Qué significa…?" else "¿Cómo se dice en alemán…?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (deToEs) {
                            ArticleText(
                                question.word,
                                style = MaterialTheme.typography.headlineLarge
                            )
                            SpeakerButton(viewModel.tts.available) {
                                viewModel.speak(question.word.german)
                            }
                        } else {
                            Text(
                                question.word.spanish,
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                    }
                }

                is Question.ArticleChoice -> {
                    Text(
                        "¿der, die o das?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        question.word.germanBare,
                        style = MaterialTheme.typography.headlineLarge
                    )
                }

                is Question.Typed -> {
                    Text(
                        "Escríbelo en alemán",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        question.word.spanish,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    if (question.word.isNoun) {
                        Text(
                            "(el artículo es opcional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is Question.SentenceBuilder -> {
                    Text(
                        "Ordena la frase en alemán",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        question.word.exampleEs,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceBuilderContent(
    question: Question.SentenceBuilder,
    ui: QuizUiState,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onSubmit: () -> Unit
) {
    val tokensById = question.tokens.associateBy { it.id }
    val selected = ui.sentenceTokenIds.mapNotNull(tokensById::get)
    val selectedIds = ui.sentenceTokenIds.toSet()
    val available = question.shuffledTokens.filter { it.id !in selectedIds }

    Text(
        "Toca las palabras para añadirlas. Mantén pulsada una palabra colocada y arrástrala para reordenar.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            1.5.dp,
            if (ui.sentenceError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Tu frase", style = MaterialTheme.typography.labelLarge)
            if (selected.isEmpty()) {
                Text(
                    "Selecciona la primera palabra…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selected.forEachIndexed { index, token ->
                        SentenceTokenChip(
                            token = token,
                            index = index,
                            lastIndex = selected.lastIndex,
                            enabled = !ui.answered,
                            onRemove = { onRemove(token.id) },
                            onMove = onMove
                        )
                    }
                }
            }
        }
    }

    if (ui.sentenceError) {
        Text(
            "El orden todavía no es correcto. Ajusta las palabras e inténtalo otra vez.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }

    if (!ui.answered) {
        Text("Palabras disponibles", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            available.forEach { token ->
                AssistChip(
                    onClick = { onAdd(token.id) },
                    label = { Text(token.text) }
                )
            }
        }
        Button(
            onClick = onSubmit,
            enabled = selected.size == question.tokens.size,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Comprobar frase")
        }
    }
}

@Composable
private fun SentenceTokenChip(
    token: SentenceToken,
    index: Int,
    lastIndex: Int,
    enabled: Boolean,
    onRemove: () -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val thresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    AssistChip(
        onClick = { if (enabled) onRemove() },
        enabled = enabled,
        modifier = Modifier.pointerInput(token.id, index, enabled) {
            if (!enabled) return@pointerInput
            var accumulated = 0f
            detectDragGesturesAfterLongPress(
                onDragStart = { accumulated = 0f },
                onDrag = { change, dragAmount ->
                    change.consume()
                    accumulated += if (abs(dragAmount.x) >= abs(dragAmount.y)) {
                        dragAmount.x
                    } else {
                        dragAmount.y
                    }
                    when {
                        accumulated > thresholdPx && index < lastIndex -> {
                            onMove(index, index + 1)
                            accumulated = 0f
                        }
                        accumulated < -thresholdPx && index > 0 -> {
                            onMove(index, index - 1)
                            accumulated = 0f
                        }
                    }
                }
            )
        },
        label = { Text(token.text) }
    )
}

@Composable
private fun SuccessSoundEffect(event: Int) {
    val tone = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull()
    }
    DisposableEffect(tone) {
        onDispose { tone?.release() }
    }
    LaunchedEffect(event) {
        if (event > 0) tone?.startTone(ToneGenerator.TONE_PROP_ACK, 220)
    }
}

@Composable
private fun OptionsList(
    options: List<String>,
    correctIndex: Int,
    ui: QuizUiState,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { index, option ->
            val isCorrect = ui.answered && index == correctIndex
            val isWrongPick = ui.answered && index == ui.selectedIndex && index != correctIndex
            val container = when {
                isCorrect -> correctColor().copy(alpha = 0.16f)
                isWrongPick -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
            val border = when {
                isCorrect -> BorderStroke(2.dp, correctColor())
                isWrongPick -> BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            }
            OutlinedCard(
                onClick = { if (!ui.answered) onSelect(index) },
                colors = CardDefaults.outlinedCardColors(containerColor = container),
                border = border
            ) {
                Text(
                    option,
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (option in listOf("der", "die", "das")) articleColor(option)
                    else androidx.compose.ui.graphics.Color.Unspecified
                )
            }
        }
    }
}

@Composable
private fun FeedbackPanel(
    question: Question,
    result: AnswerResult,
    ui: QuizUiState,
    viewModel: QuizViewModel
) {
    val (title, color) = when (result) {
        AnswerResult.CORRECT -> {
            val reward = if (ui.lastAwardedXp > 0) "+${ui.lastAwardedXp} XP" else "repaso"
            "¡Correcto! $reward" to correctColor()
        }
        AnswerResult.PARTIAL -> {
            val reward = if (ui.lastAwardedXp > 0) "+${ui.lastAwardedXp} XP" else "repaso"
            "¡Casi! Pequeño error de escritura. $reward" to partialColor()
        }
        AnswerResult.WRONG -> "Incorrecto" to MaterialTheme.colorScheme.error
    }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.6f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
            if (result != AnswerResult.CORRECT) {
                Text("Respuesta: ${correctAnswerText(question)}", style = MaterialTheme.typography.titleMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        question.word.exampleDe,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        question.word.exampleEs,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SpeakerButton(viewModel.tts.available) { viewModel.speak(question.word.exampleDe) }
            }
            Button(onClick = viewModel::next, modifier = Modifier.fillMaxWidth()) {
                Text(if (ui.isLast) "Ver resultado" else "Continuar")
            }
        }
    }
}

private fun correctAnswerText(question: Question): String = when (question) {
    is Question.MultipleChoice -> question.options[question.correctIndex]
    is Question.ArticleChoice -> "${question.word.article} (${question.word.german})"
    is Question.Typed -> question.word.german
    is Question.SentenceBuilder -> question.word.exampleDe
}
