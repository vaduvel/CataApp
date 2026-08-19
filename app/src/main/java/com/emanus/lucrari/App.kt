package com.emanus.lucrari

import android.app.Application
import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.AppPrefs
import com.emanus.lucrari.data.repo.BackupRepo
import com.emanus.lucrari.data.repo.JobRepo
import com.emanus.lucrari.data.repo.PhotoStore
import com.emanus.lucrari.data.repo.ReminderRepo
import com.emanus.lucrari.domain.Seed
import com.emanus.lucrari.work.ReminderNotifier
import com.emanus.lucrari.work.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Un singur utilizator, un singur dispozitiv: baza de date și repository-urile trăiesc
 * cât aplicația. Fără injecție de dependențe, nu merită la dimensiunea asta.
 */
class App : Application() {

	val db: AppDb by lazy { AppDb.build(this) }
	val repo: JobRepo by lazy { JobRepo(db) }
	val backupRepo: BackupRepo by lazy { BackupRepo(this, db) }
	val photoStore: PhotoStore by lazy { PhotoStore(this, db) }
	val reminderRepo: ReminderRepo by lazy { ReminderRepo(db) }
	val prefs: AppPrefs by lazy { AppPrefs(this) }

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	override fun onCreate() {
		super.onCreate()
		ReminderNotifier.createChannel(this)
		WorkScheduler.schedule(this)
		// Datele demo se pun o singură dată, la prima pornire după instalare. Steagul se
		// pune și când baza avea deja date (telefoanele cu versiuni mai vechi), ca demo-ul
		// să nu reapară după ce utilizatorul îl șterge. Din a doua pornire nu se mai
		// atinge baza deloc.
		scope.launch {
			if (!prefs.demoSeeded) {
				Seed.ensure(db, alreadySeeded = false)
				prefs.demoSeeded = true
			}
		}
	}
}
