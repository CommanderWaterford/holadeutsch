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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.holadeutsch.app.ui.AppViewModelProvider
import com.holadeutsch.app.ui.components.ArticleText
import com.holadeutsch.app.ui.components.DailyGoalRing
import com.holadeutsch.app.ui.components.SpeakerButton
import com.holadeutsch.app.ui.components.StatChip
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartQuiz: () -> Unit,
    onBrowse: () -> Unit,
    onProgress: () -> Unit,
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
            Text(
                greeting(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Tus 100 palabras esenciales de alemán",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("🔥 ${stats.streak} ${if (stats.streak == 1) "día" else "días"}")
                StatChip("⭐ Nivel ${stats.level}")
                StatChip("${stats.totalXp} XP")
            }

            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        Button(onClick = onStartQuiz, modifier = Modifier.fillMaxWidth()) {
                            Text("Empezar quiz")
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

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "¡Buenos días!"
    in 12..19 -> "¡Buenas tardes!"
    else -> "¡Buenas noches!"
}
