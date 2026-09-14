package com.giosoft.lectorpdf.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ajustes")

/** Tema elegido por el usuario, independiente del tema del celular. */
enum class ThemeMode {
    /** Sigue al tema del celular. Ya no es el valor inicial, pero se conserva
     *  por si el usuario lo eligio antes de que el claro pasara a ser el
     *  predeterminado. */
    SYSTEM,
    LIGHT,
    DARK,
}

class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val thumbnailsKey = booleanPreferencesKey("show_thumbnails")
    private val categoriesSeededKey = booleanPreferencesKey("categories_seeded")
    private val favoritesPositionKey = intPreferencesKey("favorites_position")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        preferences[themeKey]
            ?.let { stored -> runCatching { ThemeMode.valueOf(stored) }.getOrNull() }
            ?: ThemeMode.LIGHT
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    /**
     * Miniatura de la primera pagina en cada tarjeta.
     *
     * Desactivado por defecto: generar la miniatura obliga a abrir cada PDF, y
     * con un historial largo eso encarece el arranque. El usuario lo activa si
     * lo prefiere.
     */
    val showThumbnails: Flow<Boolean> =
        context.dataStore.data.map { it[thumbnailsKey] ?: false }

    suspend fun setShowThumbnails(enabled: Boolean) {
        context.dataStore.edit { it[thumbnailsKey] = enabled }
    }

    /**
     * Posicion de la ficha "Favoritos" dentro de la fila de filtros.
     *
     * Es un ajuste y no una fila mas en la tabla de categorias porque Favoritos
     * no es una categoria: cruza a todas (un documento puede estar en Trabajo
     * y ademas ser favorito).
     */
    val favoritesPosition: Flow<Int> =
        context.dataStore.data.map { it[favoritesPositionKey] ?: 0 }

    suspend fun setFavoritesPosition(position: Int) {
        context.dataStore.edit { it[favoritesPositionKey] = position.coerceAtLeast(0) }
    }

    suspend fun areCategoriesSeeded(): Boolean =
        context.dataStore.data.first()[categoriesSeededKey] ?: false

    suspend fun markCategoriesSeeded() {
        context.dataStore.edit { it[categoriesSeededKey] = true }
    }
}
