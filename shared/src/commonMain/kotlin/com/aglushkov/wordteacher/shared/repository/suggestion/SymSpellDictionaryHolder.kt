package com.aglushkov.wordteacher.shared.repository.suggestion

import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.api.HashFunction
import com.darkrockstudios.symspellkt.common.DictionaryItem
import com.darkrockstudios.symspellkt.common.SpellCheckSettings
import com.darkrockstudios.symspellkt.common.SpellHelper.getEditDeletes

class SymSpellDictionaryHolder(
    private val spellCheckSettings: SpellCheckSettings,
    private val hashFunction: HashFunction,
): DictionaryHolder {

    override val wordCount: Int
        get() {
            return 0
        }

    override fun addItem(dictionaryItem: DictionaryItem): Boolean {
        if (dictionaryItem.frequency <= 0 && spellCheckSettings.countThreshold > 0) {
            return false
        }

        var key = dictionaryItem.term
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
        TODO("Not yet implemented")
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