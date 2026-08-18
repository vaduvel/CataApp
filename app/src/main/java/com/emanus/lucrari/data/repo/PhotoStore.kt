package com.emanus.lucrari.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.Phase
import com.emanus.lucrari.data.Photo
import com.emanus.lucrari.data.now
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class PendingPhotoCapture(
	val id: String,
	val jobId: String,
	val todoId: String?,
	val phase: Phase,
	val path: String,
	val takenAt: Long,
	val uri: Uri,
)

/** Camera externă scrie prin FileProvider; aplicația nu cere permisiune de storage. */
class PhotoStore(private val context: Context, private val db: AppDb) {
	private val directory = File(context.filesDir, "photos")

	fun observe(jobId: String): Flow<List<Photo>> = db.photos().observeByJob(jobId)

	fun createCapture(jobId: String, todoId: String?, phase: Phase): PendingPhotoCapture {
		directory.mkdirs()
		val id = UUID.randomUUID().toString()
		val file = File(directory, "$id.jpg")
		if (!file.createNewFile()) throw IOException("Nu pot crea fișierul fotografiei")
		return PendingPhotoCapture(
			id = id,
			jobId = jobId,
			todoId = todoId,
			phase = phase,
			path = file.absolutePath,
			takenAt = now(),
			uri = FileProvider.getUriForFile(context, context.packageName + ".files", file),
		)
	}

	suspend fun complete(capture: PendingPhotoCapture, success: Boolean): Boolean =
		withContext(Dispatchers.IO) {
			val file = File(capture.path)
			if (!success || !file.isFile || file.length() == 0L) {
				file.delete()
				return@withContext false
			}
			normalize(file)
			db.photos().upsert(
				Photo(
					id = capture.id,
					jobId = capture.jobId,
					todoId = capture.todoId,
					path = capture.path,
					phase = capture.phase,
					takenAt = capture.takenAt,
				),
			)
			true
		}

	suspend fun delete(photo: Photo) = withContext(Dispatchers.IO) {
		db.photos().delete(photo)
		File(photo.path).delete()
	}

	fun discard(capture: PendingPhotoCapture) {
		File(capture.path).delete()
	}

	private fun normalize(file: File) {
		val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
		BitmapFactory.decodeFile(file.absolutePath, bounds)
		if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Fotografie invalidă")
		var sample = 1
		while (bounds.outWidth / sample > MAX_EDGE * 2 || bounds.outHeight / sample > MAX_EDGE * 2) {
			sample *= 2
		}
		val decoded = BitmapFactory.decodeFile(
			file.absolutePath,
			BitmapFactory.Options().apply { inSampleSize = sample },
		) ?: throw IOException("Fotografia nu poate fi citită")
		val rotation = runCatching {
			when (
				ExifInterface(file.absolutePath).getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL,
				)
			) {
				ExifInterface.ORIENTATION_ROTATE_90 -> 90f
				ExifInterface.ORIENTATION_ROTATE_180 -> 180f
				ExifInterface.ORIENTATION_ROTATE_270 -> 270f
				else -> 0f
			}
		}.getOrDefault(0f)
		val rotated = if (rotation == 0f) {
			decoded
		} else {
			Bitmap.createBitmap(
				decoded,
				0,
				0,
				decoded.width,
				decoded.height,
				Matrix().apply { postRotate(rotation) },
				true,
			).also { decoded.recycle() }
		}
		val longest = maxOf(rotated.width, rotated.height)
		val finalBitmap = if (longest > MAX_EDGE) {
			val scale = MAX_EDGE.toDouble() / longest.toDouble()
			Bitmap.createScaledBitmap(
				rotated,
				(rotated.width * scale).toInt().coerceAtLeast(1),
				(rotated.height * scale).toInt().coerceAtLeast(1),
				true,
			).also { rotated.recycle() }
		} else {
			rotated
		}
		val temporary = File(file.parentFile, file.name + ".tmp")
		try {
			temporary.outputStream().use { output ->
				if (!finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
					throw IOException("Fotografia nu poate fi comprimată")
				}
			}
			if (!file.delete() || !temporary.renameTo(file)) {
				temporary.copyTo(file, overwrite = true)
				temporary.delete()
			}
		} finally {
			finalBitmap.recycle()
			temporary.delete()
		}
	}

	companion object {
		private const val MAX_EDGE = 1600
		private const val JPEG_QUALITY = 80
	}
}
