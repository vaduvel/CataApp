package com.emanus.lucrari

import android.app.Application
import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.repo.JobRepo
import com.emanus.lucrari.domain.Seed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Un singur utilizator, un singur dispozitiv: baza de date și repository-ul trăiesc
 * cât aplicația. Fără injecție de dependențe, nu merită la dimensiunea asta.
 */
class App : Application() {

	val db: AppDb by lazy { AppDb.build(this) }
	val repo: JobRepo by lazy { JobRepo(db) }

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	override fun onCreate() {
		super.onCreate()
		// La prima pornire baza e goală; punem lucrarea demo ca ecranele să nu fie albe.
		scope.launch { Seed.ensure(db) }
	}
}
