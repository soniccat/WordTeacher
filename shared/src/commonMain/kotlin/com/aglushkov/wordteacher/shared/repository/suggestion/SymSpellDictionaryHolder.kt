package com.aglushkov.wordteacher.shared.repository.suggestion

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.dsl.DslDict
import com.aglushkov.wordteacher.shared.dicts.dsl.DslIndex
import com.aglushkov.wordteacher.shared.dicts.wordlist.WordListDict
import com.aglushkov.wordteacher.shared.dicts.wordlist.WordListDictIndex
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.repository.db.MisspellingDatabase
import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.api.HashFunction
import com.darkrockstudios.symspellkt.common.DictionaryItem
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.SpellHelper.getEditDeletes

class SymSpellDictionaryHolder(
    private val spellCheckSettings: SpellCheckSettings,
    private val hashFunction: HashFunction,
    private val misspellingDB: MisspellingDatabase,
): DictionaryHolder {

    override val wordCount: Int
        get() {
            return 0
        }

//    suspend fun fillFromDictionaries() {
//        dictRepository.dicts.waitUntilLoaded()
//        val dicts = dictRepository.dicts.value.asLoaded() ?: return
//        val dict = dicts.data.firstOrNull { it.name.endsWith(WORDLIST_EXTENSION) } ?: return
//        dict.index.allEntries().onEach { entry ->
//            addItem(DictionaryItem(entry.word, 0.0))
//        }
//    }

    fun fillFromDict(dict: Dict) {
        val wordCount = (dict.index as WordListDictIndex).wordCount
        val deletes: MutableMap<Long, ArrayList<String>> = mutableMapOf()
        dict.index.allEntries().mapIndexed { i, entry ->
            addItem(entry.word, deletes)

            val size = deletes.values.sumOf { it.size }
            if (size > 200000) {
//                misspellingDB.transaction {
//                    deletes.onEach { e ->
//                        misspellingDB.upsert(e.key, e.value)
//                    }
//                }
                Logger.v("start at index $i")
                misspellingDB.upsert(deletes)
                deletes.clear()
                Logger.v("upsert completed at index $i, ${i.toFloat()/wordCount.toFloat()}")
            }
//            Logger.v("size is $size")
        }.toList()

        misspellingDB.upsert(deletes)
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
        return 0.0 // calc frequency later using WordFrequencyDatabase
    }

    override fun getItemFrequencyBiGram(term: String): Double? {
        return 0.0
    }

    // redundant

    override fun addExclusionItem(key: String, value: String) {}

    override fun addExclusionItems(values: Map<String, String>) {}

    override fun getExclusionItem(key: String): String? { return null }
}