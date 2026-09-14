package com.giosoft.lectorpdf.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "PdfEncryption"

/**
 * Pone y quita la contrasena DENTRO del archivo PDF.
 *
 * Esto es distinto del bloqueo con huella: la contrasena viaja con el archivo.
 * Si lo envias por WhatsApp, a quien lo reciba se la pediran igual, la abra con
 * la app que la abra. El bloqueo con huella, en cambio, solo protege el
 * documento dentro de esta app y en este celular.
 *
 * androidx.pdf sabe LEER documentos cifrados pero no crearlos, por eso se usa
 * PDFBox aqui.
 */
object PdfEncryption {

    /** Longitud de clave AES. 128 bits es lo que admite cualquier lector. */
    private const val KEY_LENGTH = 128

    /** Umbral a partir del cual PDFBox trabaja en disco en vez de en memoria. */
    private const val MEMORY_LIMIT_BYTES = 8L * 1024 * 1024

    private var initialized = false

    private fun ensureInitialized(context: Context) {
        if (!initialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            initialized = true
        }
    }

    /**
     * Escribe en [destination] una copia de [source] protegida con [password].
     *
     * [currentPassword] es la contrasena actual del documento, si ya tenia una.
     *
     * Se cifra sobre un archivo temporal y solo al final se vuelca al destino:
     * si algo falla a mitad, el archivo del usuario queda intacto en vez de
     * quedarse a medio escribir.
     */
    suspend fun protect(
        context: Context,
        source: Uri,
        destination: Uri,
        password: String,
        currentPassword: String? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        transform(context, source, destination, currentPassword) { document ->
            val permisos = AccessPermission()
            val politica = StandardProtectionPolicy(password, password, permisos)
            politica.encryptionKeyLength = KEY_LENGTH
            document.protect(politica)
        }
    }

    /** Escribe en [destination] una copia de [source] SIN contrasena. */
    suspend fun removeProtection(
        context: Context,
        source: Uri,
        destination: Uri,
        currentPassword: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        transform(context, source, destination, currentPassword) { document ->
            document.isAllSecurityToBeRemoved = true
        }
    }

    private inline fun transform(
        context: Context,
        source: Uri,
        destination: Uri,
        currentPassword: String?,
        crossinline cambio: (PDDocument) -> Unit,
    ): Result<Unit> = runCatching {
        ensureInitialized(context)

        val temporal = File.createTempFile("cifrado", ".pdf", context.cacheDir)
        try {
            context.contentResolver.openInputStream(source).use { entrada ->
                requireNotNull(entrada) { "No se pudo leer el documento" }
                PDDocument.load(
                    entrada,
                    currentPassword.orEmpty(),
                    MemoryUsageSetting.setupMixed(MEMORY_LIMIT_BYTES),
                ).use { document ->
                    cambio(document)
                    temporal.outputStream().use { document.save(it) }
                }
            }

            // "wt" trunca el archivo antes de escribir; sin eso, un PDF nuevo
            // mas corto dejaria basura del anterior al final.
            context.contentResolver.openOutputStream(destination, "wt").use { salida ->
                requireNotNull(salida) { "No se pudo escribir el documento" }
                temporal.inputStream().use { it.copyTo(salida) }
            }
        } finally {
            temporal.delete()
        }
        Unit
    }.onFailure { Log.e(TAG, "Fallo al cambiar la proteccion del documento", it) }
}
