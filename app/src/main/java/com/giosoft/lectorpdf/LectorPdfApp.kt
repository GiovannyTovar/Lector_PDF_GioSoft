package com.giosoft.lectorpdf

import android.app.Application
import androidx.pdf.PdfLoader
import androidx.pdf.SandboxedPdfLoader
import com.giosoft.lectorpdf.data.DocumentRepository
import com.giosoft.lectorpdf.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers

class LectorPdfApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Inyeccion de dependencias a mano. La app tiene pocas piezas y anadir Hilt
 * solo sumaria procesamiento de anotaciones y tiempo de compilacion.
 */
class AppContainer(app: Application) {

    val documentRepository: DocumentRepository by lazy {
        DocumentRepository(app, AppDatabase.get(app).documentDao())
    }

    /**
     * Abre y parsea los PDF en un **proceso aislado**: un documento malformado
     * o malicioso no puede afectar al proceso de la app.
     */
    val pdfLoader: PdfLoader by lazy {
        SandboxedPdfLoader(app, Dispatchers.IO)
    }
}
