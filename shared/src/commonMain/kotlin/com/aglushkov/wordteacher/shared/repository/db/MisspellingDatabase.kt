package com.aglushkov.wordteacher.shared.repository.db

import app.cash.sqldelight.TransactionWithoutReturn
import com.aglushkov.wordteacher.db.Misspelling
import com.aglushkov.wordteacher.misspellingdb.MisspellingDB
import com.aglushkov.wordteacher.shared.dicts.wordlist.WORDLIST_EXTENSION
import com.aglushkov.wordteacher.shared.general.extensions.splitByChunks
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilLoaded
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.asLoaded
import com.aglushkov.wordteacher.shared.general.resource.loadResource
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.wordfrequencydb.WordFrequencyDB
import com.aglushkov.wordteacher.wordfrequencydb.WordFrequencyDB.Companion.invoke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okio.Path

class MisspellingDatabase(
    private val driverFactory: DatabaseDriverFactory,
    private val dbPreparer: () -> Path,
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var driver = driverFactory.createMisspellingDBDriver()
    private var db = MisspellingDB(driver,
        Misspelling.Adapter(
            StringListAdapter()
        )
    )

    val state = MutableStateFlow<Resource<MisspellingDatabase>>(Resource.Uninitialized())

    init {
        create()
    }

    private fun create() {
        mainScope.launch(Dispatchers.IO) {
            loadResource {
                dbPreparer()
                this@MisspellingDatabase
            }.flowOn(Dispatchers.IO).collect(state)
        }
    }

    fun select(hash: Long): List<String> {
        return db.dBMisspellingQueries.selectCandidates(hash).executeAsOneOrNull().orEmpty()
    }

    fun insert(hash: Long, candidates: List<String>) =
        db.dBMisspellingQueries.insert(hash, candidates)

    // insert or update if exists
    // it's required only for generating a db on a simulator/device
    fun upsert(map: Map<Long, ArrayList<String>>) {
        val selectHashes = map.keys.toList().splitByChunks(30000).map { chunk ->
            db.dBMisspellingQueries.selectCandidatesIn(chunk).executeAsList()
        }.flatten()

        transaction {
            // update
            selectHashes.onEach {
                val currentCandidates = it.candidates.toSet()
                val newCandidates = map[it.hash]!!.toSet()
                if (newCandidates.subtract(currentCandidates).isNotEmpty()) {
                    db.dBMisspellingQueries.update((newCandidates + currentCandidates).toList(), it.hash)
                }
            }

            // insert
            map.keys.subtract(
                selectHashes.map { it.hash }.toSet()
            ).onEach {
                db.dBMisspellingQueries.insert(it, map[it]!!)
            }
        }
    }

//    fun upsert(hash: Long, currentCandidates: List<String>, candidates: List<String>) {
//        val resultCandidates = if (currentCandidates.isEmpty()) {
//            if (candidates.isEmpty()) {
//                return
//            }
//
//            candidates
//        } else {
//            (currentCandidates + candidates).toSet().toList()
//        }
//
//        if (candidates.toSet().subtract(currentCandidates.toSet()).isNotEmpty()) {
//            if (currentCandidates.isEmpty()) {
//                db.dBMisspellingQueries.update(resultCandidates, hash)
//            } else {
//                db.dBMisspellingQueries.insert(hash, resultCandidates)
//            }
//        }
//    }

    fun transaction(
        noEnclosing: Boolean = false,
        body: TransactionWithoutReturn.() -> Unit
    ) = db.transaction(noEnclosing, body)
}