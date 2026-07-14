package com.holadeutsch.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.holadeutsch.app.data.model.Nivel
import com.holadeutsch.app.ui.AppViewModelProvider
import com.holadeutsch.app.ui.components.ArticleText
import com.holadeutsch.app.ui.components.DailyGoalRing
import com.holadeutsch.app.ui.components.SpeakerButton
import com.holadeutsch.app.ui.components.StatChip
import com.holadeutsch.app.ui.components.TricolorBar
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartQuiz: () -> Unit,
    onBrowse: () -> Unit,
    onProgress: () -> Unit,
    onVocabLists: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val stats by viewModel.stats.collectAsState()
    val wordOfDay = viewModel.wordOfDay

    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "HOLADEUTSCH",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    TricolorBar()
                }
                HomeMenu(onProgress = onProgress, onVocabLists = onVocabLists)
            }
            Text(greeting(stats.userName), style = MaterialTheme.typography.headlineLarge)
            Text(
                "Tus 1.000 palabras esenciales de alemán",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(
                    "🔥 ${stats.streak} ${if (stats.streak == 1) "día" else "días"}",
                    container = MaterialTheme.colorScheme.tertiaryContainer
                )
                StatChip(
                    "⭐ Nivel ${stats.level}",
                    container = MaterialTheme.colorScheme.primaryContainer
                )
                StatChip(
                    "${stats.totalXp} XP",
                    container = MaterialTheme.colorScheme.secondaryContainer
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Tu nivel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Nivel.entries.forEachIndexed { index, nivel ->
                        SegmentedButton(
                            selected = stats.selectedNivel == nivel.number,
                            onClick = { viewModel.setNivel(nivel.number) },
                            shape = SegmentedButtonDefaults.itemShape(index, Nivel.entries.size)
                        ) {
                            Text("${nivel.number} · ${nivel.cefr}")
                        }
                    }
                }
                Text(
                    Nivel.of(stats.selectedNivel).labelEs,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DailyGoalRing(
                            progress = if (stats.dailyGoal > 0) {
                                stats.wordsToday / stats.dailyGoal.toFloat()
                            } else 0f,
                            label = "${stats.wordsToday}/${stats.dailyGoal}"
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Meta diaria", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "palabras practicadas hoy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onStartQuiz,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Empezar quiz", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Palabras por día",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            val goals = listOf(10, 25, 50)
                            goals.forEachIndexed { index, goal ->
                                SegmentedButton(
                                    selected = stats.dailyGoal == goal,
                                    onClick = { viewModel.setDailyGoal(goal) },
                                    shape = SegmentedButtonDefaults.itemShape(index, goals.size)
                                ) {
                                    Text("$goal")
                                }
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ElevatedCard(onClick = onBrowse, modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Explorar palabras", style = MaterialTheme.typography.titleSmall)
                    }
                }
                ElevatedCard(onClick = onProgress, modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Filled.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Mi progreso", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            wordOfDay?.let { word ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Palabra del día",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ArticleText(word, style = MaterialTheme.typography.headlineSmall)
                            SpeakerButton(viewModel.tts.available) { viewModel.speak(word.german) }
                        }
                        Text(word.spanish, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "“${word.exampleDe}” — ${word.exampleEs}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private const val LEGAL_URL = "https://alemanbasico.com/aviso-legal"
private const val PRIVACY_URL = "https://alemanbasico.com/politica-de-privacidad"

/** Overflow menu: quick access to progress, word lists and the alemanbasico.com legal pages. */
@Composable
private fun HomeMenu(onProgress: () -> Unit, onVocabLists: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Menú")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Mi progreso") },
                leadingIcon = { Icon(Icons.Filled.Insights, contentDescription = null) },
                onClick = {
                    expanded = false
                    onProgress()
                }
            )
            DropdownMenuItem(
                text = { Text("Listas de palabras") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onVocabLists()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Aviso legal") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    uriHandler.openUri(LEGAL_URL)
                }
            )
            DropdownMenuItem(
                text = { Text("Política de privacidad") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    uriHandler.openUri(PRIVACY_URL)
                }
            )
        }
    }
}

private fun greeting(name: String): String {
    val base = when (LocalTime.now().hour) {
        in 5..11 -> "¡Buenos días"
        in 12..19 -> "¡Buenas tardes"
        else -> "¡Buenas noches"
    }
    return if (name.isBlank()) "$base!" else "$base, $name!"
}
