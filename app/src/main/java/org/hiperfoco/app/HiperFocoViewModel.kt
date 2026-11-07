package org.hiperfoco.app

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
        val save: suspend (String) -> Boolean,
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

    fun editUniverse(universe: Universe? = null) {
        _editor.value = Editor(
            title = if (universe == null) "Novo universo" else "Editar universo",
            name = universe?.name.orEmpty(),
            save = { name ->
                if (universe == null) dao.insertUniverse(Universe(name)) != -1L
                else dao.updateUniverse(Universe(name, id = universe.id)) > 0
            },
        )
    }

    fun editTopic(universeId: Long, topic: Topic? = null) {
        _editor.value = Editor(
            title = if (topic == null) "Novo subtema" else "Editar subtema",
            name = topic?.name.orEmpty(),
            save = { name ->
                if (topic == null) dao.insertTopic(Topic(universeId, name)) != -1L
                else dao.updateTopic(Topic(universeId, name, id = topic.id)) > 0
            },
        )
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
                if (current.save(name)) null
                else current.copy(error = "Já existe um item com esse nome.")
            } catch (_: SQLiteException) {
                current.copy(error = "Não foi possível salvar. Tente novamente.")
            }
        }
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
}
