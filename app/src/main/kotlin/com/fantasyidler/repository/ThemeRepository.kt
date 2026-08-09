package com.fantasyidler.repository

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.fantasyidler.data.db.dao.CustomThemeDao
import com.fantasyidler.data.json.ColourSchemeParameter
import com.fantasyidler.data.json.ColourSchemeParameter.*
import com.fantasyidler.data.json.ThemeData
import com.fantasyidler.data.model.CustomTheme
import com.fantasyidler.data.model.ThemeBase
import com.fantasyidler.util.toTitleCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val customThemeDao: CustomThemeDao,
    private val gameData: GameDataRepository,
    private val json: Json,
) {
    fun getOfficialThemes(): List<String> =
        gameData.officialThemes.keys.toList() + listOf("system")

    fun observeCustomThemes(): Flow<List<CustomTheme>> = customThemeDao.observeAllThemes()

    /**
     * Resolves [theme] to a Material 3 [ColorScheme], preferring official asset themes
     * and falling back to a user-defined custom theme from the database.
     *
     * `"system"` follows the device night-mode setting and resolves to `"dark"` or `"light"`.
     */
    suspend fun getColourScheme(theme: String): ColorScheme {
        val themeKey = if (theme == "system") systemThemeKey() else theme
        gameData.officialThemes[themeKey]?.let { official ->
            return buildColourScheme(official.base, official.colours, official.schemes)
        }
        val custom = customThemeDao.getTheme(themeKey)
        if (custom != null) {
            val colours = json.decodeFromString<Map<String, String>>(custom.colours)
            val schemes = json.decodeFromString<Map<ColourSchemeParameter, String>>(custom.scheme)
            return buildColourScheme(custom.base, colours, schemes)
        }
        // If the theme is not "dark", fall back to "dark"
        if (themeKey != "dark") {
            return getColourScheme("dark")
        }
        // If the dark theme is not available, use the default Material 3 dark theme
        return darkColorScheme()
    }

    private fun systemThemeKey(): String {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (night == Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
    }

    /**
     * Imports one or more themes from a JSON string in the same shape as
     * `assets/data/official_themes.json` (`Map` of theme key → theme data).
     * Existing custom themes with the same name are replaced.
     * Names that clash with [getOfficialThemes] are rejected.
     */
    suspend fun importTheme(jsonString: String) {
        val themes = json.decodeFromString<Map<String, ThemeData>>(jsonString)
        require(themes.isNotEmpty()) { "Theme import contained no themes" }
        val officialNames = getOfficialThemes().toSet()
        val reserved = themes.keys.filter { it in officialNames }
        require(reserved.isEmpty()) {
            "Cannot import theme(s) that clash with official names: ${reserved.joinToString()}"
        }
        for ((name, data) in themes) {
            customThemeDao.upsert(
                CustomTheme(
                    name = name,
                    displayName = data.displayName.ifBlank { name.replace("_", " ").toTitleCase() },
                    base = data.base,
                    colours = json.encodeToString(data.colours),
                    scheme = json.encodeToString(data.schemes),
                )
            )
        }
    }

    /** Deletes a custom theme. Returns false if [name] is official or reserved. */
    suspend fun deleteTheme(name: String): Boolean {
        if (name in getOfficialThemes()) return false
        customThemeDao.delete(name)
        return true
    }

    private fun buildColourScheme(
        base: ThemeBase,
        colours: Map<String, String>,
        schemes: Map<ColourSchemeParameter, String>,
    ): ColorScheme {
        fun resolve(param: ColourSchemeParameter): Color? {
            val colourName = schemes[param] ?: return null
            val raw = colours[colourName] ?: return null
            return try { Color(parseArgb(raw)) } catch (_: Exception) { null }
        }

        val fallback = when (base) {
            ThemeBase.LIGHT -> lightColorScheme()
            ThemeBase.DARK -> darkColorScheme()
        }

        return fallback.copy(
            primary = resolve(PRIMARY) ?: fallback.primary,
            onPrimary = resolve(ON_PRIMARY) ?: fallback.onPrimary,
            primaryContainer = resolve(PRIMARY_CONTAINER) ?: fallback.primaryContainer,
            onPrimaryContainer = resolve(ON_PRIMARY_CONTAINER) ?: fallback.onPrimaryContainer,
            inversePrimary = resolve(INVERSE_PRIMARY) ?: fallback.inversePrimary,
            secondary = resolve(SECONDARY) ?: fallback.secondary,
            onSecondary = resolve(ON_SECONDARY) ?: fallback.onSecondary,
            secondaryContainer = resolve(SECONDARY_CONTAINER) ?: fallback.secondaryContainer,
            onSecondaryContainer = resolve(ON_SECONDARY_CONTAINER) ?: fallback.onSecondaryContainer,
            tertiary = resolve(TERTIARY) ?: fallback.tertiary,
            onTertiary = resolve(ON_TERTIARY) ?: fallback.onTertiary,
            tertiaryContainer = resolve(TERTIARY_CONTAINER) ?: fallback.tertiaryContainer,
            onTertiaryContainer = resolve(ON_TERTIARY_CONTAINER) ?: fallback.onTertiaryContainer,
            background = resolve(BACKGROUND) ?: fallback.background,
            onBackground = resolve(ON_BACKGROUND) ?: fallback.onBackground,
            surface = resolve(SURFACE) ?: fallback.surface,
            onSurface = resolve(ON_SURFACE) ?: fallback.onSurface,
            surfaceVariant = resolve(SURFACE_VARIANT) ?: fallback.surfaceVariant,
            onSurfaceVariant = resolve(ON_SURFACE_VARIANT) ?: fallback.onSurfaceVariant,
            surfaceTint = resolve(SURFACE_TINT) ?: fallback.surfaceTint,
            inverseSurface = resolve(INVERSE_SURFACE) ?: fallback.inverseSurface,
            inverseOnSurface = resolve(INVERSE_ON_SURFACE) ?: fallback.inverseOnSurface,
            error = resolve(ERROR) ?: fallback.error,
            onError = resolve(ON_ERROR) ?: fallback.onError,
            errorContainer = resolve(ERROR_CONTAINER) ?: fallback.errorContainer,
            onErrorContainer = resolve(ON_ERROR_CONTAINER) ?: fallback.onErrorContainer,
            outline = resolve(OUTLINE) ?: fallback.outline,
            outlineVariant = resolve(OUTLINE_VARIANT) ?: fallback.outlineVariant,
            scrim = resolve(SCRIM) ?: fallback.scrim,
            surfaceBright = resolve(SURFACE_BRIGHT) ?: fallback.surfaceBright,
            surfaceDim = resolve(SURFACE_DIM) ?: fallback.surfaceDim,
            surfaceContainer = resolve(SURFACE_CONTAINER) ?: fallback.surfaceContainer,
            surfaceContainerHigh = resolve(SURFACE_CONTAINER_HIGH) ?: fallback.surfaceContainerHigh,
            surfaceContainerHighest = resolve(SURFACE_CONTAINER_HIGHEST) ?: fallback.surfaceContainerHighest,
            surfaceContainerLow = resolve(SURFACE_CONTAINER_LOW) ?: fallback.surfaceContainerLow,
            surfaceContainerLowest = resolve(SURFACE_CONTAINER_LOWEST) ?: fallback.surfaceContainerLowest,
        )
    }

    private fun parseArgb(raw: String): Long {
        val cleaned = raw.trim()
            .removePrefix("0x")
            .removePrefix("0X")
            .removePrefix("#")
        return cleaned.toULong(16).toLong()
    }
}
