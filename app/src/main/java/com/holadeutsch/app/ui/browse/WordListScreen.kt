package com.holadeutsch.app.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.holadeutsch.app.data.model.Category
import com.holadeutsch.app.data.model.Word
import com.holadeutsch.app.ui.AppViewModelProvider
import com.holadeutsch.app.ui.components.ArticleText
import com.holadeutsch.app.ui.components.MasteryDots
import com.holadeutsch.app.ui.components.SpeakerButton
import com.holadeutsch.app.ui.components.StatChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    onBack: () -> Unit,
    viewModel: BrowseViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val ui by viewModel.ui.collectAsState()
    var selected by remember { mutableStateOf<Word?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Palabras · Nivel ${ui.nivel}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = ui.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Buscar en alemán o español…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = ui.category == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text("Todas") }
                    )
                }
                items(Category.entries) { cat ->
                    FilterChip(
                        selected = ui.category == cat,
                        onClick = {
                            viewModel.setCategory(if (ui.category == cat) null else cat)
                        },
                        label = { Text(cat.labelEs) }
                    )
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(ui.words, key = { it.id }) { word ->
                    ListItem(
                        modifier = Modifier.clickable { selected = word },
                        headlineContent = {
                            ArticleText(word, style = MaterialTheme.typography.titleMedium)
                        },
                        supportingContent = { Text(word.spanish) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MasteryDots(ui.mastery[word.id] ?: 0)
                                SpeakerButton(viewModel.tts.available) {
                                    viewModel.speak(word.german)
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    selected?.let { word ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(
                Modifier.padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArticleText(word, style = MaterialTheme.typography.headlineMedium)
                    SpeakerButton(viewModel.tts.available) { viewModel.speak(word.german) }
                }
                Text(word.spanish, style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(word.pos)
                    StatChip(word.level)
                    StatChip(word.category.labelEs)
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    word.exampleDe,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    word.exampleEs,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MasteryDots(ui.mastery[word.id] ?: 0)
                    Text(
                        "Dominio: ${ui.mastery[word.id] ?: 0}/5",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
