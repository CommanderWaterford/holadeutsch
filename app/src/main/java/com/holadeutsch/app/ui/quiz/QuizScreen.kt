package com.holadeutsch.app.ui.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.holadeutsch.app.data.repo.SessionOutcome
import com.holadeutsch.app.domain.AnswerResult
import com.holadeutsch.app.domain.Direction
import com.holadeutsch.app.domain.Question
import com.holadeutsch.app.ui.AppViewModelProvider
import com.holadeutsch.app.ui.components.ArticleText
import com.holadeutsch.app.ui.components.SpeakerButton
import com.holadeutsch.app.ui.components.articleColor
import com.holadeutsch.app.ui.components.correctColor
import com.holadeutsch.app.ui.components.partialColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onFinished: (SessionOutcome, Int, Int, List<Int>) -> Unit,
    onExit: () -> Unit,
    viewModel: QuizViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val ui by viewModel.ui.collectAsState()
    val haptic = LocalHapticFeedback.current

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
                        if (!ui.answered) {
                            Button(
                                onClick = viewModel::submitTyped,
                                enabled = ui.typedInput.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Comprobar")
                            }
                        }
                    }
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
            }
        }
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
        AnswerResult.CORRECT -> "¡Correcto! +10 XP" to correctColor()
        AnswerResult.PARTIAL -> "¡Casi! Pequeño error de escritura. +5 XP" to partialColor()
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
}
