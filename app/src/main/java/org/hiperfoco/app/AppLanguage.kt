package org.hiperfoco.app

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList

internal enum class AppLanguage(val tag: String, val label: Int) {
    PORTUGUESE("pt-BR", R.string.language_pt),
    ENGLISH("en-US", R.string.language_en),
    SPANISH("es", R.string.language_es);

    fun save(context: Context) {
        context.getSharedPreferences("hiperfoco", Context.MODE_PRIVATE)
            .edit().putString("language", tag).apply()
    }

    companion object {
        fun selected(context: Context): AppLanguage {
            val tag = context.getSharedPreferences("hiperfoco", Context.MODE_PRIVATE)
                .getString("language", null)
            return entries.firstOrNull { it.tag == tag } ?: PORTUGUESE
        }

        fun context(base: Context): Context {
            val configuration = Configuration(base.resources.configuration)
            configuration.setLocales(LocaleList.forLanguageTags(selected(base).tag))
            return base.createConfigurationContext(configuration)
        }

    }
}
