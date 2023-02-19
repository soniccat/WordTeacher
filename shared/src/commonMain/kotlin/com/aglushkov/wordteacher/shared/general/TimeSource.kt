package com.aglushkov.wordteacher.shared.general

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

interface TimeSource {
    fun stringDate(dateLong: Long): String
    fun stringDate(instant: Instant): String
    fun getTimeInMilliseconds(): Long
    fun getTimeInstant(): Instant
}

class TimeSourceImpl: TimeSource {
    override fun stringDate(dateLong: Long): String {
        return stringDate(Instant.fromEpochMilliseconds(dateLong))
    }

    override fun stringDate(instant: Instant): String {
        val dateTime = instant.toLocalDateTime(
            TimeZone.currentSystemDefault()
        )
        return "${dateTime.dayOfMonth}.${dateTime.monthNumber}.${dateTime.year}"
    }

    override fun getTimeInMilliseconds() = getTimeInstant().toEpochMilliseconds()

    override fun getTimeInstant(): Instant = Clock.System.now()
}
