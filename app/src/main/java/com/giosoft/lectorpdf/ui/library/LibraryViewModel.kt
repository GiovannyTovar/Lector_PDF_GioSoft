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
import com.giosoft.lectorpdf.data.SettingsRepository
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
    val favoritesPosition: Int = 0,
    val showThumbnails: Boolean = false,
    val selected: Set<String> = emptySet(),
    val filter: LibraryFilter = LibraryFilter.All,
    val hasUncategorized: Boolean = false,
)

class LibraryViewModel(
    private val repository: DocumentRepository,
    private val categories: CategoryRepository,
    private val settings: SettingsRepository,
    private val context: android.content.Context,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<LibraryFilter>(LibraryFilter.All)
    private val selection = MutableStateFlow<Set<String>>(emptySet())

    private val messages = Channel<UiMessage>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    val uiState: StateFlow<LibraryUiState> =
        combine(
            repository.observeAll(),
            query,
            filter,
            categories.observeAll(),
            settings.favoritesPosition,
            settings.showThumbnails,
            selection,
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val documents = values[0] as List<DocumentEntity>
            val currentQuery = values[1] as String
            val currentFilter = values[2] as LibraryFilter
            @Suppress("UNCHECKED_CAST")
            val allCategories = values[3] as List<CategoryEntity>
            val favPosition = values[4] as Int
            val thumbnails = values[5] as Boolean
            @Suppress("UNCHECKED_CAST")
            val currentSelection = values[6] as Set<String>
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
                favoritesPosition = favPosition,
                showThumbnails = thumbnails,
                // Solo se conservan los seleccionados que siguen visibles tras
                // filtrar o buscar, para no operar sobre lo que no se ve.
                selected = currentSelection.intersect(filtered.map { it.uri }.toSet()),
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

    fun createCategory(name: String, colorArgb: Int?) = viewModelScope.launch {
        categories.create(name, colorArgb).onFailure { error ->
            val message = if (error.message == CategoryRepository.DUPLICATE) {
                R.string.category_duplicate
            } else {
                R.string.category_empty
            }
            messages.send(UiMessage(message))
        }
    }

    /** Mueve la ficha de Favoritos entre las categorias de la fila de filtros. */
    // --- Seleccion multiple ---

    fun toggleSelection(document: DocumentEntity) {
        val uri = document.uri
        selection.value = if (uri in selection.value) {
            selection.value - uri
        } else {
            selection.value + uri
        }
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun selectAllVisible() {
        selection.value = uiState.value.groups.flatMap { group -> group.documents }
            .map { it.uri }
            .toSet()
    }

    /** Documentos seleccionados, en el orden en que se ven. */
    fun selectedDocuments(): List<DocumentEntity> =
        uiState.value.groups.flatMap { it.documents }
            .filter { it.uri in uiState.value.selected }

    fun removeSelected() = viewModelScope.launch {
        val documents = selectedDocuments()
        clearSelection()
        documents.forEach { repository.removeFromHistory(it) }
        messages.send(UiMessage(R.string.selection_removed, arg = documents.size.toString()))
    }

    fun favoriteSelected(favorite: Boolean) = viewModelScope.launch {
        selectedDocuments().forEach { repository.setFavorite(it, favorite) }
        clearSelection()
    }

    fun setShowThumbnails(enabled: Boolean) = viewModelScope.launch {
        settings.setShowThumbnails(enabled)
    }

    fun moveFavorites(up: Boolean) = viewModelScope.launch {
        val current = uiState.value.favoritesPosition
        val limit = uiState.value.categories.size
        settings.setFavoritesPosition((if (up) current - 1 else current + 1).coerceIn(0, limit))
    }

    fun moveCategory(from: Int, up: Boolean) = viewModelScope.launch {
        categories.move(uiState.value.categories, from, up)
    }

    fun setCategoryColor(category: CategoryEntity, colorArgb: Int) = viewModelScope.launch {
        categories.setColor(category, colorArgb)
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

    fun toggleLocked(document: DocumentEntity) = viewModelScope.launch {
        repository.setLocked(document, !document.isLocked)
        messages.send(
            UiMessage(
                if (document.isLocked) R.string.lock_removed else R.string.lock_added,
            ),
        )
    }

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

        // Ya NO se crea un bloque "Favoritos": los favoritos tienen su propia
        // ficha en la fila de filtros, y duplicar aqui cada documento hacia que
        // apareciera dos veces en la lista (una en Favoritos y otra en su
        // fecha). La estrella en la tarjeta basta para reconocerlos.
        val groups = mutableListOf<DocumentGroup>()

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
                    app.container.settingsRepository,
                    app,
                )
            }
        }
    }
}

