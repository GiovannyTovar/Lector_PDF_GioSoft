package com.giosoft.pdf

import android.app.Application
import androidx.pdf.PdfLoader
import androidx.pdf.SandboxedPdfLoader
import com.giosoft.pdf.data.CategoryRepository
import com.giosoft.pdf.data.DocumentRepository
import com.giosoft.pdf.data.SettingsRepository
import com.giosoft.pdf.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LectorPdfApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // onCreate() se ejecuta en TODOS los procesos de la app, incluido el
        // proceso aislado donde androidx.pdf parsea los documentos. Ese proceso
        // tiene prohibido abrir bases de datos (SecurityException: "Isolated
        // process not allowed to call getContentProvider"), asi que la siembra
        // solo debe correr en el proceso principal.
        if (getProcessName() == packageName) {
            CoroutineScope(Dispatchers.IO).launch {
                container.categoryRepository.seedDefaultsIfNeeded(container.settingsRepository)
            }
        }
    }
}

/**
 * Inyeccion de dependencias a mano. La app tiene pocas piezas y anadir Hilt
 * solo sumaria procesamiento de anotaciones y tiempo de compilacion.
 */
class AppContainer(app: Application) {

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(app) }

    private val database by lazy { AppDatabase.get(app) }

    val documentRepository: DocumentRepository by lazy {
        DocumentRepository(app, database.documentDao())
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(database.categoryDao(), database.documentDao())
    }

    /**
     * Abre y parsea los PDF en un **proceso aislado**: un documento malformado
     * o malicioso no puede afectar al proceso de la app.
     */
    val pdfLoader: PdfLoader by lazy {
        SandboxedPdfLoader(app, Dispatchers.IO)
    }
}
