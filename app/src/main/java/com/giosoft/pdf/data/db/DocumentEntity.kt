package com.giosoft.pdf.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un documento del historial.
 *
 * La clave primaria es la **URI del archivo original** en el celular, no una
 * ruta a una copia: la version 4.x copiaba cada PDF a la carpeta privada de la
 * app, y por eso renombrar afectaba a la copia y no al original.
 *
 * [persistable] indica si conservamos permiso duradero sobre la URI. Los PDF
 * que llegan compartidos desde WhatsApp o el correo entregan una URI temporal
 * que caduca; esos se guardan con `persistable = false` y la interfaz avisa de
 * que puede dejar de estar disponible.
 */
@Entity(
    tableName = "documents",
    indices = [Index("contentHash"), Index("categoryId")],
)
data class DocumentEntity(
    @PrimaryKey val uri: String,
    val name: String,
    val lastOpened: Long,
    val lastPage: Int = 0,
    val pageCount: Int = 0,
    val sizeBytes: Long = 0L,
    val isFavorite: Boolean = false,
    val persistable: Boolean = true,
    /**
     * Carpeta legible ("Descargas", "WhatsApp"...). Es lo que permite
     * distinguir dos documentos con el MISMO nombre en ubicaciones distintas,
     * ya que la clave real es la URI y no el nombre.
     */
    val location: String? = null,
    /**
     * Huella SHA-256 del contenido. Identifica el documento con independencia
     * del nombre y de la URI, para no volver a copiar uno que ya conservamos.
     */
    val contentHash: String? = null,
    /** Categoria a la que el usuario lo asigno. null = sin categoria. */
    val categoryId: Long? = null,
    /**
     * Exige desbloquear el celular para abrirlo. Tampoco genera miniatura, o
     * la lista mostraria justo el contenido que se quiso proteger.
     */
    val isLocked: Boolean = false,
    /**
     * El archivo PDF lleva contrasena dentro. A diferencia de [isLocked], esto
     * viaja con el archivo: se la pediran a quien lo reciba, en cualquier app.
     */
    val hasPassword: Boolean = false,
)
