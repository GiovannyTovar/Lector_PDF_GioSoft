package com.giosoft.pdf.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
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


    /**
     * `true` si la app puede borrar este archivo del almacenamiento.
     *
     * Vale para lo que la propia app creo (escaneos y copias en "Mis PDF") y
     * para los documentos cuyo proveedor declara que admiten borrado. Un PDF
     * abierto desde Drive, por ejemplo, normalmente no lo permite.
     */
    suspend fun canDelete(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val flags = queryColumn(context, uri, DocumentsContract.Document.COLUMN_FLAGS) { c, i ->
                c.getInt(i)
            } ?: return@withContext false
            return@withContext flags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE != 0
        }
        // URI de MediaStore: la app puede borrar lo que ella misma creo.
        uri.authority == MediaStore.AUTHORITY
    }

    /**
     * Borra el archivo del almacenamiento. **Es irreversible.**
     *
     * Solo debe llamarse tras una confirmacion explicita del usuario.
     */
    suspend fun delete(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val deleted = if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } else {
                context.contentResolver.delete(uri, null, null) > 0
            }
            if (!deleted) error("El proveedor rechazo borrar el documento")
            Unit
        }.onFailure { Log.w(TAG, "No se pudo borrar $uri", it) }
    }

    /**
     * Etiqueta corta de donde vive el archivo ("Descargas", "WhatsApp",
     * "Drive"...), para poder distinguir dos documentos con el mismo nombre
     * guardados en sitios distintos.
     *
     * Es orientativa: cada proveedor expone la informacion de forma distinta y
     * algunos no la exponen en absoluto, asi que se va degradando desde la ruta
     * real hasta el nombre de la app de origen.
     */
    suspend fun locationLabel(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            // 1. MediaStore expone la carpeta relativa directamente.
            queryColumn(context, uri, MediaStore.MediaColumns.RELATIVE_PATH) { c, i ->
                c.getString(i)
            }?.trim('/')?.takeIf { it.isNotBlank() }?.let { return@withContext prettifyPath(it) }

            // 2. El proveedor de documentos de MediaStore no expone la ruta,
            //    pero su id ES el _ID de MediaStore, que si la conoce.
            mediaStoreRelativePath(context, uri)?.let { return@withContext prettifyPath(it) }

            // 3. Los document URI suelen traer "primary:Download/archivo.pdf".
            if (DocumentsContract.isDocumentUri(context, uri)) {
                runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
                    ?.substringAfter(':', "")
                    ?.substringBeforeLast('/', "")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return@withContext prettifyPath(it) }
            }

            // 4. Algunos proveedores dejan la ruta en el resumen.
            queryColumn(context, uri, DocumentsContract.Document.COLUMN_SUMMARY) { c, i ->
                c.getString(i)
            }?.takeIf { it.isNotBlank() }?.let { return@withContext prettifyPath(it) }

            // 5. Ultimo recurso: la app propietaria del proveedor.
            authorityLabel(uri.authority)
        }

    private const val MEDIA_DOCUMENTS = "com.android.providers.media.documents"

    /**
     * Ruta de un documento servido por el proveedor de MediaStore.
     *
     * Su id tiene la forma "document:1000108702", donde el numero es el _ID de
     * MediaStore. Consultarlo devuelve la carpeta real ("Download/",
     * "Android/media/com.whatsapp/.../WhatsApp Documents/"...).
     *
     * Puede devolver null: sin permisos de almacenamiento, MediaStore solo
     * responde por los archivos que creo la propia app. No es un error, solo
     * se pasa al siguiente intento.
     */
    private fun mediaStoreRelativePath(context: Context, uri: Uri): String? {
        if (uri.authority != MEDIA_DOCUMENTS) return null
        val id = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            ?.substringAfterLast(':')
            ?.toLongOrNull()
            ?: return null

        // Primero, la API pensada para esto: convierte el document URI en su
        // equivalente de MediaStore arrastrando el permiso que ya tenemos.
        runCatching { MediaStore.getMediaUri(context, uri) }
            .getOrNull()
            ?.let { mediaUri ->
                queryColumn(context, mediaUri, MediaStore.MediaColumns.RELATIVE_PATH) { c, i ->
                    c.getString(i)
                }?.takeIf { it.isNotBlank() }?.let { return it }
            }

        // Si no, consulta directa por _ID. Sin permisos de almacenamiento
        // MediaStore solo responde por los archivos de la propia app, asi que
        // esto puede devolver null sin que sea un error.
        return try {
            context.contentResolver.query(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
                "${MediaStore.MediaColumns._ID} = ?",
                arrayOf(id.toString()),
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            Log.d(TAG, "MediaStore no respondio por el id $id: ${e.message}")
            null
        }
    }

    /** Traduce las carpetas mas comunes y se queda con el tramo final. */
    private fun prettifyPath(path: String): String {
        val clean = path.trim('/')
        val translated = when {
            clean.equals("Download", true) || clean.equals("Downloads", true) -> "Descargas"
            clean.equals("Documents", true) -> "Documentos"
            clean.equals("DCIM", true) -> "Camara"
            clean.equals("Pictures", true) -> "Imagenes"
            else -> null
        }
        if (translated != null) return translated

        // "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents" -> "WhatsApp Documents"
        // "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents" -> "WhatsApp"
        if (clean.contains("com.whatsapp", ignoreCase = true)) return "WhatsApp"
        if (clean.contains("org.telegram", ignoreCase = true)) return "Telegram"

        val last = clean.substringAfterLast('/')
        return last.ifBlank { clean }
    }

    /**
     * Nombre de la app de origen. Solo se usa si los intentos anteriores no
     * dieron con la carpeta real.
     *
     * Devuelve null para los proveedores genericos de almacenamiento: una
     * etiqueta como "Archivos del celular" ocupa sitio sin distinguir nada,
     * y el tamano y las paginas ya diferencian dos archivos homonimos.
     */
    private fun authorityLabel(authority: String?): String? = when {
        authority == null -> null
        authority.contains("whatsapp", true) -> "WhatsApp"
        authority.contains("telegram", true) -> "Telegram"
        authority.contains("apps.docs", true) -> "Drive"
        authority.contains("gm.sapi", true) || authority.contains("gmail", true) -> "Gmail"
        authority.contains("dropbox", true) -> "Dropbox"
        authority.contains("downloads", true) -> "Descargas"
        else -> null
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
