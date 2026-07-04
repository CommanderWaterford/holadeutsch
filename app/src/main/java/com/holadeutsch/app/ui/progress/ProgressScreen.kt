package com.holadeutsch.app.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.holadeutsch.app.ui.AppViewModelProvider
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    viewModel: ProgressViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val ui by viewModel.ui.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi progreso") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Streak
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Racha", style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "🔥 ${ui.stats.streak} ${if (ui.stats.streak == 1) "día" else "días"}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Mejor: ${ui.stats.longestStreak}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val today = LocalDate.now().toEpochDay()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0 until 30).map { today - 29 + it }.chunked(10).forEach { rowDays ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowDays.forEach { day ->
                                    androidx.compose.foundation.layout.Box(
                                        Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (day in ui.stats.activeDays) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Últimos 30 días",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Overall stats
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Estadísticas · Nivel ${ui.stats.selectedNivel}", style = MaterialTheme.typography.titleMedium)
                    StatRow("Palabras dominadas", "${ui.masteredCount} / ${ui.totalWords}")
                    StatRow("Nivel", "${ui.stats.level} (${ui.stats.totalXp} XP)")
                    LinearProgressIndicator(
                        progress = { ui.stats.levelProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ui.accuracyPercent?.let { StatRow("Precisión", "$it %") }
                }
            }

            // Per-category mastery
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Dominio por categoría", style = MaterialTheme.typography.titleMedium)
                    ui.perCategory.forEach { mastery ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(mastery.category.labelEs, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${(mastery.fraction * 100).roundToInt()} %",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            LinearProgressIndicator(
                                progress = { mastery.fraction },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Settings
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ajustes", style = MaterialTheme.typography.titleMedium)
                    Text("Meta diaria (palabras)", style = MaterialTheme.typography.bodyMedium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        val goals = listOf(5, 10, 15, 20)
                        goals.forEachIndexed { index, goal ->
                            SegmentedButton(
                                selected = ui.stats.dailyGoal == goal,
                                onClick = { viewModel.setDailyGoal(goal) },
                                shape = SegmentedButtonDefaults.itemShape(index, goals.size)
                            ) {
                                Text("$goal")
                            }
                        }
                    }
                    HorizontalDivider()
                    SettingSwitch(
                        "Pronunciación (voz alemana)",
                        ui.stats.ttsEnabled,
                        viewModel::setTtsEnabled
                    )
                    SettingSwitch("Vibración", ui.stats.hapticsEnabled, viewModel::setHapticsEnabled)
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reiniciar progreso", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("¿Reiniciar progreso?") },
            text = {
                Text("Se borrarán tu XP, tu racha y el dominio de las palabras. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetProgress()
                    showResetDialog = false
                }) {
                    Text("Reiniciar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
