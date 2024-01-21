package com.aglushkov.wordteacher.shared.repository.db

import co.touchlab.kermit.Logger
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import com.aglushkov.wordteacher.wordfrequencydb.WordFrequencyDB
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path
import kotlin.math.pow

@Serializable
data class WordFrequencyGradation(
    val levels: List<WordFrequencyLevel>
) {
    companion object {
        const val UNKNOWN_LEVEL = -1
    }

    fun gradationLevelByFrequency(frequency: Double?): Int? {
        if (frequency == null) {
            return null
        } else if (frequency == UNKNOWN_FREQUENCY) {
            return UNKNOWN_LEVEL
        }

        for (i in levels.indices) {
            if (frequency > levels[i].frequency) {
                return i
            }
        }

        return levels.size
    }

    fun gradationLevelRatio(level: Int?): Float? {
        if (level == null) return null

        return level / levels.size.toFloat()
    }

    fun gradationLevelAndRatio(frequency: Double?): WordFrequencyLevelAndRatio? {
        if (frequency == null) {
            return null
        }

        val level = gradationLevelByFrequency(frequency) ?: return null
        val ratio = gradationLevelRatio(level) ?: return null
        return WordFrequencyLevelAndRatio(level, ratio)
    }
}

@Serializable
data class WordFrequencyLevelAndRatio(
    val level: Int,
    val ratio: Float,
)

@Serializable
data class WordFrequencyLevel(
    val index: Int,
    val frequency: Double
)

interface WordFrequencyGradationProvider {
    val gradationState: Flow<Resource<WordFrequencyGradation>>

    suspend fun resolveFrequencyForWord(word: String): Double
    suspend fun resolveFrequencyForWords(words: List<String>): List<Double>
}

class WordFrequencyDatabase(
    driverFactory: DatabaseDriverFactory,
    private val dbPreparer: () -> Path,
    private val settings: FlowSettings,
): WordFrequencyGradationProvider {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val driver = driverFactory.createFrequencyDBDriver()
    private var db = WordFrequencyDB(driver)

    val state = MutableStateFlow<Resource<WordFrequencyDatabase>>(Resource.Uninitialized())

    private val jsonCoder = Json {
        ignoreUnknownKeys = true
    }

    override var gradationState = MutableStateFlow<Resource<WordFrequencyGradation>>(Resource.Uninitialized())
    // private var gradation: WordFrequencyGradation? = null
    private val defaultFrequency: Double = UNKNOWN_FREQUENCY

    init {
        create()
    }

    suspend fun waitUntilInitialized() = state.first { it.isLoaded() }

    override suspend fun resolveFrequencyForWord(word: String): Double {
        return withContext(Dispatchers.Default) {
            waitUntilInitialized()
            db.dBWordFrequencyQueries.selectFrequency(word).executeAsOneOrNull()?.frequency ?: defaultFrequency
        }
    }

    override suspend fun resolveFrequencyForWords(words: List<String>): List<Double> {
        return withContext(Dispatchers.Default) {
            waitUntilInitialized()
            val list = db.dBWordFrequencyQueries.selectFrequencies(words).executeAsList()
            words.map { w ->
                list.firstOrNull { it.word == w }?.frequency ?: defaultFrequency
            }
        }
    }

    private fun create() {
        mainScope.launch(Dispatchers.Default) {
            loadResource {
                dbPreparer()
                val res = db.dBWordFrequencyQueries.selectFrequency("cat").executeAsOne()
                Logger.i("test", null)

                resolveGradation()

                this@WordFrequencyDatabase
            }.flowOn(Dispatchers.Default).collect(state)
        }
    }

    private suspend fun resolveGradation() {
        try {
            val gradationString = settings.getStringOrNull(WORD_FREQUENCY_GRADATION_SETTINGS_NAME)
            if (gradationString != null) {
                (jsonCoder.decodeFromString(gradationString) as? WordFrequencyGradation)?.let { gradation ->
                    gradationState.update { Resource.Loaded(gradation) }
                }
            }

//            defaultFrequency = settings.getDoubleOrNull(WORD_FREQUENCY_DEFAULT_SETTINGS_NAME) ?: UNKNOWN_FREQUENCY
        } catch (_: Exception) {
        }

        if (gradationState.value.isUninitialized()) {
            val newGradation = calcGradation()
            gradationState.update { Resource.Loaded(newGradation) }
//            val defaultFrequency = if (newGradation.levels.isEmpty()) {
//                UNKNOWN_FREQUENCY
//            } else {
//                newGradation.levels[newGradation.levels.size/2].frequency
//            }

            settings.putString(WORD_FREQUENCY_GRADATION_SETTINGS_NAME, jsonCoder.encodeToString(newGradation))
//            settings.putDouble(WORD_FREQUENCY_DEFAULT_SETTINGS_NAME, defaultFrequency)
        }
    }

    private fun calcGradation(): WordFrequencyGradation {
        val levelCount = 5
        val m = 4.0 // multiplier
        val range = (0 until levelCount)

        // calc x in x + m*x + m^2*x + m^3*x + m^4*x ... = rowCount
        val xSum = range.fold(0) { acc, i -> acc + m.pow(i).toInt() }
        val rowCount = db.dBWordFrequencyQueries.selectCount().executeAsOne()
        val x = rowCount.toDouble() / xSum.toDouble()
        val rowIndexes = range.map { (m.pow(it) * x).toInt() }
        val levels = rowIndexes.mapIndexed { i, v ->
            val frequency = db.dBWordFrequencyQueries.selectOrderedFrequencyWithOffset(v.toLong())
                .executeAsOne().frequency ?: 0.0
            WordFrequencyLevel(i, frequency)
        }

        return WordFrequencyGradation(levels)
    }
}

private const val WORD_FREQUENCY_GRADATION_SETTINGS_NAME = "wordFrequencyGradation"
//private const val WORD_FREQUENCY_DEFAULT_SETTINGS_NAME = "wordFrequencyDefault"

private const val UNKNOWN_FREQUENCY = -1.0
