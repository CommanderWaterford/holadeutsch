package com.holadeutsch.app.ui.vocab

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.holadeutsch.app.data.model.Nivel
import com.holadeutsch.app.ui.AppViewModelProvider
import com.holadeutsch.app.ui.components.ArticleText
import com.holadeutsch.app.ui.components.SpeakerButton

/** Reference lists: every word of each nivel, German vs. Spanish, side by side. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabListsScreen(
    onBack: () -> Unit,
    viewModel: VocabViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var tab by remember { mutableIntStateOf(0) }
    val nivel = Nivel.entries[tab]
    val words = viewModel.wordsByNivel[nivel.number].orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listas de palabras") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Nivel.entries.forEachIndexed { index, n ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text("${n.number} · ${n.cefr}") }
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Alemán",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Español",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${words.size} palabras",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()

            LazyColumn(Modifier.fillMaxSize()) {
                items(words, key = { it.id }) { word ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            ArticleText(word, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            word.spanish,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SpeakerButton(viewModel.tts.available) { viewModel.speak(word.german) }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
