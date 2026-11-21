package com.wantstracker.app.domain

import com.wantstracker.app.data.WantEntity
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

const val DAY_MS = 86_400_000L

enum class Status(val label: String, val emoji: String) {
    NOT_STARTED("Not started", "⚪"),
    IN_PROGRESS("In progress", "🔵"),
    BOUGHT("Bought", "🟢"),
    FAILED("Failed", "🔴");

    val isOpen: Boolean get() = this == NOT_STARTED || this == IN_PROGRESS

    companion object {
        fun fromText(text: String): Status {
            val t = text.trim().lowercase()
            return when {
                t.contains("bought") || t.contains("done") -> BOUGHT
                t.contains("progress") || t.contains("doing") || (t.contains("started") && !t.contains("not")) -> IN_PROGRESS
                t.contains("fail") || t.contains("dropped") || t.contains("cancel") -> FAILED
                else -> NOT_STARTED
            }
        }
    }
}

enum class Priority(val label: String, val emoji: String) {
    NEED("Need", "🔥"),
    WANT("Want", "✨"),
    SOMEDAY("Someday", "💭")
}

data class QuarterKey(val year: Int, val quarter: Int) : Comparable<QuarterKey> {
    override fun compareTo(other: QuarterKey): Int =
        compareValuesBy(this, other, { it.year }, { it.quarter })

    val label: String get() = "Q$quarter $year"

    fun next(): QuarterKey = if (quarter == 4) QuarterKey(year + 1, 1) else QuarterKey(year, quarter + 1)

    fun startMillis(): Long =
        LocalDate.of(year, (quarter - 1) * 3 + 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun midMillis(): Long = startMillis() + 45L * DAY_MS

    companion object {
        fun of(millis: Long): QuarterKey {
            val d = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            return QuarterKey(d.year, (d.monthValue - 1) / 3 + 1)
        }

        fun now(): QuarterKey = of(System.currentTimeMillis())

        /** Understands "Q2 2025", "2025 Q2", "2025-Q2". Returns null for anything else. */
        fun parse(text: String): QuarterKey? {
            val t = text.trim()
            Regex("(?i)q\\s*([1-4])\\D{0,3}(\\d{4})").find(t)?.let {
                return QuarterKey(it.groupValues[2].toInt(), it.groupValues[1].toInt())
            }
            Regex("(\\d{4})\\D{0,3}(?i:q)\\s*([1-4])").find(t)?.let {
                return QuarterKey(it.groupValues[1].toInt(), it.groupValues[2].toInt())
            }
            return null
        }
    }
}

val WantEntity.plannedKey: QuarterKey?
    get() {
        val y = plannedYear
        val q = plannedQuarter
        return if (y != null && q != null) QuarterKey(y, q) else null
    }

/** Planned quarter, or the quarter it was added in when unplanned. */
val WantEntity.effectiveKey: QuarterKey get() = plannedKey ?: QuarterKey.of(addedAt)

fun WantEntity.isOverdue(): Boolean {
    val p = plannedKey ?: return false
    return status.isOpen && p < QuarterKey.now()
}

fun WantEntity.coolingDaysLeft(now: Long = System.currentTimeMillis()): Int? {
    val until = coolingUntil ?: return null
    return ceil((until - now) / DAY_MS.toDouble()).toInt()
}

private val inrLocale: Locale = Locale.Builder().setLanguage("en").setRegion("IN").build()

fun formatInr(value: Double): String {
    val nf = NumberFormat.getCurrencyInstance(inrLocale)
    val digits = if (value % 1.0 == 0.0) 0 else 2
    nf.minimumFractionDigits = digits
    nf.maximumFractionDigits = digits
    return nf.format(value)
}

fun plainNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

fun formatDate(millis: Long): String =
    dateFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

/** Date pickers work in UTC midnight; convert to/from local start-of-day. */
fun pickerToLocal(utcMillis: Long): Long =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun localToPicker(localMillis: Long): Long =
    Instant.ofEpochMilli(localMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
