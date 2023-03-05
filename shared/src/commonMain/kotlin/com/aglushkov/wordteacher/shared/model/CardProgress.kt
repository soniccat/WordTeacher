package com.aglushkov.wordteacher.shared.model

import com.aglushkov.wordteacher.shared.general.TimeSource
import kotlinx.serialization.Serializable

@Serializable
data class CardProgress(
    val currentLevel: Int = 0,
    val lastMistakeCount: Int = 0,
    val lastLessonDate: Long = 0
) {

    companion object {
        val EMPTY = CardProgress()
    }

    fun progress(): Float {
        val result = if (currentLevel < CardProgressTable.lastLevel) {
            CardProgressTable.progress(currentLevel).toFloat()
        } else {
            100f
        }

        return result / 100.0f
    }

    fun isReadyToLearn(timeSource: TimeSource): Boolean {
        return if (isCompleted()) {
            false
        } else {
            val newLessonDate: Long = nextLessonDate()
            if (newLessonDate != 0L) {
                timeSource.timeInMilliseconds() >= newLessonDate
            } else {
                true
            }
        }
    }

    fun nextLessonDate(): Long {
        var result: Long = 0
        if (lastLessonDate != 0L) {
            val interval = CardProgressTable.nextLessonInterval(currentLevel)
            result = lastLessonDate + interval
        }

        return result
    }

    fun isCompleted(): Boolean =
        currentLevel >= CardProgressTable.lastLevel

    fun withRightAnswer(timeSource: TimeSource) =
        copy(
            currentLevel = currentLevel + 1,
            lastMistakeCount = 0,
            lastLessonDate = getNewLastLessonDate(timeSource)
        )

    fun withWrongAnswer(timeSource: TimeSource): CardProgress {
        val tooManyMistakes = lastMistakeCount + 1 >= 2
        return copy(
            currentLevel = if (tooManyMistakes && currentLevel > 0) {
                currentLevel - 1
            } else {
                currentLevel
            },
            lastMistakeCount = if (tooManyMistakes) {
                0
            } else {
                lastMistakeCount + 1
            },
            lastLessonDate = getNewLastLessonDate(timeSource)
        )
    }

//    private fun updateLastLessonDate(timeSource: TimeSource) {
//        lastLessonDate = timeSource.getTimeInMilliseconds()
//    }

    fun getNewLastLessonDate(timeSource: TimeSource) = timeSource.timeInMilliseconds()

//    fun set(progress: CardProgress) {
//        currentLevel = progress.currentLevel
//        lastMistakeCount = progress.lastMistakeCount
//        lastLessonDate = progress.lastLessonDate
//    }
}

private object CardProgressTable {
    private const val MIN = 60000
    private const val HOUR = 60 * MIN
    private const val LEARN_SPAN = 8 * HOUR

    // progress, next lesson time interval
    private val LEARN_TABLE = arrayOf(
        //            progress in % | time interval to the next lession
        /* 0 level */ intArrayOf(0, 0),
        /* 1 level */ intArrayOf(5, 0),
        /* 2 level */ intArrayOf(20, LEARN_SPAN),
        /* 3 level */ intArrayOf(30, LEARN_SPAN),
        /* 4 level */ intArrayOf(50, 2 * LEARN_SPAN),
        /* 5 level */ intArrayOf(70, 2 * LEARN_SPAN),
        /* 6 level */ intArrayOf(90, 3 * LEARN_SPAN)
    )
    val lastLevel = LEARN_TABLE.size

    fun progress(level: Int): Int {
        return LEARN_TABLE[level][0]
    }

    fun nextLessonInterval(level: Int): Int {
        return LEARN_TABLE[level][1]
    }
}
