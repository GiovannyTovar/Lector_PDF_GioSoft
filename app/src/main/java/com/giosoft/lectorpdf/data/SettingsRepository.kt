package com.giosoft.lectorpdf.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        preferences[themeKey]
            ?.let { stored -> runCatching { ThemeMode.valueOf(stored) }.getOrNull() }
            ?: ThemeMode.LIGHT
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }
}
