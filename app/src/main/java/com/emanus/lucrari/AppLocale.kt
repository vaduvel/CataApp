package com.emanus.lucrari

import android.content.Context
import android.content.res.Configuration
import com.emanus.lucrari.data.AppPrefs
import java.util.Locale

enum class AppLanguage(val code: String) {
	ROMANIAN("ro"),
	ITALIAN("it"),
	;

	companion object {
		fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: ITALIAN
	}
}

/** Aplică limba aleasă fără rețea, cont sau schimbarea limbii întregului telefon. */
object AppLocale {

	fun current(context: Context): AppLanguage = AppLanguage.fromCode(AppPrefs(context).languageCode)

	fun set(context: Context, language: AppLanguage) {
		AppPrefs(context).languageCode = language.code
	}

	fun wrap(context: Context): Context = contextFor(context, current(context), updateDefault = true)

	fun contextFor(
		context: Context,
		language: AppLanguage,
		updateDefault: Boolean = false,
	): Context {
		val locale = Locale.forLanguageTag(language.code)
		if (updateDefault) Locale.setDefault(locale)
		val configuration = Configuration(context.resources.configuration).apply {
			setLocale(locale)
			setLayoutDirection(locale)
		}
		return context.createConfigurationContext(configuration)
	}
}
