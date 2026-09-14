package com.giosoft.pdf

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
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import com.giosoft.pdf.data.SafDocuments
import com.giosoft.pdf.data.ThemeMode
import com.giosoft.pdf.ui.AppNavigation
import com.giosoft.pdf.ui.theme.LectorPdfTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingDocument by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIncomingIntent(intent)

        val settings = (application as LectorPdfApp).container.settingsRepository

        setContent {
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.LIGHT)
            val systemDark = isSystemInDarkTheme()

            LectorPdfTheme(themeMode = themeMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        initialDocumentUri = pendingDocument,
                        onInitialDocumentConsumed = { pendingDocument = null },
                        isDarkTheme = when (themeMode) {
                            ThemeMode.SYSTEM -> systemDark
                            ThemeMode.LIGHT -> false
                            ThemeMode.DARK -> true
                        },
                        onToggleTheme = { currentlyDark ->
                            lifecycleScope.launch {
                                // Al primer toque se fija una eleccion explicita,
                                // que ya no depende del tema del celular.
                                settings.setThemeMode(
                                    if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK,
                                )
                            }
                        },
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
            val document = (application as LectorPdfApp).container.documentRepository
                .registerOpened(uri, persistable)
            // Puede no ser la URI recibida: si el acceso era temporal, el
            // repositorio ya conservo el documento y devuelve la copia estable.
            pendingDocument = document.uri
        }
    }
}
