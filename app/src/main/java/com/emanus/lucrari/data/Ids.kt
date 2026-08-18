package com.emanus.lucrari.data

import java.time.LocalDate
import java.util.UUID

// Id-uri generate în aplicație, nu autoincrement: import-ul de backup rămâne idempotent.
fun uuid(): String = UUID.randomUUID().toString()

fun now(): Long = System.currentTimeMillis()

fun today(): LocalDate = LocalDate.now()
