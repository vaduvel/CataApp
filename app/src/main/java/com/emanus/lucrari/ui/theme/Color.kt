package com.emanus.lucrari.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paletă cu contrast mare: telefonul se folosește pe șantier, adesea în plin soare.
// Valorile vin din design system-ul aprobat. Nu folosim culori dinamice (Material You),
// ca aspectul să fie mereu previzibil.

private val BurntOrange = Color(0xFFB94708)
private val SafetyOrange = Color(0xFFF47A20)
private val Peach = Color(0xFFFFDBCA)
private val Charcoal = Color(0xFF1F2529)
private val SteelBlue = Color(0xFF305F73)
private val IndustrialPurple = Color(0xFF6A4C93)
private val Concrete = Color(0xFFF5F2EC)
private val Cement = Color(0xFFE5E1D8)
private val DividerGray = Color(0xFFE0E0E0)
private val OutlineGray = Color(0xFF74736E)

val LightColors: ColorScheme = lightColorScheme(
	primary = BurntOrange,
	onPrimary = Color.White,
	primaryContainer = Peach,
	onPrimaryContainer = Color(0xFF3B0D02),
	secondary = SteelBlue,
	onSecondary = Color.White,
	secondaryContainer = Color(0xFFD8E5EC),
	onSecondaryContainer = Color(0xFF10323F),
	tertiary = IndustrialPurple,
	onTertiary = Color.White,
	tertiaryContainer = Color(0xFFEBE3F5),
	onTertiaryContainer = Color(0xFF31214A),
	background = Concrete,
	onBackground = Charcoal,
	surface = Color.White,
	onSurface = Charcoal,
	surfaceVariant = Cement,
	onSurfaceVariant = Color(0xFF4B5157),
	outline = OutlineGray,
	outlineVariant = DividerGray,
	error = Color(0xFFC62828),
	onError = Color.White,
	errorContainer = Color(0xFFFADCDC),
	onErrorContainer = Color(0xFF5A1212),
)

val DarkColors: ColorScheme = darkColorScheme(
	primary = Color(0xFFFFB59B),
	onPrimary = Color(0xFF521800),
	primaryContainer = Color(0xFF7A2E05),
	onPrimaryContainer = Peach,
	secondary = Color(0xFF9CC9DC),
	onSecondary = Color(0xFF0B2C38),
	tertiary = Color(0xFFC9B2E8),
	onTertiary = Color(0xFF35234F),
	background = Color(0xFF141312),
	onBackground = Color(0xFFECE7E1),
	surface = Color(0xFF1F1E1D),
	onSurface = Color(0xFFECE7E1),
	surfaceVariant = Color(0xFF4A4842),
	onSurfaceVariant = Color(0xFFD5CFC6),
	outline = Color(0xFF9A958C),
	outlineVariant = Color(0xFF3A3936),
	error = Color(0xFFF2B8B5),
	onError = Color(0xFF5A1212),
)

/**
 * Culoarea plină a fiecărui status (SPEC §5.1): bare, puncte, accente.
 * Înseamnă mereu același lucru, în liste, chip-uri și calendar.
 */
object StatusColor {
	val Ofertat = Color(0xFF5A6570)
	val Programat = SteelBlue
	val InLucru = BurntOrange
	val Asteptare = Color(0xFFF9A825)
	val DeFinisat = IndustrialPurple
	val Terminat = Color(0xFF2E7D32)
	val Anulat = Color(0xFF8A8A85)
}

/** Fundal și text pentru un chip de status, alese ca să treacă contrastul AA. */
data class StatusTone(val container: Color, val content: Color)

object StatusTones {
	val Ofertat = StatusTone(Color(0xFFEDEFF1), Color(0xFF3F4750))
	val Programat = StatusTone(Color(0xFFDFEAF0), Color(0xFF23505F))
	val InLucru = StatusTone(Peach, Color(0xFF8E3606))
	val Asteptare = StatusTone(Color(0xFFFFF0CC), Color(0xFF6E4900))
	val DeFinisat = StatusTone(Color(0xFFEBE3F5), Color(0xFF4E3670))
	val Terminat = StatusTone(Color(0xFFDCEFDD), Color(0xFF1E5C22))
	val Anulat = StatusTone(Color(0xFFE9E8E5), Color(0xFF55554F))
}

/** Culori funcționale care nu încap în schema Material. */
object AppColor {
	val Success = Color(0xFF2E7D32)
	val SuccessContainer = Color(0xFFDCEFDD)
	val OnSuccessContainer = Color(0xFF1E5C22)
	val Warning = Color(0xFFF9A825)
	val WarningContainer = Color(0xFFFFF0CC)
	val OnWarningContainer = Color(0xFF6E4900)
	val Safety = SafetyOrange
}
