package org.hiperfoco.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.Normalizer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF6554A4))) {
                HiperFocoScreen(viewModel())
            }
        }
    }
}

@Composable
private fun HiperFocoScreen(model: HiperFocoViewModel) {
    val universes by model.universes.collectAsStateWithLifecycle()
    val readError by model.readError.collectAsStateWithLifecycle()
    val editor by model.editor.collectAsStateWithLifecycle()
    val deletion by model.deletion.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val selected = universes?.find { it.universe.id == selectedId }
    val query = remember(search) { search.trim().searchKey() }
    val results = remember(universes, query) {
        universes.orEmpty().mapNotNull { entry ->
            val matchedTopics = if (query.isEmpty()) emptyList() else entry.topics
                .filter { it.name.searchKey().contains(query) }
                .sortedBy { it.id }
            if (entry.universe.name.searchKey().contains(query) || matchedTopics.isNotEmpty()) {
                entry.universe to matchedTopics
            } else null
        }
    }

    LaunchedEffect(universes, selectedId) {
        if (universes != null && selectedId != null && selected == null) selectedId = null
    }
    BackHandler(enabled = selectedId != null) { selectedId = null }

    Scaffold(topBar = {
        Surface(color = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
            Column(
                Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(20.dp),
            ) {
                Text("HiperFoco", style = MaterialTheme.typography.headlineMedium)
                Text("Seu conhecimento, seu universo.")
            }
        }
    }) { padding ->
        if (universes == null && !readError) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = key(selectedId) { rememberLazyListState() },
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                    .testTag("lista-interesses"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    readError -> item { Text("Não foi possível abrir os dados do HiperFoco.") }
                    selected == null -> {
                        item {
                            Column(
                                modifier = Modifier.padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("Meus universos", style = MaterialTheme.typography.titleLarge)
                                Button(onClick = { model.editUniverse() }) { Text("Criar universo") }
                                OutlinedTextField(
                                    value = search,
                                    onValueChange = { search = it },
                                    label = { Text("Buscar universos e subtemas") },
                                    modifier = Modifier.fillMaxWidth().testTag("busca-interesses"),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                    trailingIcon = if (search.isNotEmpty()) {
                                        { TextButton(onClick = { search = "" }) { Text("Limpar") } }
                                    } else null,
                                )
                                if (results.isEmpty()) {
                                    Text(if (query.isEmpty()) "Nenhum universo cadastrado." else "Nenhum resultado encontrado.")
                                }
                            }
                        }
                        items(results, key = { it.first.id }) { (universe, matchedTopics) ->
                            InterestCard(
                                name = universe.name,
                                matchedTopics = matchedTopics,
                                onOpen = {
                                    focusManager.clearFocus()
                                    selectedId = universe.id
                                },
                                onEdit = { model.editUniverse(universe) },
                                onDelete = { model.confirmDelete(universe) },
                            )
                        }
                    }
                    else -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { selectedId = null }) { Text("Voltar aos universos") }
                                Text(selected.universe.name, style = MaterialTheme.typography.titleLarge)
                                Text("Subtemas", style = MaterialTheme.typography.titleMedium)
                                Button(onClick = { model.editTopic(selected.universe.id) }) { Text("Criar subtema") }
                                if (selected.topics.isEmpty()) Text("Nenhum subtema cadastrado.")
                            }
                        }
                        items(selected.topics.sortedBy { it.id }, key = { it.id }) { topic ->
                            InterestCard(
                                name = topic.name,
                                onEdit = { model.editTopic(selected.universe.id, topic) },
                                onDelete = { model.confirmDelete(topic) },
                            )
                        }
                    }
                }
            }
        }
    }

    editor?.let { state ->
        AlertDialog(
            onDismissRequest = model::dismissEditor,
            title = { Text(state.title) },
            text = {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = model::changeName,
                    label = { Text("Nome") },
                    enabled = !state.saving,
                    singleLine = true,
                    isError = state.error != null,
                    supportingText = state.error?.let { message -> { Text(message) } },
                )
            },
            confirmButton = {
                TextButton(onClick = model::saveName, enabled = !state.saving && state.name.isNotBlank()) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = model::dismissEditor, enabled = !state.saving) { Text("Cancelar") }
            },
        )
    }

    deletion?.let { state ->
        AlertDialog(
            onDismissRequest = model::dismissDeletion,
            title = { Text("Confirmar exclusão") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.message)
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = model::delete, enabled = !state.saving) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = model::dismissDeletion, enabled = !state.saving) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun InterestCard(
    name: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpen: (() -> Unit)? = null,
    matchedTopics: List<Topic> = emptyList(),
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(4.dp))
            for (topic in matchedTopics) {
                Text(
                    topic.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            FlowRow {
                if (onOpen != null) {
                    TextButton(
                        onClick = onOpen,
                        modifier = Modifier.semantics { contentDescription = "Abrir subtemas de $name" },
                    ) { Text("Abrir subtemas") }
                }
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.semantics { contentDescription = "Editar $name" },
                ) { Text("Editar") }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.semantics { contentDescription = "Excluir $name" },
                ) { Text("Excluir") }
            }
        }
    }
}

private val accentMarks = Regex("\\p{M}+")

private fun String.searchKey(): String =
    accentMarks.replace(Normalizer.normalize(nameKey(this), Normalizer.Form.NFD), "")
