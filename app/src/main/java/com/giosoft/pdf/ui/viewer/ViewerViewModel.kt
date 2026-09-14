package com.giosoft.pdf.ui.viewer

import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.pdf.PdfDocument
import androidx.pdf.PdfLoader
import androidx.pdf.PdfPasswordException
import com.giosoft.pdf.LectorPdfApp
import com.giosoft.pdf.data.DocumentRepository
import com.giosoft.pdf.data.db.DocumentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOException

private const val TAG = "ViewerViewModel"

sealed interface ViewerUiState {

    data object Loading : ViewerUiState

    /**
     * El documento esta cifrado. La interfaz muestra NUESTRO dialogo de
     * contrasena, no uno impuesto por la libreria: androidx.pdf nos entrega el
     * control porque somos nosotros quienes abrimos el documento.
     */
    data class PasswordRequired(
        val documentName: String,
        val previousAttemptFailed: Boolean,
        val checking: Boolean = false,
    ) : ViewerUiState

    data class Ready(
        val document: PdfDocument,
        val entity: DocumentEntity,
        val initialPage: Int,
    ) : ViewerUiState

    data class Failed(val reason: Reason) : ViewerUiState {
        enum class Reason { MISSING, PASSWORD_UNSUPPORTED, GENERIC }
    }
}

class ViewerViewModel(
    private val loader: PdfLoader,
    private val repository: DocumentRepository,
    private val documentUri: Uri,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ViewerUiState>(ViewerUiState.Loading)
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private val _search = MutableStateFlow(SearchState())
    val search: StateFlow<SearchState> = _search.asStateFlow()

    private var searchJob: Job? = null

    private var openDocument: PdfDocument? = null
    private var entity: DocumentEntity? = null

    init {
        load(password = null, isRetry = false)
    }

    fun submitPassword(password: String) {
        _uiState.update { current ->
            if (current is ViewerUiState.PasswordRequired) current.copy(checking = true) else current
        }
        load(password = password, isRetry = true)
    }

    fun retry() = load(password = null, isRetry = false)

    private fun load(password: String?, isRetry: Boolean) = viewModelScope.launch {
        if (!isRetry) _uiState.value = ViewerUiState.Loading

        val stored = repository.find(documentUri)

        try {
            val document = loader.openDocument(documentUri, password)
            openDocument = document

            val registered = repository.registerOpened(
                uri = documentUri,
                persistable = stored?.persistable ?: false,
            )
            repository.setPageCount(documentUri, document.pageCount)
            // Si se abrio sin contrasena, ya no esta cifrado.
            if (password == null) repository.setHasPassword(documentUri, false)
            entity = registered

            _uiState.value = ViewerUiState.Ready(
                document = document,
                entity = registered.copy(pageCount = document.pageCount),
                initialPage = registered.lastPage.coerceIn(0, (document.pageCount - 1).coerceAtLeast(0)),
            )
        } catch (e: PdfPasswordException) {
            // Cifrado: hace falta contrasena, o la introducida es incorrecta.
            // Se anota para poder mostrar el candado en la lista.
            repository.setHasPassword(documentUri, true)
            _uiState.value = ViewerUiState.PasswordRequired(
                documentName = stored?.name ?: documentUri.lastPathSegment.orEmpty(),
                previousAttemptFailed = isRetry,
            )
        } catch (e: SecurityException) {
            // SecurityException que NO es PdfPasswordException: en dispositivos
            // sin la extension de SDK 13, el renderizador del sistema no sabe
            // descifrar y falla sin poder pedir contrasena.
            Log.w(TAG, "Acceso denegado al documento", e)
            _uiState.value = ViewerUiState.Failed(
                if (supportsPasswordProtectedPdf()) {
                    ViewerUiState.Failed.Reason.MISSING
                } else {
                    ViewerUiState.Failed.Reason.PASSWORD_UNSUPPORTED
                },
            )
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "El documento ya no existe", e)
            _uiState.value = ViewerUiState.Failed(ViewerUiState.Failed.Reason.MISSING)
        } catch (e: IOException) {
            Log.e(TAG, "Error de E/S al abrir el documento", e)
            _uiState.value = ViewerUiState.Failed(ViewerUiState.Failed.Reason.GENERIC)
        }
    }

    // --- Buscar dentro del documento ---

    fun onSearchQueryChange(query: String) {
        _search.value = _search.value.copy(query = query)
        // Se cancela la busqueda anterior: al escribir se genera una por cada
        // pulsacion y solo interesa la ultima.
        searchJob?.cancel()
        if (query.isBlank()) {
            _search.value = SearchState()
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val document = openDocument ?: return@launch
            _search.value = _search.value.copy(searching = true, completed = false)
            val matches = runCatching { document.findMatches(query) }.getOrDefault(emptyList())
            _search.value = SearchState(
                query = query,
                matches = matches,
                currentIndex = 0,
                searching = false,
                completed = true,
            )
        }
    }

    fun nextMatch() {
        val state = _search.value
        if (!state.hasResults) return
        _search.value = state.copy(currentIndex = (state.currentIndex + 1) % state.total)
    }

    fun previousMatch() {
        val state = _search.value
        if (!state.hasResults) return
        val index = if (state.currentIndex == 0) state.total - 1 else state.currentIndex - 1
        _search.value = state.copy(currentIndex = index)
    }

    fun closeSearch() {
        searchJob?.cancel()
        _search.value = SearchState()
    }

    /** Guarda la pagina para reanudar la lectura donde se quedo. */
    fun onPageChanged(page: Int) {
        val current = entity ?: return
        if (current.lastPage == page) return
        entity = current.copy(lastPage = page)
        viewModelScope.launch { repository.setLastPage(documentUri, page) }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { openDocument?.close() }
        openDocument = null
    }

    private fun MutableStateFlow<ViewerUiState>.update(block: (ViewerUiState) -> ViewerUiState) {
        value = block(value)
    }

    companion object {
        /**
         * androidx.pdf solo sabe abrir PDF protegidos cuando el sistema expone
         * PdfRendererPreV, es decir Android 12 (S) con la extension de SDK 13
         * o superior. Por debajo cae en PdfRendererCompatAdapter, cuyo
         * constructor ni siquiera acepta contrasena.
         */
        fun supportsPasswordProtectedPdf(): Boolean =
            SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13

        const val ARG_URI = "documentUri"

        /** Espera antes de buscar, para no lanzar una busqueda por tecla. */
        private const val SEARCH_DEBOUNCE_MS = 350L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LectorPdfApp
                val handle: SavedStateHandle = createSavedStateHandle()
                val uri = Uri.parse(requireNotNull(handle.get<String>(ARG_URI)))
                ViewerViewModel(
                    loader = app.container.pdfLoader,
                    repository = app.container.documentRepository,
                    documentUri = uri,
                )
            }
        }
    }
}
