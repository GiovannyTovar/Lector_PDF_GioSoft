package com.giosoft.lectorpdf.ui.library

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.giosoft.lectorpdf.LectorPdfApp
import com.giosoft.lectorpdf.R
import com.giosoft.lectorpdf.data.CategoryRepository
import com.giosoft.lectorpdf.data.DocumentRepository
import com.giosoft.lectorpdf.data.PublicDocuments
import com.giosoft.lectorpdf.data.SafDocuments
import com.giosoft.lectorpdf.data.db.CategoryEntity
import com.giosoft.lectorpdf.data.db.DocumentEntity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Un bloque de la lista: "Favoritos", "Hoy", "Ayer" o una fecha concreta. */
data class DocumentGroup(
    val title: GroupTitle,
    val documents: List<DocumentEntity>,
)

sealed interface GroupTitle {
    data class Resource(@StringRes val resId: Int) : GroupTitle
    data class Literal(val text: String) : GroupTitle
}

/** Mensaje para la barra inferior. [undo] permite restaurar lo quitado. */
data class UiMessage(
    @StringRes val resId: Int,
    val arg: String? = null,
    val undo: DocumentEntity? = null,
)

data class LibraryUiState(
    val groups: List<DocumentGroup> = emptyList(),
    val query: String = "",
    val isEmpty: Boolean = false,
    val hasAnyDocument: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    val filter: LibraryFilter = LibraryFilter.All,
    val hasUncategorized: Boolean = false,
)

