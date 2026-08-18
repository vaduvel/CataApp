package com.emanus.lucrari.domain

/** Numele ISO se sortează cronologic; păstrăm numai cele mai noi șapte arhive zilnice. */
object BackupRotation {
	private val archive = Regex("lucrari-\\d{4}-\\d{2}-\\d{2}\\.zip")

	fun filesToDelete(names: List<String>, keep: Int = 7): List<String> {
		require(keep >= 1)
		return names.filter { archive.matches(it) }.sortedDescending().drop(keep)
	}
}
