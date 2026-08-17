package com.emanus.lucrari.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paletă cu contrast mare: telefonul se folosește pe șantier, adesea în plin soare.
// Nu folosim culori dinamice (Material You) ca aspectul să fie mereu previzibil.

private val Brick = Color(0xFF9A3412)
private val BrickLight = Color(0xFFFFDBCF)
private val BrickOnDark = Color(0xFFFFB59B)
private val Slate = Color(0xFF1C1B1A)
private val Sand = Color(0xFFFBF8F6)
private val SandDark = Color(0xFF141312)

val LightColors: ColorScheme = lightColorScheme(
	primary = Brick,
	onPrimary = Color.White,
	primaryContainer = BrickLight,
	onPrimaryContainer = Color(0xFF3B0D02),
	background = Sand,
	onBackground = Slate,
	surface = Color.White,
	onSurface = Slate,
	surfaceVariant = Color(0xFFEFE7E2),
	onSurfaceVariant = Color(0xFF534B47),
	error = Color(0xFFB3261E),
)

val DarkColors: ColorScheme = darkColorScheme(
	primary = BrickOnDark,
	onPrimary = Color(0xFF521800),
	primaryContainer = Color(0xFF6F2400),
	onPrimaryContainer = BrickLight,
	background = SandDark,
	onBackground = Color(0xFFECE0DB),
	surface = Color(0xFF201F1E),
	onSurface = Color(0xFFECE0DB),
	surfaceVariant = Color(0xFF534B47),
	onSurfaceVariant = Color(0xFFD8C2BA),
	error = Color(0xFFF2B8B5),
)

/**
 * Culorile statusurilor de lucrare (SPEC §5.1). Se folosesc identic în liste,
 * chip-uri și calendar, ca să însemne mereu același lucru.
 */
object StatusColor {
	val Ofertat = Color(0xFF6B7280)
	val Programat = Color(0xFF2563EB)
	val InLucru = Color(0xFFF59E0B)
	val Asteptare = Color(0xFFDC2626)
	val DeFinisat = Color(0xFF7C3AED)
	val Terminat = Color(0xFF16A34A)
	val Anulat = Color(0xFF9CA3AF)
}
