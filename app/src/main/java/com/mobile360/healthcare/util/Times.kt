package com.mobile360.healthcare.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

object Times {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun at(daysFromToday: Int, hour: Int, minute: Int = 0): Long =
        LocalDate.now().plusDays(daysFromToday.toLong()).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    fun startOfDay(daysFromToday: Int): Long =
        LocalDate.now().plusDays(daysFromToday.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()

    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun toLocal(millis: Long): LocalDateTime = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()

    fun toDate(millis: Long): LocalDate = toLocal(millis).toLocalDate()
}

object Formats {
    private fun fmt(pattern: String, millis: Long): String =
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(Times.toLocal(millis))

    fun time(millis: Long): String = fmt("hh:mm a", millis)
    fun dateShort(millis: Long): String = fmt("EEE, MMM d", millis)
    fun dateLong(millis: Long): String = fmt("dd MMM yyyy", millis)
    fun dayAndTime(millis: Long): String = fmt("EEE, MMM d • hh:mm a", millis)
    fun weekdayTime(millis: Long): String = fmt("EEE hh:mm a", millis)

    // The market is India (+91), so money is in rupees.
    fun money(amount: Int): String {
        val nf = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        nf.currency = Currency.getInstance("INR")
        nf.maximumFractionDigits = 0
        return nf.format(amount)
    }
}
