package com.giosoft.pdf.scan

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "DocumentScanner"

/**
 * Escaneo de documentos con ML Kit.
 *
 * Google entrega la interfaz completa de captura (deteccion de bordes,
 * recorte, filtros, varias paginas, reordenar) y devuelve **el PDF ya armado**,
 * asi que la app no necesita procesar imagenes ni pedir permiso de camara: el
 * escaneo ocurre en una actividad de los servicios de Google Play.
 */
object DocumentScanner {

    private const val MAX_PAGES = 50

    val options: GmsDocumentScannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setPageLimit(MAX_PAGES)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()

    fun client(context: Context) = GmsDocumentScanning.getClient(options)

    /** Extrae la URI temporal del PDF generado, en la cache de la app. */
    fun pdfFromResult(data: android.content.Intent?): Uri? =
        GmsDocumentScanningResult.fromActivityResultIntent(data)?.pdf?.uri


    /**
     * Copia el PDF temporal al destino que el usuario eligio con el selector.
     * Es la unica copia que hace la app, y solo porque el escaner produce un
     * archivo nuevo que todavia no tiene sitio en el almacenamiento.
     */
    suspend fun saveTo(context: Context, source: Uri, destination: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(source).use { input ->
                    requireNotNull(input) { "No se pudo leer el PDF escaneado" }
                    context.contentResolver.openOutputStream(destination).use { output ->
                        requireNotNull(output) { "No se pudo escribir en el destino" }
                        input.copyTo(output)
                    }
                }
                Unit
            }.onFailure { Log.e(TAG, "Fallo al guardar el escaneo", it) }
        }

    fun createDocumentIntent(name: String): android.content.Intent =
        android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_TITLE, name)
        }

    fun isSuccess(resultCode: Int) = resultCode == Activity.RESULT_OK
}
