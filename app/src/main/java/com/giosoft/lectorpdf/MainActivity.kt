package com.giosoft.lectorpdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.giosoft.lectorpdf.data.SafDocuments
import com.giosoft.lectorpdf.ui.AppNavigation
import com.giosoft.lectorpdf.ui.theme.LectorPdfTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingDocument by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIncomingIntent(intent)

        setContent {
            LectorPdfTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        initialDocumentUri = pendingDocument,
                        onInitialDocumentConsumed = { pendingDocument = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * PDF que llega desde otra app (WhatsApp, correo, Archivos...).
     *
     * Se intenta conservar el acceso de forma duradera. Cuando la URI viene de
     * un FileProvider ajeno no es persistible: el documento se abre igual con
     * el permiso temporal de este intent, y queda marcado en el historial para
     * que la interfaz avise de que puede dejar de estar disponible.
     *
     * En ningun caso se copia el archivo, que era la causa de que renombrar
     * afectara a una copia y no al original.
     */
    private fun handleIncomingIntent(intent: Intent) {
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
        if (uri == null) return

        lifecycleScope.launch {
            val persistable = SafDocuments.takePersistablePermission(this@MainActivity, uri)
            (application as LectorPdfApp).container.documentRepository
                .registerOpened(uri, persistable)
            pendingDocument = uri.toString()
        }
    }
}
