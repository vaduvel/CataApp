package com.emanus.lucrari.data.repo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.backup.BackupCodec
import com.emanus.lucrari.data.backup.BackupPayload
import com.emanus.lucrari.data.backup.BackupSnapshot
import com.emanus.lucrari.data.backup.toPayload
import com.emanus.lucrari.data.backup.toSnapshot
import com.emanus.lucrari.domain.BackupRotation
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class ImportMode { REPLACE, MERGE }

data class ImportResult(val recordsRead: Int, val photosRead: Int)

/**
 * Arhiva M7: data.json plus photos/<uuid>.jpg. Toate operațiile sunt serializate ca un
 * import să nu poată concura cu worker-ul de backup.
 */
class BackupRepo(private val context: Context, private val db: AppDb) {
	private val mutex = Mutex()
	private val backupDir = File(context.filesDir, "backup")
	private val photoDir = File(context.filesDir, "photos")

	suspend fun createAutomaticBackup(
		date: LocalDate = LocalDate.now(),
		exportedAt: Instant = Instant.now(),
	): File = withContext(Dispatchers.IO) {
		mutex.withLock { createAutomaticBackupLocked(date, exportedAt) }
	}

	suspend fun exportTo(uri: Uri, exportedAt: Instant = Instant.now()) = withContext(Dispatchers.IO) {
		mutex.withLock {
			val snapshot = readSnapshot()
			val output = context.contentResolver.openOutputStream(uri, "w")
				?: throw IOException("Nu pot deschide fișierul ales")
			output.use { writeArchive(it, snapshot, exportedAt) }
		}
	}

	suspend fun importFrom(uri: Uri, mode: ImportMode): ImportResult = withContext(Dispatchers.IO) {
		mutex.withLock {
			val input = context.contentResolver.openInputStream(uri)
				?: throw IOException("Nu pot citi fișierul ales")
			input.use { readAndImport(it, mode) }
		}
	}

	suspend fun latestOrCreate(): File = withContext(Dispatchers.IO) {
		mutex.withLock {
			backupDir.mkdirs()
			backupDir.listFiles()
				?.filter { it.isFile && it.name.matches(ARCHIVE_NAME) }
				?.maxByOrNull { it.name }
				?: createAutomaticBackupLocked(LocalDate.now(), Instant.now())
		}
	}

	fun shareUri(file: File): Uri = FileProvider.getUriForFile(
		context,
		context.packageName + ".files",
		file,
	)

	private suspend fun createAutomaticBackupLocked(date: LocalDate, exportedAt: Instant): File {
		backupDir.mkdirs()
		val target = File(backupDir, "lucrari-$date.zip")
		val temporary = File(backupDir, target.name + ".tmp")
		if (temporary.exists()) temporary.delete()
		val snapshot = readSnapshot()
		temporary.outputStream().use { writeArchive(it, snapshot, exportedAt) }
		if (target.exists() && !target.delete()) throw IOException("Nu pot înlocui backup-ul zilei")
		if (!temporary.renameTo(target)) {
			temporary.copyTo(target, overwrite = true)
			temporary.delete()
		}
		val names = backupDir.listFiles()?.filter { it.isFile }?.map { it.name }.orEmpty()
		BackupRotation.filesToDelete(names).forEach { File(backupDir, it).delete() }
		return target
	}

	private suspend fun readSnapshot(): BackupSnapshot = db.withTransaction {
		val dao = db.backup()
		BackupSnapshot(
			clients = dao.clients(),
			jobs = dao.jobs(),
			stages = dao.stages(),
			workDays = dao.workDays(),
			todos = dao.todos(),
			materials = dao.materials(),
			measures = dao.measures(),
			extras = dao.extras(),
			payments = dao.payments(),
			invoices = dao.invoices(),
			photos = dao.photos().filter { File(it.path).isFile },
			reminders = dao.reminders(),
		)
	}

	private fun writeArchive(output: OutputStream, snapshot: BackupSnapshot, exportedAt: Instant) {
		ZipOutputStream(output.buffered()).use { zip ->
			val payload = snapshot.toPayload(exportedAt.toString())
			zip.putNextEntry(ZipEntry(DATA_ENTRY))
			zip.write(BackupCodec.encode(payload).toByteArray(Charsets.UTF_8))
			zip.closeEntry()
			for (photo in snapshot.photos) {
				val source = File(photo.path)
				if (!source.isFile) continue
				zip.putNextEntry(ZipEntry("photos/${photo.id}.jpg"))
				source.inputStream().use { it.copyTo(zip) }
				zip.closeEntry()
			}
		}
	}

