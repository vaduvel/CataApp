package com.emanus.lucrari

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLocaleTest {

	@Test
	fun resursele_urmeaza_limba_aleasa_in_aplicatie() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		val italian = AppLocale.contextFor(context, AppLanguage.ITALIAN)
		val romanian = AppLocale.contextFor(context, AppLanguage.ROMANIAN)

		assertEquals("Altro", italian.getString(R.string.screen_more_title))
		assertEquals("Mai mult", romanian.getString(R.string.screen_more_title))
		assertEquals("Bagno completo", italian.getString(R.string.template_baie_completa))
		assertEquals("Baie completă", romanian.getString(R.string.template_baie_completa))
	}

	@Test
	fun italiana_este_rezerva_pentru_o_preferinta_necunoscuta() {
		assertEquals(AppLanguage.ITALIAN, AppLanguage.fromCode(null))
		assertEquals(AppLanguage.ITALIAN, AppLanguage.fromCode("necunoscuta"))
	}
}
