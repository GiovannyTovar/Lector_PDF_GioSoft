package com.giosoft.pdf.data

import com.giosoft.pdf.data.db.CategoryDao
import com.giosoft.pdf.data.db.CategoryEntity
import com.giosoft.pdf.data.db.DocumentDao
import com.giosoft.pdf.data.db.DocumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Paleta para las categorias.
 *
 * Tonos de saturacion parecida para que ninguno destaque sobre los demas, y
 * suficientes para que el usuario no tenga que repetir color.
 */
val CATEGORY_COLORS = listOf(
    0xFF1E88E5.toInt(), // azul
    0xFF3949AB.toInt(), // indigo
    0xFF5E35B1.toInt(), // violeta
    0xFF8E24AA.toInt(), // morado
    0xFFD81B60.toInt(), // fucsia
    0xFFE53935.toInt(), // rojo
    0xFFF4511E.toInt(), // naranja intenso
    0xFFFB8C00.toInt(), // naranja
    0xFFFFB300.toInt(), // ambar
    0xFFFDD835.toInt(), // amarillo
    0xFFC0CA33.toInt(), // lima
    0xFF7CB342.toInt(), // verde claro
    0xFF43A047.toInt(), // verde
    0xFF00897B.toInt(), // turquesa
    0xFF00ACC1.toInt(), // cian
    0xFF039BE5.toInt(), // celeste
    0xFF6D4C41.toInt(), // cafe
    0xFF757575.toInt(), // gris
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
            DEFAULT_CATEGORIES.forEachIndexed { index, (name, color) ->
                categoryDao.insert(
                    CategoryEntity(name = name, colorArgb = color, position = index),
                )
            }
        }
        settings.markCategoriesSeeded()
    }

    suspend fun create(name: String, colorArgb: Int? = null): Result<Unit> = runCatching {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "El nombre no puede estar vacio" }
        require(categoryDao.findByName(clean) == null) { DUPLICATE }
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
        val existing = categoryDao.findByName(clean)
        require(existing == null || existing.id == category.id) { DUPLICATE }
        categoryDao.update(category.copy(name = clean))
    }

    companion object {
        /** Marca de nombre repetido, para distinguirla de "nombre vacio". */
        const val DUPLICATE = "duplicado"

        /**
         * Categorias con las que arranca la app.
         *
         * Los tres primeros colores siguen el orden de un semaforo (rojo,
         * amarillo, verde), que se reconoce sin pensarlo. El amarillo es un
         * tono limon a proposito, para no confundirse con el dorado de la
         * estrella de favoritos.
         *
         * Es solo el punto de partida: el usuario las renombra, recolorea,
         * reordena o borra a su gusto, y no vuelven a crearse.
         */
        private val DEFAULT_CATEGORIES = listOf(
            "Trabajo" to 0xFFE53935.toInt(),   // rojo
            "Estudio" to 0xFFFDD835.toInt(),   // amarillo
            "Personal" to 0xFF43A047.toInt(),  // verde
            "Facturas" to 0xFF8E24AA.toInt(),  // morado
        )
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
