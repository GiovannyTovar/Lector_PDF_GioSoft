package com.giosoft.lectorpdf.data

import com.giosoft.lectorpdf.data.db.CategoryDao
import com.giosoft.lectorpdf.data.db.CategoryEntity
import com.giosoft.lectorpdf.data.db.DocumentDao
import com.giosoft.lectorpdf.data.db.DocumentEntity
import kotlinx.coroutines.flow.Flow

/** Paleta de la que se toma el color al crear una categoria. */
val CATEGORY_COLORS = listOf(
    0xFF2196F3.toInt(), // azul
    0xFF43A047.toInt(), // verde
    0xFFF4511E.toInt(), // naranja
    0xFF8E24AA.toInt(), // morado
    0xFF00897B.toInt(), // turquesa
    0xFFD81B60.toInt(), // rosa
    0xFF6D4C41.toInt(), // marron
    0xFF546E7A.toInt(), // gris azulado
)

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val documentDao: DocumentDao,
) {

    fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    /**
     * Crea las categorias sugeridas la primera vez.
     *
     * Se controla con una marca en los ajustes y no con "¿esta vacia la tabla?":
     * si el usuario borra todas sus categorias a proposito, no deben volver a
     * aparecer solas en el siguiente arranque.
     */
    suspend fun seedDefaultsIfNeeded(settings: SettingsRepository) {
        if (settings.areCategoriesSeeded()) return
        if (categoryDao.count() == 0) {
            listOf("Personal", "Trabajo", "Recibos", "Estudio").forEachIndexed { index, name ->
                categoryDao.insert(
                    CategoryEntity(
                        name = name,
                        colorArgb = CATEGORY_COLORS[index % CATEGORY_COLORS.size],
                        position = index,
                    ),
                )
            }
        }
        settings.markCategoriesSeeded()
    }

    suspend fun create(name: String, colorArgb: Int? = null): Result<Unit> = runCatching {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "El nombre no puede estar vacio" }
        val position = (categoryDao.maxPosition() ?: -1) + 1
        categoryDao.insert(
            CategoryEntity(
                name = clean,
                colorArgb = colorArgb ?: CATEGORY_COLORS[position % CATEGORY_COLORS.size],
                position = position,
            ),
        )
        Unit
    }

    /**
     * Sube o baja una categoria en la fila de filtros.
     *
     * Intercambia la posicion con su vecina y reescribe ambas, para que el
     * orden quede siempre consecutivo aunque se hayan borrado categorias.
     */
    suspend fun move(categories: List<CategoryEntity>, from: Int, up: Boolean) {
        val to = if (up) from - 1 else from + 1
        if (from !in categories.indices || to !in categories.indices) return
        val reordered = categories.toMutableList()
        val moved = reordered.removeAt(from)
        reordered.add(to, moved)
        reordered.forEachIndexed { index, category ->
            if (category.position != index) {
                categoryDao.update(category.copy(position = index))
            }
        }
    }

    suspend fun rename(category: CategoryEntity, newName: String): Result<Unit> = runCatching {
        val clean = newName.trim()
        require(clean.isNotEmpty()) { "El nombre no puede estar vacio" }
        categoryDao.update(category.copy(name = clean))
    }

    suspend fun setColor(category: CategoryEntity, colorArgb: Int) =
        categoryDao.update(category.copy(colorArgb = colorArgb))

    /**
     * Borra la categoria. Los documentos que la tenian quedan **sin categoria**,
     * nunca se borran: la categoria es una etiqueta, no una carpeta real.
     */
    suspend fun delete(category: CategoryEntity) {
        categoryDao.clearCategoryFromDocuments(category.id)
        categoryDao.delete(category)
    }

    suspend fun assign(document: DocumentEntity, categoryId: Long?) =
        documentDao.updateCategory(document.uri, categoryId)

    suspend fun assignAll(documents: Collection<DocumentEntity>, categoryId: Long?) =
        documentDao.updateCategoryForAll(documents.map { it.uri }, categoryId)
}
