package com.giosoft.lectorpdf.ui.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.compose.PdfViewer
import androidx.pdf.compose.rememberPdfViewerState
import android.content.Intent
import android.net.Uri
import com.giosoft.lectorpdf.R
import com.giosoft.lectorpdf.print.PdfPrinter
import com.giosoft.lectorpdf.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    onBack: () -> Unit,
    viewModel: ViewerViewModel = viewModel(factory = ViewerViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = (state as? ViewerUiState.Ready)?.entity?.name
                                ?: stringResource(R.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    (state as? ViewerUiState.Ready)?.entity?.let { document ->
                        IconButton(onClick = {
                            PdfPrinter.print(context, Uri.parse(document.uri), document.name)
                        }) {
                            Icon(Icons.Default.Print, stringResource(R.string.action_print))
                        }
                        IconButton(onClick = {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(document.uri))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(share, context.getString(R.string.action_share)),
                            )
                        }) {
                            Icon(Icons.Default.Share, stringResource(R.string.action_share))
                        }
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
                    navigationIconContentColor = if (isDark) {
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
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                is ViewerUiState.Loading -> LoadingContent()

                is ViewerUiState.PasswordRequired -> {
                    // El fondo queda vacio: no hay nada que mostrar hasta descifrar.
                    PasswordDialog(
                        state = current,
                        onSubmit = viewModel::submitPassword,
                        onDismiss = onBack,
                    )
                }

                is ViewerUiState.Ready -> DocumentContent(
                    state = current,
                    onPageChanged = viewModel::onPageChanged,
                )

                is ViewerUiState.Failed -> FailureContent(
                    reason = current.reason,
                    onRetry = viewModel::retry,
                    onBack = onBack,
                )
            }
        }
    }
}

@OptIn(ExperimentalPdfApi::class)
@Composable
private fun DocumentContent(
    state: ViewerUiState.Ready,
    onPageChanged: (Int) -> Unit,
) {
    val pdfState = rememberPdfViewerState()

    // Reanudar donde se quedo la lectura.
    LaunchedEffect(state.document, state.initialPage) {
        if (state.initialPage > 0) {
            runCatching { pdfState.scrollToPage(state.initialPage) }
        }
    }

    // Guardar la pagina visible para la proxima vez que se abra.
    LaunchedEffect(pdfState) {
        snapshotFlow { pdfState.firstVisiblePage }
            .distinctUntilChanged()
            .collect(onPageChanged)
    }

    Box(Modifier.fillMaxSize()) {
        // Desplazamiento VERTICAL y continuo entre paginas, que es el
        // comportamiento por defecto de androidx.pdf. La version 4.x con MuPDF
        // solo permitia pasar paginas en horizontal.
        // El contenido se renderiza tal cual viene en el PDF: no se altera
        // ningun color ni disposicion.
        PdfViewer(
            state.document,
            pdfState,
            Modifier.fillMaxSize(),
        )

        if (state.entity.pageCount > 0) {
            PageIndicator(
                page = pdfState.firstVisiblePage + 1,
                total = state.entity.pageCount,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun PageIndicator(page: Int, total: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
    ) {
        Text(
            text = stringResource(R.string.viewer_page_of, page, total),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.viewer_loading), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FailureContent(
    reason: ViewerUiState.Failed.Reason,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val message = when (reason) {
        ViewerUiState.Failed.Reason.MISSING -> R.string.viewer_error_missing
        ViewerUiState.Failed.Reason.PASSWORD_UNSUPPORTED -> R.string.password_unsupported
        ViewerUiState.Failed.Reason.GENERIC -> R.string.viewer_error_generic
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.viewer_error_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (reason == ViewerUiState.Failed.Reason.GENERIC) {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
            TextButton(onClick = onBack) { Text(stringResource(R.string.cd_back)) }
        }
    }
}
