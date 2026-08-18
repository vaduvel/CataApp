package com.emanus.lucrari.data

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Datele calendaristice se scriu ca TEXT ISO (`2026-08-10`), ca să se poată compara și
 * sorta direct în SQL. Enum-urile se scriu ca nume, nu ca ordinal: dacă se adaugă o
 * valoare nouă în mijlocul listei, datele vechi rămân corecte.
 */
class Converters {

	@TypeConverter
	fun fromDate(value: LocalDate?): String? = value?.toString()

	@TypeConverter
	fun toDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

	@TypeConverter
	fun fromStatus(value: JobStatus): String = value.name

	@TypeConverter
	fun toStatus(value: String): JobStatus = JobStatus.valueOf(value)

	@TypeConverter
	fun fromBilling(value: Billing): String = value.name

	@TypeConverter
	fun toBilling(value: String): Billing = Billing.valueOf(value)

	@TypeConverter
	fun fromReason(value: Reason?): String? = value?.name

	@TypeConverter
	fun toReason(value: String?): Reason? = value?.let { Reason.valueOf(it) }

	@TypeConverter
	fun fromUnit(value: MeasureUnit): String = value.name

	@TypeConverter
	fun toUnit(value: String): MeasureUnit = MeasureUnit.valueOf(value)

	@TypeConverter
	fun fromMethod(value: Method): String = value.name

	@TypeConverter
	fun toMethod(value: String): Method = Method.valueOf(value)

	@TypeConverter
	fun fromInvoiceKind(value: InvoiceKind): String = value.name

	@TypeConverter
	fun toInvoiceKind(value: String): InvoiceKind = InvoiceKind.valueOf(value)

	@TypeConverter
	fun fromPhase(value: Phase): String = value.name

	@TypeConverter
	fun toPhase(value: String): Phase = Phase.valueOf(value)
}
