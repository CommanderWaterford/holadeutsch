package com.holadeutsch.app.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResultScreen(
    correct: Int,
    total: Int,
    xp: Int,
    goalMet: Boolean,
    streak: Int,
    perfect: Boolean,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when {
                    perfect -> "🏆"
                    correct >= total / 2 -> "🎉"
                    else -> "💪"
                },
                fontSize = 64.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    perfect -> "¡Perfecto!"
                    correct >= total / 2 -> "¡Muy bien!"
                    else -> "¡Sigue practicando!"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "$correct / $total",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text("aciertos", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("+$xp XP ganados", style = MaterialTheme.typography.titleMedium)
                    if (perfect) Text("Incluye +25 XP por sesión perfecta ⭐")
                    if (goalMet) Text("¡Meta diaria cumplida! +50 XP 🎯")
                    Text("🔥 Racha: $streak ${if (streak == 1) "día" else "días"}")
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
                Text("Otra ronda")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                Text("Volver al inicio")
            }
        }
    }
}
