package com.aglushkov.wordteacher.shared.features.learning.cardteacher

import com.aglushkov.wordteacher.shared.general.TimeSource

interface CardProgress {
    val currentLevel: Int
    val lastMistakeCount: Int
    val lastLessonDate: Long

    companion object {
        val EMPTY = ImmutableCardProgress()
    }

    fun progress(): Float {
        val result = if (currentLevel < CardProgressTable.lastLevel) {
            CardProgressTable.progress(currentLevel).toFloat()
        } else {
            100f
        }

        return result / 100.0f
    }

    fun isReadyForLearning(timeSource: TimeSource): Boolean {
        var result = true

        if (isCompleted()) {
            result = false
        } else {
            val newLessonDate: Long = nextLessonDate()
            if (newLessonDate != 0L) {
                result = timeSource.getTimeInMilliseconds() >= newLessonDate
            }
        }

        return result
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

    fun toMutableCardProgress(): MutableCardProgress =
        MutableCardProgress(
            currentLevel = currentLevel,
            lastMistakeCount = lastMistakeCount,
            lastLessonDate = lastLessonDate
        )

    fun toImmutableCardProgress(): ImmutableCardProgress =
        if (this is ImmutableCardProgress) {
            this
        } else {
            ImmutableCardProgress(
                currentLevel = currentLevel,
                lastMistakeCount = lastMistakeCount,
                lastLessonDate = lastLessonDate
            )
        }
}

data class ImmutableCardProgress (
    override val currentLevel: Int = 0,
    override val lastMistakeCount: Int = 0,
    override val lastLessonDate: Long = 0
) : CardProgress

data class MutableCardProgress(
    override var currentLevel: Int = 0,
    override var lastMistakeCount: Int = 0,
    override var lastLessonDate: Long = 0
) : CardProgress {

    fun applyRightAnswer(timeSource: TimeSource) {
        ++currentLevel
        lastMistakeCount = 0

        updateLastLessonDate(timeSource)
    }

    fun applyWrongAnswer(timeSource: TimeSource) {
        ++lastMistakeCount

        if (lastMistakeCount >= 2) {
            lastMistakeCount = 0

            if (currentLevel > 0) {
                currentLevel--
            }
        }

        updateLastLessonDate(timeSource)
    }

    private fun updateLastLessonDate(timeSource: TimeSource) {
        lastLessonDate = timeSource.getTimeInMilliseconds()
    }

    fun set(progress: CardProgress) {
        currentLevel = progress.currentLevel
        lastMistakeCount = progress.lastMistakeCount
        lastLessonDate = progress.lastLessonDate
    }
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