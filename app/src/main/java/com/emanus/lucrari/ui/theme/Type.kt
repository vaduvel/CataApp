package com.emanus.lucrari.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Scara din design system-ul aprobat. Roboto vine de la sistem: zero fonturi în APK.
// Cifrele mari cer „tnum”, ca sumele să fie aliniate una sub alta în liste.
// Totul în sp, deci scalarea de sistem îl ajută dacă vede greu.
val LucrariTypography = Typography(
	displayLarge = TextStyle(
		fontSize = 34.sp,
		lineHeight = 42.sp,
		fontWeight = FontWeight.Bold,
		fontFeatureSettings = "tnum",
	),
	headlineMedium = TextStyle(
		fontSize = 28.sp,
		lineHeight = 36.sp,
		fontWeight = FontWeight.Bold,
		fontFeatureSettings = "tnum",
	),
	titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
	titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
	bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
	bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
	labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
	labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)
