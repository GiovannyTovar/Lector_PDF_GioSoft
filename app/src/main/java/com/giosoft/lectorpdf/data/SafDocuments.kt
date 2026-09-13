package com.giosoft.lectorpdf.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "SafDocuments"

/**
 * Acceso a los archivos ORIGINALES del usuario mediante el Storage Access
 * Framework. La app nunca copia el PDF ni guarda rutas de archivo: guarda la
 * URI que devuelve el selector del sistema.
 *
 * Esto es lo que permite renombrar el documento real y no una copia, y lo que
 * evita pedir permisos de almacenamiento (y con ello el formulario de
 * justificacion de MANAGE_EXTERNAL_STORAGE en Google Play).
 */
object SafDocuments {

    /** Intent para elegir un PDF conservando el acceso tras reiniciar. */
    fun openDocumentIntent(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/pdf"
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf"))
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
        )
    }

    /**
     * Intenta conservar el acceso a [uri] de forma duradera.
     *
     * Devuelve `true` si se logro. Las URI que comparten otras apps (WhatsApp,
     * Gmail) suelen venir de un FileProvider propio y NO son persistibles: en
     * ese caso devuelve `false` y el documento solo se podra abrir mientras
     * dure el permiso temporal de este intent.
     */
    fun takePersistablePermission(context: Context, uri: Uri): Boolean {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return false
        return try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            true
        } catch (e: SecurityException) {
            Log.i(TAG, "URI no persistible (${uri.authority}): ${e.message}")
            false
        }
    }

    /** Suelta el permiso duradero; se llama al quitar un documento del historial. */
    fun releasePersistablePermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            // No lo teniamos; no hay nada que soltar.
            Log.d(TAG, "Sin permiso persistente que soltar: ${e.message}")
        }
    }

    fun hasPersistedPermission(context: Context, uri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }

    /** Nombre visible del archivo, tal y como lo muestra el sistema. */
    suspend fun displayName(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        queryColumn(context, uri, OpenableColumns.DISPLAY_NAME) { cursor, index ->
            cursor.getString(index)
        } ?: uri.lastPathSegment
    }

    suspend fun sizeBytes(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        queryColumn(context, uri, OpenableColumns.SIZE) { cursor, index ->
            if (cursor.isNull(index)) null else cursor.getLong(index)
        } ?: 0L
    }

    /**
     * Comprueba que el documento siga siendo accesible.
     * Abrir el descriptor es la unica forma fiable: un documento puede constar
     * en el proveedor y aun asi haber sido borrado.
     */
    suspend fun isAvailable(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (e: SecurityException) {
            false
        } catch (e: java.io.FileNotFoundException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /** `true` si el proveedor declara que este documento se puede renombrar. */
    suspend fun canRename(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        if (!DocumentsContract.isDocumentUri(context, uri)) return@withContext false
        val flags = queryColumn(context, uri, DocumentsContract.Document.COLUMN_FLAGS) { c, i ->
            c.getInt(i)
        } ?: return@withContext false
        flags and DocumentsContract.Document.FLAG_SUPPORTS_RENAME != 0
    }

    /**
     * Renombra el archivo ORIGINAL en el almacenamiento del usuario.
     *
     * Devuelve la URI resultante, que puede ser distinta de la original: algunos
     * proveedores emiten una URI nueva tras renombrar, y como la URI es la clave
     * del historial, el llamante debe migrar la fila.
     */
    suspend fun rename(context: Context, uri: Uri, newName: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val renamed = DocumentsContract.renameDocument(
                    context.contentResolver,
                    uri,
                    newName,
                )
                // Algunos proveedores devuelven null cuando la URI no cambia.
                val result = renamed ?: uri
                // Conservar el acceso duradero sobre la URI nueva.
                if (result != uri) takePersistablePermission(context, result)
                result
            }.onFailure { Log.w(TAG, "Fallo al renombrar ${uri.authority}", it) }
        }

    private inline fun <T> queryColumn(
        context: Context,
        uri: Uri,
        column: String,
        extract: (Cursor, Int) -> T?,
    ): T? = try {
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(column)
            if (index < 0) null else extract(cursor, index)
        }
    } catch (e: Exception) {
        Log.d(TAG, "No se pudo leer '$column' de $uri: ${e.message}")
        null
    }
}
