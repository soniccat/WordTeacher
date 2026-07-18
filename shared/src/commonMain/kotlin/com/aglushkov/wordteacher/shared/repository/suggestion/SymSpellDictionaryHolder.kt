package com.aglushkov.wordteacher.shared.repository.suggestion

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.wordlist.WordListDictIndex
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.repository.db.MisspellingDatabase
import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.api.HashFunction
import com.darkrockstudios.symspellkt.common.DictionaryItem
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.SpellHelper.getEditDeletes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SymSpellDictionaryHolder(
    private val spellCheckSettings: SpellCheckSettings,
    private val hashFunction: HashFunction,
    private val misspellingDB: MisspellingDatabase,
    private val settingStore: SettingStore,
    private val dictProvider: () -> Dict?,
): DictionaryHolder {
    val isReady: Boolean
        get() {
            return dictProvider() != null
        }

    override val wordCount: Int
        get() {
            return 0
        }

    suspend fun resetProgress() {
        settingStore[SYMSPELL_DB_ENTRIES_OFFSET_KEY] = 0
        misspellingDB.deleteAll()
    }

    fun fillFromDict(dict: Dict): Flow<Pair<Float, Unit>> = flow {
        val offset = settingStore.int(SYMSPELL_DB_ENTRIES_OFFSET_KEY) ?: 0
        val wordCount = (dict.index as WordListDictIndex).wordCount
        val deletes: MutableMap<Long, ArrayList<String>> = mutableMapOf()

        val progress = offset.toFloat() / wordCount.toFloat()
        emit(progress to Unit)

        for (entry in dict.index.allEntries().withIndex().drop(offset)) {
            if (entry.value.word.length > 15) {
                continue
            }

            addItem(entry.value.word, deletes)
            val size = deletes.values.sumOf { it.size }
            if (size > 20000) {
                val i = entry.index
                Logger.v("start at index $i")
                misspellingDB.upsert(deletes)
                deletes.clear()

                val progress = (i+1).toFloat() / wordCount.toFloat()
                Logger.v("upsert completed at index $i, $progress")
                settingStore[SYMSPELL_DB_ENTRIES_OFFSET_KEY] = i+1
                emit(progress to Unit)
            }
        }

        misspellingDB.upsert(deletes)
        emit(1.0f to Unit)
    }

    override fun addItem(dictionaryItem: DictionaryItem): Boolean {
        //return addItem(dictionaryItem.term)
        TODO("not supported")
    }

    fun addItem(term: String, deletes: MutableMap<Long, ArrayList<String>>): Boolean {
        var key = term
        if (spellCheckSettings.lowerCaseTerms) {
            key = key.lowercase()
        }
        /*
         * edits/suggestions are created only once, no matter how often
         * word occurs. edits/suggestions are created as soon as the
         * word occurs in the corpus, even if the same term existed
         * before in the dictionary as an edit from another word
         */
        if (key.length > spellCheckSettings.maxLength) {
            spellCheckSettings.maxLength = key.length
        }

        //create deletes
        val editDeletes = getEditDeletes(
            key,
            spellCheckSettings.maxEditDistance,
            spellCheckSettings.prefixLength,
            spellCheckSettings.editFactor,
        )
        for (delete in editDeletes) {
            val hash = hashFunction.hash(delete)
            if (hash != null) {
                if (deletes.containsKey(hash)) {
                    deletes[hash]!!.add(key)
                } else {
                    deletes[hash] = arrayListOf(key)
                }
            }
        }
        return true
    }

    override fun clear(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getDeletes(key: String): ArrayList<String>? {
        return ArrayList(
            misspellingDB.select(hashFunction.hash(key)!!)
        )
    }

    override fun getItemFrequency(term: String): Double? {
        val dict = dictProvider() ?: return null
        val e = dict.index.indexEntry(term)
        if (e == null) {
            return null // entry doesn't exist
        }

        return 0.0 // calc frequency later using WordFrequencyDatabase
    }

    override fun getItemFrequencyBiGram(term: String): Double? {
        return getItemFrequency(term)
    }

    // redundant

    override fun addExclusionItem(key: String, value: String) {}

    override fun addExclusionItems(values: Map<String, String>) {}

    override fun getExclusionItem(key: String): String? { return null }
}

private const val SYMSPELL_DB_ENTRIES_OFFSET_KEY = "SYMSPELL_DB_ENTRIES_OFFSET_KEY"