	private suspend fun readAndImport(input: InputStream, mode: ImportMode): ImportResult {
		val stagingRoot = File(context.filesDir, ".import-${UUID.randomUUID()}")
		val stagingPhotos = File(stagingRoot, "photos")
		stagingPhotos.mkdirs()
		try {
			var jsonBytes: ByteArray? = null
			ZipInputStream(input.buffered()).use { zip ->
				var entry = zip.nextEntry
				while (entry != null) {
					if (!entry.isDirectory) {
						when {
							entry.name == DATA_ENTRY -> jsonBytes = readLimited(zip, MAX_JSON_BYTES)
							entry.name.startsWith("photos/") -> {
								val fileName = entry.name.removePrefix("photos/")
								if (!SAFE_PHOTO_NAME.matches(fileName)) {
									throw IOException("Nume de fotografie invalid în arhivă")
								}
								File(stagingPhotos, fileName).outputStream().use { out ->
									copyLimited(zip, out, MAX_PHOTO_BYTES)
								}
							}
						}
					}
					zip.closeEntry()
					entry = zip.nextEntry
				}
			}
			val payload = BackupCodec.decode(
				(jsonBytes ?: throw IOException("Arhiva nu conține data.json")).toString(Charsets.UTF_8),
			)
			validatePhotos(payload, stagingPhotos)
			// Conversia validează datele, enum-urile și toate câmpurile înainte de orice ștergere.
			payload.toSnapshot(photoDir)
			val snapshot = payload.toSnapshot(photoDir)
			when (mode) {
				ImportMode.REPLACE -> replaceAll(snapshot, stagingPhotos)
				ImportMode.MERGE -> mergeMissing(snapshot, stagingPhotos, payload)
			}
			return ImportResult(snapshot.recordCount, snapshot.photos.size)
		} finally {
			stagingRoot.deleteRecursively()
		}
	}

	private fun validatePhotos(payload: BackupPayload, stagingPhotos: File) {
		val unique = payload.photos.map { it.id }.toSet()
		if (unique.size != payload.photos.size) throw IOException("Fotografii duplicate în arhivă")
		for (photo in payload.photos) {
			if (!SAFE_ID.matches(photo.id) || photo.fileName != "${photo.id}.jpg") {
				throw IOException("Referință de fotografie invalidă")
			}
			if (!File(stagingPhotos, photo.fileName).isFile) {
				throw IOException("Lipsește fotografia ${photo.fileName}")
			}
		}
	}

	private suspend fun replaceAll(snapshot: BackupSnapshot, stagingPhotos: File) {
		val oldPhotos = File(context.filesDir, ".photos-before-import-${UUID.randomUUID()}")
		val hadOldPhotos = photoDir.exists()
		if (hadOldPhotos && !photoDir.renameTo(oldPhotos)) {
			throw IOException("Nu pot pregăti fotografiile existente")
		}
		if (!stagingPhotos.renameTo(photoDir)) {
			if (hadOldPhotos) oldPhotos.renameTo(photoDir)
			throw IOException("Nu pot instala fotografiile din backup")
		}
		try {
			db.withTransaction {
				val dao = db.backup()
				dao.clearReminders()
				dao.clearClients()
				insertSnapshot(snapshot)
			}
			oldPhotos.deleteRecursively()
		} catch (error: Throwable) {
			photoDir.deleteRecursively()
			if (hadOldPhotos) oldPhotos.renameTo(photoDir)
			throw error
		}
	}

	private suspend fun mergeMissing(
		snapshot: BackupSnapshot,
		stagingPhotos: File,
		payload: BackupPayload,
	) {
		photoDir.mkdirs()
		val existingPhotoIds = db.backup().photos().map { it.id }.toSet()
		val createdFiles = mutableListOf<File>()
		try {
			for (photo in payload.photos) {
				if (photo.id in existingPhotoIds) continue
				val destination = File(photoDir, photo.fileName)
				val existed = destination.exists()
				File(stagingPhotos, photo.fileName).copyTo(destination, overwrite = true)
				if (!existed) createdFiles += destination
			}
			db.withTransaction { insertSnapshot(snapshot) }
		} catch (error: Throwable) {
			createdFiles.forEach { it.delete() }
			throw error
		}
	}

	private suspend fun insertSnapshot(snapshot: BackupSnapshot) {
		val dao = db.backup()
		dao.insertClients(snapshot.clients)
		dao.insertJobs(snapshot.jobs)
		dao.insertStages(snapshot.stages)
		dao.insertWorkDays(snapshot.workDays)
		dao.insertTodos(snapshot.todos)
		dao.insertMaterials(snapshot.materials)
		dao.insertMeasures(snapshot.measures)
		dao.insertExtras(snapshot.extras)
		dao.insertPayments(snapshot.payments)
		dao.insertInvoices(snapshot.invoices)
		dao.insertPhotos(snapshot.photos)
		dao.insertReminders(snapshot.reminders)
	}

	private fun readLimited(input: InputStream, maxBytes: Long): ByteArray {
		val output = ByteArrayOutputStream()
		copyLimited(input, output, maxBytes)
		return output.toByteArray()
	}

	private fun copyLimited(input: InputStream, output: OutputStream, maxBytes: Long) {
		val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
		var total = 0L
		while (true) {
			val count = input.read(buffer)
			if (count < 0) break
			total += count
			if (total > maxBytes) throw IOException("Fișier prea mare în arhivă")
			output.write(buffer, 0, count)
		}
	}

	companion object {
		private const val DATA_ENTRY = "data.json"
		private const val MAX_JSON_BYTES = 20L * 1024L * 1024L
		private const val MAX_PHOTO_BYTES = 40L * 1024L * 1024L
		private val ARCHIVE_NAME = Regex("lucrari-\\d{4}-\\d{2}-\\d{2}\\.zip")
		private val SAFE_ID = Regex("[A-Za-z0-9_-]+")
		private val SAFE_PHOTO_NAME = Regex("[A-Za-z0-9_-]+\\.jpg")
	}
}
