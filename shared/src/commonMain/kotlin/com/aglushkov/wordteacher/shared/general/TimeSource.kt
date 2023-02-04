package com.aglushkov.wordteacher.shared.general

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

interface TimeSource {
    fun stringDate(dateLong: Long): String
    fun getTimeInMilliseconds(): Long
    fun getTimeInstant(): Instant
}

class TimeSourceImpl: TimeSource {
    override fun stringDate(dateLong: Long): String {
        val dateTime = Instant.fromEpochMilliseconds(dateLong).toLocalDateTime(
            TimeZone.currentSystemDefault()
        )
        return "${dateTime.dayOfMonth}.${dateTime.monthNumber}.${dateTime.year}"
    }

    override fun getTimeInMilliseconds() = getTimeInstant().toEpochMilliseconds()

    override fun getTimeInstant(): Instant = Clock.System.now()
}