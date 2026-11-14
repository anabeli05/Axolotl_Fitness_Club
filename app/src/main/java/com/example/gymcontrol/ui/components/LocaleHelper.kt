package com.example.gymcontrol.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.os.ConfigurationCompat
import com.example.gymcontrol.R
import java.util.Locale

@Composable
fun metodoPagoLocalizado(metodo: String): String {
    return when (metodo.trim().lowercase(Locale.ROOT)) {
        "efectivo", "cash" -> stringResource(R.string.payment_cash)
        "tarjeta", "card" -> stringResource(R.string.payment_card)
        "transferencia", "transfer" -> stringResource(R.string.payment_transfer)
        else -> metodo
    }
}

object LocaleHelper {
    private const val PREFS_NAME = "app_lang_prefs"
    private const val KEY_LANG = "selected_language"

    fun getActiveLocale(context: Context): Locale {
        val locales = ConfigurationCompat.getLocales(context.resources.configuration)
        return if (!locales.isEmpty) locales[0] ?: Locale.getDefault() else Locale.getDefault()
    }

    fun toggleLanguage(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentLang = prefs.getString(KEY_LANG, "es") ?: "es"
        val newLang = if (currentLang == "es") "en" else "es"
        prefs.edit().putString(KEY_LANG, newLang).apply()
        applyLanguage(context, newLang)
        val activity = findActivity(context) ?: (context as? Activity)
        activity?.recreate()
    }

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun applyLanguage(context: Context, lang: String? = null): Context {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val language = lang ?: prefs.getString(KEY_LANG, "es") ?: "es"
        val locale = Locale(language)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        @Suppress("DEPRECATION")
        context.applicationContext?.resources?.let { appRes ->
            val appConfig = Configuration(appRes.configuration)
            appConfig.setLocale(locale)
            appRes.updateConfiguration(appConfig, appRes.displayMetrics)
        }
        return context.createConfigurationContext(config)
    }
}