class LibraryViewModel(
    private val repository: DocumentRepository,
    private val categories: CategoryRepository,
    private val context: android.content.Context,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<LibraryFilter>(LibraryFilter.All)

    private val messages = Channel<UiMessage>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    val uiState: StateFlow<LibraryUiState> =
        combine(
            repository.observeAll(),
            query,
            filter,
            categories.observeAll(),
        ) { documents, currentQuery, currentFilter, allCategories ->
            // Si la categoria seleccionada se borro, se vuelve a "Todos" en vez
            // de dejar la lista vacia sin explicacion.
            val activeFilter = when {
                currentFilter is LibraryFilter.Category &&
                    allCategories.none { it.id == currentFilter.id } -> LibraryFilter.All
                else -> currentFilter
            }

            val byFilter = when (activeFilter) {
                is LibraryFilter.All -> documents
                is LibraryFilter.Favorites -> documents.filter { it.isFavorite }
                is LibraryFilter.Uncategorized -> documents.filter { it.categoryId == null }
                is LibraryFilter.Category -> documents.filter { it.categoryId == activeFilter.id }
            }

            val filtered = if (currentQuery.isBlank()) {
                byFilter
            } else {
                byFilter.filter { it.name.contains(currentQuery.trim(), ignoreCase = true) }
            }

            LibraryUiState(
                groups = groupDocuments(filtered),
                query = currentQuery,
                isEmpty = filtered.isEmpty(),
                hasAnyDocument = documents.isNotEmpty(),
                categories = allCategories,
                filter = activeFilter,
                hasUncategorized = documents.any { it.categoryId == null },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState(),
        )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: LibraryFilter) {
        filter.value = value
    }

    // --- Categorias ---

    fun createCategory(name: String) = viewModelScope.launch {
        categories.create(name).onFailure { messages.send(UiMessage(R.string.category_empty)) }
    }

    fun renameCategory(category: CategoryEntity, newName: String) = viewModelScope.launch {
        categories.rename(category, newName)
    }

    fun deleteCategory(category: CategoryEntity) = viewModelScope.launch {
        categories.delete(category)
        messages.send(UiMessage(R.string.category_deleted, arg = category.name))
    }

    fun assignCategory(documents: Collection<DocumentEntity>, categoryId: Long?) =
        viewModelScope.launch {
            categories.assignAll(documents, categoryId)
            messages.send(UiMessage(R.string.category_assigned))
        }

    /**
     * Registra un documento elegido en el selector del sistema.
     * Devuelve la ficha para que la interfaz navegue al visor.
     */
    suspend fun registerPickedDocument(uri: Uri, persistable: Boolean): DocumentEntity =
        repository.registerOpened(uri, persistable)

    fun toggleFavorite(document: DocumentEntity) = viewModelScope.launch {
        repository.setFavorite(document, !document.isFavorite)
    }

    /**
     * Quita del historial. **No borra el archivo del celular**, por eso se
     * ofrece deshacer en lugar de un dialogo de confirmacion destructivo.
     */
    fun removeFromHistory(document: DocumentEntity) = viewModelScope.launch {
        repository.removeFromHistory(document)
        messages.send(UiMessage(R.string.remove_done, undo = document))
    }

    fun undoRemove(document: DocumentEntity) = viewModelScope.launch {
        repository.restore(document)
    }

    fun rename(document: DocumentEntity, newName: String) = viewModelScope.launch {
        if (newName.isBlank()) {
            messages.send(UiMessage(R.string.rename_empty))
            return@launch
        }
        repository.rename(document, newName)
            .onSuccess { messages.send(UiMessage(R.string.rename_done, arg = it.name)) }
            .onFailure { messages.send(UiMessage(R.string.rename_failed)) }
    }

    suspend fun canRename(document: DocumentEntity): Boolean = repository.canRename(document)

    suspend fun canDelete(document: DocumentEntity): Boolean = repository.canDelete(document)

    /** Borra el archivo del celular. Sin deshacer: no se puede recuperar. */
    fun deleteFromDevice(document: DocumentEntity) = viewModelScope.launch {
        repository.deleteFromDevice(document)
            .onSuccess { messages.send(UiMessage(R.string.delete_done, arg = document.name)) }
            .onFailure { messages.send(UiMessage(R.string.delete_failed)) }
    }

    /**
     * Copia a "Documentos/Mis PDF" un documento cuyo acceso es temporal
     * (tipicamente llegado por WhatsApp o correo) para que deje de caducar.
     *
     * Es la unica copia que hace la app sobre documentos existentes, y siempre
     * a peticion explicita del usuario.
     */
    fun saveToMisPdf(document: DocumentEntity) = viewModelScope.launch {
        PublicDocuments.save(context, Uri.parse(document.uri), document.name)
            .onSuccess { newUri ->
                repository.registerOpened(newUri, persistable = true)
                repository.removeFromHistory(document)
                messages.send(
                    UiMessage(R.string.save_copy_done, arg = PublicDocuments.displayPath),
                )
            }
            .onFailure { messages.send(UiMessage(R.string.save_copy_failed)) }
    }

    /** Registra el PDF escaneado ya guardado y devuelve su URI para abrirlo. */
    suspend fun registerScanned(uri: Uri): DocumentEntity =
        repository.registerOpened(uri, persistable = true)

    private fun groupDocuments(documents: List<DocumentEntity>): List<DocumentGroup> {
        if (documents.isEmpty()) return emptyList()

        val groups = mutableListOf<DocumentGroup>()

        val favorites = documents.filter { it.isFavorite }
        if (favorites.isNotEmpty()) {
            groups += DocumentGroup(GroupTitle.Resource(R.string.favorites), favorites)
        }

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.getDefault())

        documents
            .groupBy { it.lastOpened.toLocalDate() }
            .toSortedMap(compareByDescending { it })
            .forEach { (date, items) ->
                val title = when (date) {
                    today -> GroupTitle.Resource(R.string.group_today)
                    yesterday -> GroupTitle.Resource(R.string.group_yesterday)
                    else -> GroupTitle.Literal(date.format(formatter).replaceFirstChar { it.uppercase() })
                }
                groups += DocumentGroup(title, items)
            }

        return groups
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LectorPdfApp
                LibraryViewModel(
                    app.container.documentRepository,
                    app.container.categoryRepository,
                    app,
                )
            }
        }
    }
}

