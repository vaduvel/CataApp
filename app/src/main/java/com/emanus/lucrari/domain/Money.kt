package com.emanus.lucrari.domain

/**
 * Banii se țin în cenți, ca Long (SPEC §5.1). Scrierea și citirea sumelor se fac de mână, nu
 * cu NumberFormat, din două motive: textul trebuie să arate identic pe orice telefon, indiferent
 * de limba sistemului, iar testul golden din M6 compară șir cu șir.
 *
 * Formatul e cel folosit în Italia și în România: punct la mii, virgulă la bani.
 */
object Money {

	/** 240000 -> 2.400,00 € */
	fun format(cents: Long): String = plain(cents) + " €"

	/** 240000 -> 2.400,00, fără simbol. Textul facturii pune singur moneda. */
	fun plain(cents: Long): String {
		val negative = cents < 0
		val abs = if (negative) -cents else cents
		val whole = abs / 100
		val rest = abs % 100
		val sign = if (negative) "-" else ""
		val bani = if (rest < 10) "0" + rest else rest.toString()
		return sign + thousands(whole) + "," + bani
	}

	private fun thousands(whole: Long): String {
		val digits = whole.toString()
		val out = StringBuilder()
		for (index in digits.indices) {
			if (index > 0 && (digits.length - index) % 3 == 0) out.append('.')
			out.append(digits[index])
		}
		return out.toString()
	}

	/**
	 * Citește ce a scris el în câmpul de preț: 180, 180,50, 180.50, 1.800, 1.800,50, cu sau
	 * fără euro și spații. Întoarce null când nu se înțelege nimic din text, ca butonul de
	 * salvare să rămână stins în loc să se salveze o cifră greșită.
	 *
	 * Virgula e mereu zecimală. Un punct urmat de exact trei cifre e separator de mii, deci
	 * 1.800 înseamnă o mie opt sute, nu unu și optzeci. Tastatura de telefon dă uneori doar
	 * punct, de aceea se acceptă și 180.50.
	 */
	fun parse(input: String): Long? {
		val raw = input.trim()
			.replace("€", "")
			.replace(" ", "")
			.replace("\u00a0", "")
		if (raw.isEmpty()) return null
		val negative = raw.startsWith("-")
		val body = raw.removePrefix("-").removePrefix("+")
		if (body.isEmpty()) return null
		if (body.any { !it.isDigit() && it != '.' && it != ',' }) return null

		val normal = normalize(body) ?: return null
		val dot = normal.indexOf('.')
		val wholePart = if (dot < 0) normal else normal.substring(0, dot)
		val fraction = if (dot < 0) "" else normal.substring(dot + 1)
		if (wholePart.length > 15) return null
		val whole = (if (wholePart.isEmpty()) "0" else wholePart).toLongOrNull() ?: return null

		var cents = whole * 100
		if (fraction.isNotEmpty()) {
			val bani = (fraction + "00").substring(0, 2).toLongOrNull() ?: return null
			cents += bani
			val third = fraction.getOrNull(2)
			if (third != null && third >= '5') cents += 1
		}
		return if (negative) -cents else cents
	}

	/** Aduce textul la o formă cu un singur punct zecimal, sau null dacă e stricat. */
	private fun normalize(body: String): String? {
		val lastDot = body.lastIndexOf('.')
		val lastComma = body.lastIndexOf(',')
		val lastSeparator = if (lastDot > lastComma) lastDot else lastComma
		if (lastSeparator < 0) return body
		val head = body.substring(0, lastSeparator).filter { it.isDigit() }
		val tail = body.substring(lastSeparator + 1)
		if (tail.any { !it.isDigit() }) return null
		val isComma = lastSeparator == lastComma
		return when {
			tail.isEmpty() -> head
			isComma -> head + "." + tail
			tail.length == 3 && lastComma < 0 -> head + tail
			else -> head + "." + tail
		}
	}
}
