package com.emanus.lucrari

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
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			LucrariTheme {
				AppRoot()
			}
		}
	}
}
