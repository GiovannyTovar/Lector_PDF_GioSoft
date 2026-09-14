package com.giosoft.pdf.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una categoria creada por el usuario ("Trabajo", "Recibos"...).
 *
 * No se declara clave foranea hacia [DocumentEntity] a proposito: SQLite no
 * permite anadir una a una tabla existente sin recrearla, y el comportamiento
 * que queremos (al borrar la categoria, los documentos quedan sin categoria y
 * NO se borran) se resuelve en el repositorio, donde ademas queda explicito.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Color con el que se pinta el chip, en formato ARGB. */
    val colorArgb: Int,
    /** Orden en el que aparece en la fila de filtros. */
    val position: Int,
)
