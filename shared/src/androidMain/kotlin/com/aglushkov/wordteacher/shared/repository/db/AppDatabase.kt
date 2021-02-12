package com.aglushkov.db

import com.aglushkov.extensions.firstLong
import com.aglushkov.nlp.NLPCore
import com.aglushkov.wordteacher.cache.DBNLPSentence
import com.aglushkov.wordteacher.cache.DBNLPTextGroup
import com.aglushkov.wordteacher.shared.cache.SQLDelightDatabase
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import com.aglushkov.wordteacher.shared.nlp.NLPSentence
import com.aglushkov.wordteacher.shared.repository.db.DatabaseDriverFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppDatabase(driverFactory: DatabaseDriverFactory) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val driver = driverFactory.createDriver()
    private var db = SQLDelightDatabase(driver)

    val textGroups = TextGroups()
    val sentencesNLP = DBNLPSentences()

    val state = MutableStateFlow<Resource<AppDatabase>>(Resource.Uninitialized())

    init {
        create()
    }

    suspend fun waitUntilInitialized() = state.first { it.isLoaded() }

    fun create() {
        state.value = Resource.Loading(this@AppDatabase)
        mainScope.launch(Dispatchers.Default) {
            try {
                createDb()
                state.value = Resource.Loaded(this@AppDatabase)
            } catch (e: Exception) {
                state.value = Resource.Error(e, true)
            }
        }
    }

    private fun createDb() {
        SQLDelightDatabase.Schema.create(driver)
    }

    inner class DBNLPSentences {
        fun insert(nlpSentence: NLPSentence) = db.dBNLPSentenceQueries.insert(
            nlpSentence.tokens.joinToString(nlpSeparator),
            nlpSentence.tags.joinToString(nlpSeparator),
            nlpSentence.lemmas.joinToString(nlpSeparator),
            nlpSentence.chunks.joinToString(nlpSeparator)
        )

        fun selectAll() = db.dBNLPSentenceQueries.selectAll()

        fun removeAll() = db.dBNLPSentenceQueries.removeAll()
    }

    inner class TextGroups {
        fun insert(textGroup: DBNLPTextGroup) = db.dBNLPTextGroupQueries.insertTextGroup(textGroup)
        fun insertedTextGroupId() = db.dBNLPTextGroupQueries.lastInsertedRowId().firstLong()

        fun removeAll() = db.dBNLPTextGroupQueries.removeAll()
    }

    companion object {
        const val nlpSeparator = "&&"

        fun splitNLPString(str: String) = str.split(nlpSeparator)

        fun createNLPSentence(sentence: DBNLPSentence, nlpCore: NLPCore): NLPSentence {
            val tokens = sentence.tokens.split(nlpSeparator)
            val tags = sentence.tags.split(nlpSeparator)
            val lemmas = sentence.lemmas.split(nlpSeparator)
            val chunks = sentence.chunks.split(nlpSeparator)

            return NLPSentence(
                tokens.toTypedArray(),
                tags.toTypedArray(),
                lemmas.toTypedArray(),
                chunks.toTypedArray(),
                nlpCore
            )
        }
    }
}