package com.emanus.lucrari.data.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class IncompatibleBackupException(val foundVersion: Int) :
	IllegalArgumentException("Versiune de backup incompatibilă: $foundVersion")

object BackupCodec {
	private val json = Json {
		prettyPrint = true
		encodeDefaults = true
		ignoreUnknownKeys = false
	}

	fun encode(payload: BackupPayload): String = json.encodeToString(payload)

	@Throws(IncompatibleBackupException::class, SerializationException::class)
	fun decode(text: String): BackupPayload {
		val payload = json.decodeFromString<BackupPayload>(text)
		if (payload.schemaVersion != BACKUP_SCHEMA_VERSION) {
			throw IncompatibleBackupException(payload.schemaVersion)
		}
		return payload
	}
}
