package com.emanus.lucrari.data

import android.content.Context

/**
 * Preferințe locale, în afara bazei de date, ca să nu atingem schema Room.
 * Se șterg doar la dezinstalare sau la „Șterge datele aplicației” din Android.
 */
class AppPrefs(context: Context) {

	private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

	/**
	 * Datele demo au fost puse o dată. Rămâne `true` și după ce utilizatorul le
	 * șterge, ca demo-ul să nu reapară la următoarea pornire.
	 */
	var demoSeeded: Boolean
		get() = prefs.getBoolean(KEY_DEMO_SEEDED, false)
		set(value) {
			prefs.edit().putBoolean(KEY_DEMO_SEEDED, value).apply()
		}

	private companion object {
		const val NAME = "lucrari"
		const val KEY_DEMO_SEEDED = "demo_seeded"
	}
}
