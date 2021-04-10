package com.aglushkov.wordteacher.shared.general

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

interface Time {
    fun stringDate(dateLong: Long): String
    fun getTimeInMilliseconds(): Long
}

class TimeImpl: Time {
    override fun stringDate(dateLong: Long): String {
        val dateTime = Instant.fromEpochMilliseconds(dateLong).toLocalDateTime(
            TimeZone.currentSystemDefault()
        )
        return "${dateTime.dayOfMonth}.${dateTime.monthNumber}.${dateTime.year}"
    }

    override fun getTimeInMilliseconds() = Clock.System.now().toEpochMilliseconds()
}