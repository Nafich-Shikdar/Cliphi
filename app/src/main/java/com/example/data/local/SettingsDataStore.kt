package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import com.example.model.DurationPreset
import com.example.model.ExportQuality
import com.example.model.LanguageOption
import com.example.model.SubtitleFontFamily
import com.example.model.SubtitlePosition
import com.example.model.SubtitleStyleConfig
import com.example.model.SubtitleTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clipforge_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val AI_MODEL = stringPreferencesKey("ai_model_name")
        val DEFAULT_LANGUAGE = stringPreferencesKey("default_language")
        val EXPORT_QUALITY = stringPreferencesKey("export_quality")
        val STORAGE_LABEL = stringPreferencesKey("storage_label")
        val DEFAULT_DURATION = stringPreferencesKey("default_duration_preset")
        val DEFAULT_CLIP_COUNT = intPreferencesKey("default_clip_count")

        val SUB_TEMPLATE = stringPreferencesKey("sub_template")
        val SUB_FONT = stringPreferencesKey("sub_font")
        val SUB_FONT_SIZE = floatPreferencesKey("sub_font_size")
        val SUB_TEXT_COLOR = longPreferencesKey("sub_text_color")
        val SUB_BG_COLOR = longPreferencesKey("sub_bg_color")
        val SUB_OUTLINE_COLOR = longPreferencesKey("sub_outline_color")
        val SUB_HIGHLIGHT_COLOR = longPreferencesKey("sub_highlight_color")
        val SUB_POSITION = stringPreferencesKey("sub_position")
        val SUB_BURN_IN = booleanPreferencesKey("sub_burn_in")
    }

    val appSettingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            aiModelName = prefs[Keys.AI_MODEL] ?: "gemini-3.8-flash",
            defaultLanguage = prefs[Keys.DEFAULT_LANGUAGE]?.let {
                runCatching { LanguageOption.valueOf(it) }.getOrNull()
            } ?: LanguageOption.AUTO_DETECT,
            exportQuality = prefs[Keys.EXPORT_QUALITY]?.let {
                runCatching { ExportQuality.valueOf(it) }.getOrNull()
            } ?: ExportQuality.FHD_1080X1920,
            storageLocationLabel = prefs[Keys.STORAGE_LABEL] ?: "Downloads/ClipForge & App Workspace",
            defaultDurationPreset = prefs[Keys.DEFAULT_DURATION]?.let {
                runCatching { DurationPreset.valueOf(it) }.getOrNull()
            } ?: DurationPreset.MEDIUM_30_45,
            defaultClipCount = prefs[Keys.DEFAULT_CLIP_COUNT] ?: 5
        )
    }

    val subtitleStyleFlow: Flow<SubtitleStyleConfig> = context.dataStore.data.map { prefs ->
        val defaultTemplate = SubtitleTemplate.PREMIUM_PURPLE
        SubtitleStyleConfig(
            template = prefs[Keys.SUB_TEMPLATE]?.let {
                runCatching { SubtitleTemplate.valueOf(it) }.getOrNull()
            } ?: defaultTemplate,
            fontFamily = prefs[Keys.SUB_FONT]?.let {
                runCatching { SubtitleFontFamily.valueOf(it) }.getOrNull()
            } ?: defaultTemplate.defaultFont,
            fontSizeSp = prefs[Keys.SUB_FONT_SIZE] ?: defaultTemplate.defaultFontSizeSp,
            textColorHex = prefs[Keys.SUB_TEXT_COLOR] ?: defaultTemplate.defaultTextHex,
            backgroundColorHex = prefs[Keys.SUB_BG_COLOR] ?: defaultTemplate.defaultBgHex,
            outlineColorHex = prefs[Keys.SUB_OUTLINE_COLOR] ?: defaultTemplate.defaultOutlineHex,
            highlightColorHex = prefs[Keys.SUB_HIGHLIGHT_COLOR] ?: defaultTemplate.defaultHighlightHex,
            position = prefs[Keys.SUB_POSITION]?.let {
                runCatching { SubtitlePosition.valueOf(it) }.getOrNull()
            } ?: defaultTemplate.defaultPosition,
            burnInSubtitles = prefs[Keys.SUB_BURN_IN] ?: true
        )
    }

    suspend fun updateAppSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AI_MODEL] = settings.aiModelName
            prefs[Keys.DEFAULT_LANGUAGE] = settings.defaultLanguage.name
            prefs[Keys.EXPORT_QUALITY] = settings.exportQuality.name
            prefs[Keys.STORAGE_LABEL] = settings.storageLocationLabel
            prefs[Keys.DEFAULT_DURATION] = settings.defaultDurationPreset.name
            prefs[Keys.DEFAULT_CLIP_COUNT] = settings.defaultClipCount
        }
    }

    suspend fun updateSubtitleStyle(style: SubtitleStyleConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SUB_TEMPLATE] = style.template.name
            prefs[Keys.SUB_FONT] = style.fontFamily.name
            prefs[Keys.SUB_FONT_SIZE] = style.fontSizeSp
            prefs[Keys.SUB_TEXT_COLOR] = style.textColorHex
            prefs[Keys.SUB_BG_COLOR] = style.backgroundColorHex
            prefs[Keys.SUB_OUTLINE_COLOR] = style.outlineColorHex
            prefs[Keys.SUB_HIGHLIGHT_COLOR] = style.highlightColorHex
            prefs[Keys.SUB_POSITION] = style.position.name
            prefs[Keys.SUB_BURN_IN] = style.burnInSubtitles
        }
    }
}
