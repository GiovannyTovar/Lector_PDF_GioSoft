package com.giosoft.lectorpdf.ui.library

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giosoft.lectorpdf.R
import com.giosoft.lectorpdf.data.SafDocuments
import com.giosoft.lectorpdf.data.db.DocumentEntity
import com.giosoft.lectorpdf.data.PublicDocuments
import com.giosoft.lectorpdf.scan.DocumentScanner
import com.giosoft.lectorpdf.ui.scan.ScanSaveDialog
import com.giosoft.lectorpdf.ui.about.AboutDialog
import com.giosoft.lectorpdf.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenDocument: (String) -> Unit,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val isDark = LocalIsDarkTheme.current

    var searching by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<DocumentEntity?>(null) }
    var renamingAllowed by remember { mutableStateOf(true) }
    var deleting by remember { mutableStateOf<DocumentEntity?>(null) }
    var deletingAllowed by remember { mutableStateOf(true) }
    var pendingScanPdf by remember { mutableStateOf<Uri?>(null) }

    // --- Selector del sistema: abre el archivo ORIGINAL, sin copiarlo ---
    val pickDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        scope.launch {
            val persistable = SafDocuments.takePersistablePermission(context, uri)
            val document = viewModel.registerPickedDocument(uri, persistable)
            onOpenDocument(document.uri)
        }
    }

    // --- Guardar el PDF escaneado donde el usuario elija ---
    val saveScanToChosenFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val destination = result.data?.data
        val source = pendingScanPdf
        pendingScanPdf = null
        if (result.resultCode != Activity.RESULT_OK || destination == null || source == null) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            DocumentScanner.saveTo(context, source, destination)
                .onSuccess {
                    val persistable = SafDocuments.takePersistablePermission(context, destination)
                    val saved = viewModel.registerPickedDocument(destination, persistable)
                    val open = snackbarHost.showSnackbar(
                        message = context.getString(R.string.scan_saved),
                        actionLabel = context.getString(R.string.action_open_now),
                    )
                    if (open == SnackbarResult.ActionPerformed) onOpenDocument(saved.uri)
                }
                .onFailure { snackbarHost.showSnackbar(context.getString(R.string.scan_failed)) }
        }
    }

    // --- Escaner de ML Kit ---
    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (!DocumentScanner.isSuccess(result.resultCode)) return@rememberLauncherForActivityResult
        val pdf = DocumentScanner.pdfFromResult(result.data)
        if (pdf == null) {
            scope.launch { snackbarHost.showSnackbar(context.getString(R.string.scan_failed)) }
            return@rememberLauncherForActivityResult
        }
        pendingScanPdf = pdf
    }

    val startScan = {
        val activity = context as? Activity
        if (activity != null) {
            DocumentScanner.client(context).getStartScanIntent(activity)
                .addOnSuccessListener { sender ->
                    scanLauncher.launch(IntentSenderRequest.Builder(sender).build())
                }
                .addOnFailureListener {
                    scope.launch { snackbarHost.showSnackbar(context.getString(R.string.scan_failed)) }
                }
        }
    }

    // Mensajes con opcion de deshacer
    LaunchedEffect(Unit) {
        viewModel.messageFlow.collect { message ->
            val text = message.arg?.let { context.getString(message.resId, it) }
                ?: context.getString(message.resId)
            val result = snackbarHost.showSnackbar(
                message = text,
                actionLabel = message.undo?.let { context.getString(R.string.undo) },
            )
            if (result == SnackbarResult.ActionPerformed) {
                message.undo?.let(viewModel::undoRemove)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    if (searching) {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            placeholder = { Text(stringResource(R.string.library_search_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.app_title_bar),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        searching = !searching
                        if (!searching) viewModel.onQueryChange("")
                    }) {
                        Icon(Icons.Outlined.Search, stringResource(R.string.cd_search))
                    }
                    // Tema claro/oscuro independiente del celular.
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = stringResource(
                                if (isDarkTheme) R.string.theme_to_light else R.string.theme_to_dark,
                            ),
                        )
                    }
                    IconButton(onClick = { showAbout = true }) {
                        Icon(Icons.Outlined.Info, stringResource(R.string.about))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    titleContentColor = if (isDark) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    actionIconContentColor = if (isDark) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                ),
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                val accent = if (isDark) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                }
                val onAccent = if (isDark) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }
                val fabShape = RoundedCornerShape(18.dp)

                // Escanear: fondo del color de la superficie con borde del azul
                // de marca, para que no compita con el boton principal.
                FloatingActionButton(
                    onClick = startScan,
                    shape = fabShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = accent,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                    ),
                    modifier = Modifier
                        .size(60.dp)
                        .border(2.dp, accent, fabShape),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DocumentScanner,
                        contentDescription = stringResource(R.string.scan_to_pdf),
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                ExtendedFloatingActionButton(
                    onClick = { pickDocument.launch(SafDocuments.openDocumentIntent()) },
                    shape = fabShape,
                    containerColor = accent,
                    contentColor = onAccent,
                    modifier = Modifier.height(60.dp),
                    icon = {
                        Icon(Icons.Outlined.FolderOpen, null, Modifier.size(26.dp))
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.open_pdf),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                )
            }
        },
    ) { padding ->
        when {
            state.groups.isEmpty() && !state.hasAnyDocument -> EmptyLibrary(Modifier.padding(padding))

            state.groups.isEmpty() -> NoResults(state.query, Modifier.padding(padding))

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!searching) {
                    item(key = "titulo-historial") {
                        Text(
                            text = stringResource(R.string.history_title),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        )
                    }
                }
                state.groups.forEach { group ->
                    item(key = "header-${group.title}") {
                        GroupHeader(group.title)
                    }
                    items(group.documents, key = { "${group.title}-${it.uri}" }) { document ->
                        DocumentRow(
                            document = document,
                            onOpen = { onOpenDocument(document.uri) },
                            onRename = {
                                scope.launch {
                                    renamingAllowed = viewModel.canRename(document)
                                    renaming = document
                                }
                            },
                            onShare = { context.shareDocument(document) },
                            onSaveToMisPdf = { viewModel.saveToMisPdf(document) },
                            onToggleFavorite = { viewModel.toggleFavorite(document) },
                            onRemove = { viewModel.removeFromHistory(document) },
                            onDeleteFromDevice = {
                                scope.launch {
                                    deletingAllowed = viewModel.canDelete(document)
                                    deleting = document
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    renaming?.let { document ->
        RenameDialog(
            document = document,
            canRename = renamingAllowed,
            onConfirm = { newName ->
                viewModel.rename(document, newName)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }

    deleting?.let { document ->
        DeleteDialog(
            document = document,
            canDelete = deletingAllowed,
            onConfirm = {
                viewModel.deleteFromDevice(document)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }

    pendingScanPdf?.let { scanned ->
        ScanSaveDialog(
            onSaveToMisPdf = { name ->
                pendingScanPdf = null
                scope.launch {
                    PublicDocuments.save(context, scanned, name)
                        .onSuccess { saved ->
                            viewModel.registerScanned(saved)
                            val open = snackbarHost.showSnackbar(
                                message = context.getString(
                                    R.string.scan_saved_at,
                                    PublicDocuments.displayPath,
                                ),
                                actionLabel = context.getString(R.string.action_open_now),
                            )
                            if (open == SnackbarResult.ActionPerformed) {
                                onOpenDocument(saved.toString())
                            }
                        }
                        .onFailure {
                            snackbarHost.showSnackbar(context.getString(R.string.scan_failed))
                        }
                }
            },
            onChooseFolder = { name ->
                saveScanToChosenFolder.launch(DocumentScanner.createDocumentIntent(name))
            },
            onDismiss = { pendingScanPdf = null },
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun GroupHeader(title: GroupTitle) {
    Text(
        text = when (title) {
            is GroupTitle.Resource -> stringResource(title.resId)
            is GroupTitle.Literal -> title.text
        },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.library_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.library_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NoResults(query: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.library_no_results, query),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Comparte el documento ORIGINAL por su propia URI: no hace falta FileProvider. */
private fun android.content.Context.shareDocument(document: DocumentEntity) {
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, Uri.parse(document.uri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(share, getString(R.string.action_share)))
}
