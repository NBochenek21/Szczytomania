package pl.szczytomania.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Motyw aplikacji z ustawień profilu. SYSTEM – jak w telefonie (domyślnie). */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Wybrany motyw, zapamiętany w telefonie (SharedPreferences) – działa też bez zasięgu i po restarcie. */
object ThemeSettings {
    private const val PREFS = "ustawienia"
    private const val KEY = "motyw"

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun load(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        _mode.value = ThemeMode.entries.find { it.name == saved } ?: ThemeMode.SYSTEM
    }

    fun set(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, mode.name).apply()
        _mode.value = mode
    }
}
