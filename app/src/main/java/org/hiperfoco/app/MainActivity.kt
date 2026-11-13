package org.hiperfoco.app

import android.content.Intent
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.Normalizer

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.context(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("hiperfoco", MODE_PRIVATE)
        setContent {
            var darkMode by rememberSaveable { mutableStateOf(preferences.getBoolean("darkMode", false)) }
            val colorScheme = if (darkMode) {
                darkColorScheme(primary = Color(0xFF9F86C0), surface = Color(0xFF1E1E1E))
            } else {
                lightColorScheme(primary = Color(0xFF6554A4))
            }
            MaterialTheme(colorScheme = colorScheme) {
                HiperFocoScreen(
                    viewModel(),
                    darkMode,
                    { enabled ->
                        darkMode = enabled
                        preferences.edit().putBoolean("darkMode", enabled).apply()
                    },
                )
            }
        }
    }

    @Composable
    private fun HiperFocoScreen(
        model: HiperFocoViewModel,
        darkMode: Boolean,
        onDarkModeChange: (Boolean) -> Unit,
    ) {
        var showSettings by rememberSaveable { mutableStateOf(false) }
        val settingsLabel = stringResource(R.string.settings)
        val themeLabel = stringResource(if (darkMode) R.string.light_mode else R.string.dark_mode)
        val exportLabel = stringResource(R.string.export)
        val sessionLabel = stringResource(R.string.session_accessibility)
        val universes by model.universes.collectAsStateWithLifecycle()
        val readError by model.readError.collectAsStateWithLifecycle()
        val editor by model.editor.collectAsStateWithLifecycle()
        val deletion by model.deletion.collectAsStateWithLifecycle()
        val timerSeconds by model.timerSeconds.collectAsStateWithLifecycle()
        val breakReminder by model.breakReminder.collectAsStateWithLifecycle()
        val exportJson by model.exportJson.collectAsStateWithLifecycle()
        val exportError by model.exportError.collectAsStateWithLifecycle()
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
            if (universes != null && selectedId != null && selected == null) {
                selectedId = null
                model.stopTimer()
            }
        }
        BackHandler(enabled = selectedId != null) {
            selectedId = null
            model.stopTimer()
        }

        var showExportDialog by remember { mutableStateOf(false) }
        LaunchedEffect(exportJson) {
            if (exportJson != null) {
                showExportDialog = true
            }
        }

        Scaffold(
            topBar = {
                Surface(color = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
                    Column(
                        Modifier.fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                            .padding(20.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodySmall)
                        FlowRow {
                            TextButton(
                                onClick = { onDarkModeChange(!darkMode) },
                                modifier = Modifier.semantics { contentDescription = themeLabel },
                            ) {
                                Text(if (darkMode) "☀️" else "🌙")
                            }
                            TextButton(
                                onClick = { model.requestExport() },
                                modifier = Modifier.semantics { contentDescription = exportLabel },
                            ) {
                                Text("💾")
                            }
                            TextButton(
                                onClick = { showSettings = true },
                                modifier = Modifier.semantics { contentDescription = settingsLabel },
                            ) {
                                Text("⚙️")
                            }
                        }
                    }
                }
            },
        ) { padding ->
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
                        readError -> item { Text(stringResource(R.string.read_error)) }
                        selected == null -> {
                            item {
                                Column(
                                    modifier = Modifier.padding(top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(stringResource(R.string.my_universes), style = MaterialTheme.typography.titleLarge)
                                    Button(onClick = { model.editUniverse() }) { Text(stringResource(R.string.create_universe)) }
                                    OutlinedTextField(
                                        value = search,
                                        onValueChange = { search = it },
                                        label = { Text(stringResource(R.string.search_interests)) },
                                        modifier = Modifier.fillMaxWidth().testTag("busca-interesses"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                        trailingIcon = if (search.isNotEmpty()) {
                                            { TextButton(onClick = { search = "" }) { Text(stringResource(R.string.clear)) } }
                                        } else null,
                                    )
                                    if (results.isEmpty()) {
                                        Text(stringResource(if (query.isEmpty()) R.string.empty_universes else R.string.no_results))
                                    }
                                }
                            }
                            items(results, key = { it.first.id }) { (universe, matchedTopics) ->
                                UniverseCard(
                                    icon = universe.icon,
                                    color = universe.color,
                                    name = universe.name,
                                    matchedTopics = matchedTopics,
                                    onOpen = {
                                        focusManager.clearFocus()
                                        model.startSession()
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        TextButton(onClick = { selectedId = null; model.stopTimer() }) {
                                            Text(stringResource(R.string.back_to_universes))
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            stringResource(R.string.session_time, model.formatTime(timerSeconds)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.semantics { contentDescription = sessionLabel },
                                        )
                                    }
                                    Text(selected.universe.name, style = MaterialTheme.typography.titleLarge)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(stringResource(R.string.topics), style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.weight(1f))
                                        Button(onClick = { model.editTopic(selected.universe.id) }) {
                                            Text(stringResource(R.string.create_topic))
                                        }
                                    }
                                    if (selected.topics.isEmpty()) Text(stringResource(R.string.empty_topics))
                                    if (breakReminder) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text(
                                                    stringResource(R.string.break_reminder, model.formatTime(timerSeconds)),
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                                TextButton(onClick = model::dismissBreakReminder) {
                                                    Text(stringResource(R.string.acknowledge))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            items(selected.topics.sortedBy { it.id }, key = { it.id }) { topic ->
                                TopicCard(
                                    name = topic.name,
                                    done = topic.done,
                                    onDoneChange = { model.toggleTopicDone(selected.universe.id, topic.id, it) },
                                    onEdit = { model.editTopic(selected.universe.id, topic) },
                                    onDelete = { model.confirmDelete(topic) },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSettings) {
            val current = AppLanguage.selected(this)
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text(settingsLabel) },
                text = {
                    Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                        Text(stringResource(R.string.language))
                        AppLanguage.entries.forEach { language ->
                            val selectedLanguage = language == current
                            Row(
                                modifier = Modifier.fillMaxWidth().selectable(
                                    selected = selectedLanguage,
                                    role = Role.RadioButton,
                                    onClick = {
                                        if (!selectedLanguage) {
                                            language.save(this@MainActivity)
                                            recreate()
                                        }
                                    },
                                ).padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selectedLanguage, onClick = null)
                                Text(stringResource(language.label), modifier = Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettings = false }) { Text(stringResource(R.string.close)) }
                },
            )
        }

        editor?.let { state ->
            val isUniverse = state.isUniverse
            var localNote by remember(state) { mutableStateOf(state.note.orEmpty()) }
            LaunchedEffect(state) {
                localNote = state.note.orEmpty()
            }
            AlertDialog(
                onDismissRequest = model::dismissEditor,
                title = { Text(stringResource(state.title)) },
                text = {
                    Column(modifier = Modifier.width(320.dp)) {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = model::changeName,
                            label = { Text(stringResource(R.string.name)) },
                            modifier = Modifier.testTag("campo-nome"),
                            enabled = !state.saving,
                            singleLine = true,
                            isError = state.error != null,
                            supportingText = state.error?.let { message -> { Text(stringResource(message)) } },
                        )
                        if (isUniverse) {
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.icon), style = MaterialTheme.typography.labelSmall)
                            EmojiSelector(
                                selected = state.icon,
                                onEmojiSelected = { model.setUniverseIconColor(it, state.color) },
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.color), style = MaterialTheme.typography.labelSmall)
                            ColorSelector(
                                selected = state.color,
                                onColorSelected = { model.setUniverseIconColor(state.icon, it) },
                            )
                        } else {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = localNote,
                                onValueChange = { localNote = it; model.saveTopicNote(it.ifBlank { null }) },
                                label = { Text(stringResource(R.string.note)) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                                maxLines = 5,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = model::saveName, enabled = !state.saving && state.name.isNotBlank()) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = model::dismissEditor, enabled = !state.saving) { Text(stringResource(R.string.cancel)) }
                },
            )
        }

        deletion?.let { state ->
            AlertDialog(
                onDismissRequest = model::dismissDeletion,
                title = { Text(stringResource(R.string.confirm_delete)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(state.message, state.name))
                        state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = model::delete, enabled = !state.saving) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = {
                    TextButton(onClick = model::dismissDeletion, enabled = !state.saving) { Text(stringResource(R.string.cancel)) }
                },
            )
        }

        val json = exportJson
        if (showExportDialog && json != null) {
            ExportDialog(
                json = json,
                onDismiss = {
                    showExportDialog = false
                    model.clearExport()
                },
            )
        }

        exportError?.let { message ->
            AlertDialog(
                onDismissRequest = model::clearExport,
                title = { Text(stringResource(R.string.export)) },
                text = { Text(stringResource(message)) },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = model::clearExport) { Text(stringResource(R.string.close)) }
                },
            )
        }
    }

    @Composable
    private fun EmojiSelector(selected: String?, onEmojiSelected: (String?) -> Unit) {
        val noIconLabel = stringResource(R.string.no_icon)
        val emojis = listOf("🌌", "💻", "📜", "🐧", "🦖", "📚", "🎵", "🎨", "🧪", "🚀", "🐱", "🐶", "🌸", "⭐", "🌙", "🔥", "💡", "🎯")
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (emoji in emojis) {
                val emojiLabel = stringResource(R.string.icon_label, emoji)
                val isSelected = selected == emoji
                Box(
                    modifier = Modifier
                        .requiredSize(48.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified,
                            shape = CircleShape,
                        )
                        .padding(2.dp)
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, fontSize = 20.sp, modifier = Modifier.semantics { contentDescription = emojiLabel })
                }
            }
            Box(
                modifier = Modifier
                    .requiredSize(36.dp)
                    .background(
                        if (selected == null) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified,
                        shape = CircleShape,
                    )
                    .padding(2.dp)
                    .clickable { onEmojiSelected(null) },
                contentAlignment = Alignment.Center,
            ) {
                Text("–", fontSize = 20.sp, modifier = Modifier.semantics { contentDescription = noIconLabel })
            }
        }
    }

    @Composable
    private fun ColorSelector(selected: Long, onColorSelected: (Long) -> Unit) {
        val colors = listOf(
            0xFF6554A4L to R.string.purple,
            0xFF37474FL to R.string.gray,
            0xFF1565C0L to R.string.blue,
            0xFF2E7D32L to R.string.green,
            0xFFC62828L to R.string.red,
            0xFFF57C00L to R.string.orange,
            0xFF6A1B9AL to R.string.violet,
            0xFF455A64L to R.string.dark_blue,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for ((color, label) in colors) {
                val colorLabel = stringResource(R.string.color_label, stringResource(label))
                val isSelected = selected == color
                Box(
                    modifier = Modifier
                        .requiredSize(44.dp)
                        .background(Color(color), shape = CircleShape)
                        .border(
                            if (isSelected) 2.dp else 0.dp,
                            MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                        )
                        .clickable { onColorSelected(color) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (isSelected) "✓" else "",
                        color = Color.White,
                        modifier = Modifier.semantics { contentDescription = colorLabel },
                    )
                }
            }
        }
    }

    @Composable
    private fun UniverseCard(
        icon: String?,
        color: Long,
        name: String,
        matchedTopics: List<Topic>,
        onOpen: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
    ) {
        val openLabel = stringResource(R.string.open_topics_label, name)
        val editLabel = stringResource(R.string.edit_label, name)
        val deleteLabel = stringResource(R.string.delete_label, name)
        Card(
            Modifier.fillMaxWidth()
                .border(2.dp, Color(color), MaterialTheme.shapes.medium),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(icon.orEmpty(), fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                }
                for (topic in matchedTopics) {
                    Text(
                        topic.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                FlowRow {
                    TextButton(
                        onClick = onOpen,
                        modifier = Modifier.semantics { contentDescription = openLabel },
                    ) { Text(stringResource(R.string.open_topics)) }
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.semantics { contentDescription = editLabel },
                    ) { Text(stringResource(R.string.edit)) }
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.semantics { contentDescription = deleteLabel },
                    ) { Text(stringResource(R.string.delete)) }
                }
            }
        }
    }

    @Composable
    private fun TopicCard(
        name: String,
        done: Boolean,
        onDoneChange: (Boolean) -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
    ) {
        val doneLabel = stringResource(R.string.done_label, name)
        val editLabel = stringResource(R.string.edit_label, name)
        val deleteLabel = stringResource(R.string.delete_label, name)
        Card(
            Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = done,
                    onCheckedChange = onDoneChange,
                    modifier = Modifier.semantics { contentDescription = doneLabel },
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                FlowRow {
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.semantics { contentDescription = editLabel },
                    ) { Text(stringResource(R.string.edit)) }
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.semantics { contentDescription = deleteLabel },
                    ) { Text(stringResource(R.string.delete)) }
                }
            }
        }
    }

    @Composable
    private fun ExportDialog(
        json: String,
        onDismiss: () -> Unit,
    ) {
        val context = LocalContext.current
        val subject = stringResource(R.string.backup_subject)
        val chooserTitle = stringResource(R.string.share_backup)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.export_json)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = 200.dp, max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    SelectionContainer {
                        Text(
                            text = json,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(intent, chooserTitle))
                    },
                ) { Text(stringResource(R.string.share)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            },
        )
    }

    private val accentMarks = Regex("\\p{M}+")

    private fun String.searchKey(): String =
        accentMarks.replace(Normalizer.normalize(nameKey(this), Normalizer.Form.NFD), "")
}
