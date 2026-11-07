package org.hiperfoco.app

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Locale

internal class HiperFocoViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = HiperFocoDatabase.get(application).universeDao()

    private val _readError = MutableStateFlow(false)
    val readError = _readError.asStateFlow()

    val universes = dao.observeUniverses()
        .catch { error ->
            if (error !is SQLiteException) throw error
            _readError.value = true
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    data class Editor(
        val title: String,
        val name: String,
        val icon: String? = null,
        val color: Long = 0xFF6554A4,
        val note: String? = null,
        val save: suspend (String, String?, Long) -> Boolean,
        val saving: Boolean = false,
        val error: String? = null,
    )

    data class Deletion(
        val message: String,
        val delete: suspend () -> Unit,
        val saving: Boolean = false,
        val error: String? = null,
    )

    private val _editor = MutableStateFlow<Editor?>(null)
    val editor = _editor.asStateFlow()

    private val _deletion = MutableStateFlow<Deletion?>(null)
    val deletion = _deletion.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0L)
    val timerSeconds = _timerSeconds.asStateFlow()
    private var timerJob: kotlinx.coroutines.Job? = null

    private val _exportJson = MutableStateFlow<String?>(null)
    val exportJson = _exportJson.asStateFlow()

    fun editUniverse(universe: Universe? = null) {
        _editor.value = Editor(
            title = if (universe == null) "Novo universo" else "Editar universo",
            name = universe?.name.orEmpty(),
            icon = universe?.icon,
            color = universe?.color ?: 0xFF6554A4,
            save = { name, icon, color ->
                if (universe == null) dao.insertUniverse(Universe(name, icon = icon, color = color)) != -1L
                else dao.updateUniverse(Universe(name, icon = icon, color = color, id = universe.id)) > 0
            },
        )
    }

    fun setUniverseIconColor(icon: String?, color: Long) {
        val current = _editor.value ?: return
        if (!current.saving) _editor.value = current.copy(icon = icon, color = color, error = null)
    }

    fun editTopic(universeId: Long, topic: Topic? = null) {
        _editor.value = Editor(
            title = if (topic == null) "Novo subtema" else "Editar subtema",
            name = topic?.name.orEmpty(),
            note = topic?.note,
            save = { name, _, _ ->
                if (topic == null) dao.insertTopic(Topic(universeId, name)) != -1L
                else dao.updateTopic(Topic(universeId, name, note = currentNote(), done = topic?.done ?: false, id = topic.id)) > 0
            },
        )
    }

    private fun currentNote(): String? {
        return _editor.value?.note
    }

    fun changeName(name: String) {
        val current = _editor.value ?: return
        if (!current.saving) _editor.value = current.copy(name = name, error = null)
    }

    fun dismissEditor() {
        if (_editor.value?.saving == false) _editor.value = null
    }

    fun saveName() {
        val current = _editor.value ?: return
        val name = current.name.trim()
        if (current.saving || name.isEmpty()) return
        _editor.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            _editor.value = try {
                if (current.save(name, current.icon, current.color)) null
                else current.copy(error = "Já existe um item com esse nome.")
            } catch (_: SQLiteException) {
                current.copy(error = "Não foi possível salvar. Tente novamente.")
            }
        }
    }

    fun saveTopicNote(note: String?) {
        val current = _editor.value ?: return
        if (current.saving) return
        _editor.value = current.copy(note = note, error = null)
    }

    fun saveUniverseAppearance() {
        val current = _editor.value ?: return
        if (current.saving) return
        // A aparência é persistida junto com o nome ao salvar; este método
        // serve para aplicar a escolha de ícone/cor sem fechar o editor.
        _editor.value = current.copy(error = null)
    }

    fun confirmDelete(universe: Universe) {
        _deletion.value = Deletion(
            message = "Excluir “${universe.name}” e todos os seus subtemas?",
            delete = { dao.deleteUniverse(universe.id) },
        )
    }

    fun confirmDelete(topic: Topic) {
        _deletion.value = Deletion(
            message = "Excluir o subtema “${topic.name}”?",
            delete = { dao.deleteTopic(topic.id) },
        )
    }

    fun dismissDeletion() {
        if (_deletion.value?.saving == false) _deletion.value = null
    }

    fun delete() {
        val current = _deletion.value ?: return
        if (current.saving) return
        _deletion.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            _deletion.value = try {
                current.delete()
                null
            } catch (_: SQLiteException) {
                current.copy(error = "Não foi possível excluir. Tente novamente.")
            }
        }
    }

    fun toggleTopicDone(universeId: Long, topicId: Long, done: Boolean) {
        viewModelScope.launch {
            dao.toggleTopicDone(topicId, done)
        }
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (isActive) {
                _timerSeconds.value += 1
                delay(1000)
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun resetTimer() {
        stopTimer()
        _timerSeconds.value = 0
    }

    fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    suspend fun exportToJson(): String {
        val universes: List<UniverseWithTopics> = dao.observeUniverses().first()
        val builder = StringBuilder()
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(java.util.Date())
        builder.append("{\n")
        builder.append("  \"exportedAt\": \"$date\",\n")
        builder.append("  \"universes\": [\n")
        for ((uIndex, entry) in universes.withIndex()) {
            builder.append("    {\n")
            builder.append("      \"name\": ${quote(entry.universe.name)},\n")
            builder.append("      \"icon\": ${quote(entry.universe.icon)},\n")
            builder.append("      \"color\": ${entry.universe.color},\n")
            builder.append("      \"topics\": [\n")
            for ((tIndex, topic) in entry.topics.withIndex()) {
                builder.append("        {\n")
                builder.append("          \"name\": ${quote(topic.name)},\n")
                builder.append("          \"note\": ${quote(topic.note)},\n")
                builder.append("          \"done\": ${topic.done}\n")
                builder.append("        }${if (tIndex < entry.topics.size - 1) "," else ""}\n")
            }
            builder.append("      ]\n")
            builder.append("    }${if (uIndex < universes.size - 1) "," else ""}\n")
        }
        builder.append("  ]\n")
        builder.append("}\n")
        return builder.toString()
    }

    fun requestExport() {
        viewModelScope.launch {
            _exportJson.value = exportToJson()
        }
    }

    private fun quote(value: String?): String =
        if (value == null) "null" else "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
