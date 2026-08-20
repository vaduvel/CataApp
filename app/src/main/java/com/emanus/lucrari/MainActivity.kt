package com.emanus.lucrari

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.emanus.lucrari.ui.nav.AppRoot
import com.emanus.lucrari.ui.theme.LucrariTheme

/**
 * Singura activitate din aplicație. Fără onboarding, fără cont: se intră direct în ecranul „Azi".
 */
class MainActivity : ComponentActivity() {
	override fun attachBaseContext(newBase: Context) {
		super.attachBaseContext(AppLocale.wrap(newBase))
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			LucrariTheme {
				val language = AppLocale.current(this)
				AppRoot(
					language = language,
					onLanguageChange = { selected ->
						if (selected != language) {
							AppLocale.set(this, selected)
							recreate()
						}
					},
				)
			}
		}
	}
}
