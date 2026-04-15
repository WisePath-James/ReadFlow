package com.readflow.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.readflow.app.domain.model.ReadingMode
import com.readflow.app.domain.model.ReadingTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "readflow_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Theme settings
    private val themeKey = stringPreferencesKey("theme")
    val themeFlow: Flow<ReadingTheme> = context.dataStore.data.map { prefs ->
        ReadingTheme.valueOf(prefs[themeKey] ?: ReadingTheme.LIGHT.name)
    }

    // User settings
    private val userIdKey = stringPreferencesKey("user_id")
    val userIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[userIdKey]
    }

    // PDF reading defaults
    private val defaultZoomKey = floatPreferencesKey("default_zoom")
    val defaultZoomFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[defaultZoomKey] ?: 1.0f
    }

    private val readingModeKey = stringPreferencesKey("reading_mode")
    val readingModeFlow: Flow<ReadingMode> = context.dataStore.data.map { prefs ->
        ReadingMode.valueOf(prefs[readingModeKey] ?: ReadingMode.CONTINUOUS.name)
    }

    // Handwriting settings (tablet)
    private val defaultPenColorKey = stringPreferencesKey("default_pen_color")
    val defaultPenColorFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[defaultPenColorKey] ?: "#000000"
    }

    private val defaultPenWidthKey = floatPreferencesKey("default_pen_width")
    val defaultPenWidthFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[defaultPenWidthKey] ?: 2.0f
    }

    // First launch
    private val isFirstLaunchKey = booleanPreferencesKey("is_first_launch")
    val isFirstLaunchFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[isFirstLaunchKey] ?: true
    }

    // Last opened document
    private val lastDocumentIdKey = stringPreferencesKey("last_document_id")
    val lastDocumentIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[lastDocumentIdKey]
    }

    suspend fun setTheme(theme: ReadingTheme) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = theme.name
        }
    }

    suspend fun setUserId(userId: String) {
        context.dataStore.edit { prefs ->
            prefs[userIdKey] = userId
        }
    }

    suspend fun setDefaultZoom(zoom: Float) {
        context.dataStore.edit { prefs ->
            prefs[defaultZoomKey] = zoom
        }
    }

    suspend fun setReadingMode(mode: ReadingMode) {
        context.dataStore.edit { prefs ->
            prefs[readingModeKey] = mode.name
        }
    }

    suspend fun setDefaultPenColor(color: String) {
        context.dataStore.edit { prefs ->
            prefs[defaultPenColorKey] = color
        }
    }

    suspend fun setDefaultPenWidth(width: Float) {
        context.dataStore.edit { prefs ->
            prefs[defaultPenWidthKey] = width
        }
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[isFirstLaunchKey] = isFirst
        }
    }

    suspend fun setLastDocumentId(documentId: String) {
        context.dataStore.edit { prefs ->
            prefs[lastDocumentIdKey] = documentId
        }
    }
}
