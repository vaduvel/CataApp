package com.emanus.lucrari.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paleta vine direct din iconiță: calendar albastru, cască portocalie și bară coral.
// Nuanțele mai închise păstrează contrastul în soare, iar cele deschise construiesc
// suprafețele. Culorile dinamice rămân dezactivate ca identitatea să fie constantă.

private val BurntOrange = Color(0xFFB94708)
private val HelmetOrange = Color(0xFFF47A20)
private val CalendarCoral = Color(0xFFFF4F2C)
private val CalendarSky = Color(0xFFCFE3F7)
private val CalendarSkySoft = Color(0xFFEAF4FC)
private val Peach = Color(0xFFFFDBCA)
private val Charcoal = Color(0xFF1F2529)
private val SteelBlue = Color(0xFF305F73)
private val IndustrialPurple = Color(0xFF6A4C93)
private val Concrete = Color(0xFFF7F3EC)
private val WarmWhite = Color(0xFFFFFDF9)
private val Cement = Color(0xFFE8E2D9)
private val DividerGray = Color(0xFFD9D3CA)
private val OutlineGray = Color(0xFF74736E)

val LightColors: ColorScheme = lightColorScheme(
	primary = BurntOrange,
	onPrimary = Color.White,
	primaryContainer = Peach,
	onPrimaryContainer = Color(0xFF3B0D02),
	secondary = SteelBlue,
	onSecondary = Color.White,
	secondaryContainer = CalendarSky,
	onSecondaryContainer = Color(0xFF10323F),
	tertiary = CalendarCoral,
	onTertiary = Color.White,
	tertiaryContainer = Color(0xFFFFDDD4),
	onTertiaryContainer = Color(0xFF511306),
	background = Concrete,
	onBackground = Charcoal,
	surface = WarmWhite,
	onSurface = Charcoal,
	surfaceVariant = CalendarSkySoft,
	onSurfaceVariant = Color(0xFF4B5157),
	surfaceTint = BurntOrange,
	surfaceDim = Color(0xFFE5DED4),
	surfaceBright = WarmWhite,
	surfaceContainerLowest = Color.White,
	surfaceContainerLow = Color(0xFFFCF8F2),
	surfaceContainer = Color(0xFFF2ECE4),
	surfaceContainerHigh = Color(0xFFEBE5DD),
	// Card-urile Material care nu au fost încă specializate folosesc implicit acest token.
	// Îl ținem alb ca toate ecranele, inclusiv cele secundare, să aparțină aceluiași sistem.
	surfaceContainerHighest = Color.White,
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
	secondaryContainer = Color(0xFF244B5D),
	onSecondaryContainer = CalendarSky,
	tertiary = Color(0xFFFFB4A0),
	onTertiary = Color(0xFF611D0C),
	tertiaryContainer = Color(0xFF7C2C18),
	onTertiaryContainer = Color(0xFFFFDBD1),
	background = Color(0xFF141312),
	onBackground = Color(0xFFECE7E1),
	surface = Color(0xFF1F1E1D),
	onSurface = Color(0xFFECE7E1),
	surfaceVariant = Color(0xFF263740),
	onSurfaceVariant = Color(0xFFD5CFC6),
	surfaceTint = Color(0xFFFFB59B),
	surfaceDim = Color(0xFF141312),
	surfaceBright = Color(0xFF3B3936),
	surfaceContainerLowest = Color(0xFF0F0E0D),
	surfaceContainerLow = Color(0xFF1B1A18),
	surfaceContainer = Color(0xFF211F1D),
	surfaceContainerHigh = Color(0xFF2B2926),
	surfaceContainerHighest = Color(0xFF36332F),
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
	val Safety = HelmetOrange
	val BrandCoral = CalendarCoral
	val BrandSky = CalendarSky
	val BrandSkySoft = CalendarSkySoft
	val BrandCharcoal = Charcoal
}
