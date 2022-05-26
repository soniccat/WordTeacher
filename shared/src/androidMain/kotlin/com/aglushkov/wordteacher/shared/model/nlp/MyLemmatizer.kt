package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.repository.dict.Trie
import com.aglushkov.wordteacher.shared.repository.dict.TrieNode
import opennlp.tools.lemmatizer.Lemmatizer
import java.io.*
import java.util.*

class MyLemmatizer : Lemmatizer {
    /**
     * The hashmap containing the dictionary.
     */
    //private val dictMap: MutableMap<List<String>, List<String>> = HashMap()
    private val trie = LemmatizerTrie()

    private class LemmatizerTrie: Trie<LemmatizerEntry, LemmatizerData>(){
        override fun createEntry(
            node: TrieNode<LemmatizerEntry>,
            data: LemmatizerData
        ) =
            LemmatizerEntry(
                node,
                data.postag,
                data.lemma,
                data.isLemma
            )

        override fun setNodeForEntry(entry: LemmatizerEntry, node: TrieNode<LemmatizerEntry>) {
            entry.node = node
        }
    }

    private data class LemmatizerData(
        val word: String,
        val postag: String?,
        val lemma: LemmatizerEntry?,
        val isLemma: Boolean
    )

    private class LemmatizerEntry(
        var node: TrieNode<LemmatizerEntry>,
        val postag: String?,
        val lemma: LemmatizerEntry?,
        val isLemma: Boolean
    )

    /**
     * Construct a hashmap from the input tab separated dictionary.
     *
     * The input file should have, for each line, word\tabpostag\tablemma.
     * Alternatively, if multiple lemmas are possible for each word,postag pair,
     * then the format should be word\tab\postag\tablemma01#lemma02#lemma03
     *
     * @param dictionary
     * the input dictionary via inputstream
     */
    constructor(dictionary: InputStream) {
        init(dictionary)
    }

    constructor(dictionaryFile: File?) {
        FileInputStream(dictionaryFile).use { `in` -> init(`in`) }
    }

    @Throws(IOException::class)
    private fun init(dictionary: InputStream) {
        val breader = BufferedReader(
            InputStreamReader(dictionary)
        )
        var line: String

        val elemRegexp = "\t".toRegex()
        val lemmasRegexp = "#".toRegex()

        try {
            while (breader.readLine().also { line = it } != null) {
                val elems = line.split(elemRegexp).toTypedArray()
                val lemmas = elems[2].split(lemmasRegexp).toTypedArray()
                //dictMap[Arrays.asList(elems[0], elems[1])] = Arrays.asList(*lemmas)

                //put lemmas
                val lemmaEntry = lemmas.firstOrNull()?.let {
                    val existingLemmas = trie.word(it).filter { it.isLemma }
                    if (existingLemmas.isEmpty()) {
                        trie.put(it, LemmatizerData(it, null, null, true))
                    } else {
                        existingLemmas.first()
                    }
                }

                //put word
                trie.put(elems[0], LemmatizerData(elems[0], elems[1], lemmaEntry, false))
            }
        } catch (t: Throwable) {
            Logger.e(t.message.orEmpty())
        }
    }

    /**
     * Get the Map containing the dictionary.
     *
     * @return dictMap the Map
     */
//    fun getDictMap(): Map<List<String>, List<String>> {
//        return dictMap
//    }

    /**
     * Get the dictionary keys (word and postag).
     *
     * @param word
     * the surface form word
     * @param postag
     * the assigned postag
     * @return returns the dictionary keys
     */
//    private fun getDictKeys(word: String, postag: String): List<String> {
//        return ArrayList(Arrays.asList(word.lowercase(Locale.getDefault()), postag))
//    }

    override fun lemmatize(tokens: Array<String>, postags: Array<String>): Array<String> {
        val lemmas: MutableList<String> = ArrayList()
        for (i in tokens.indices) {
            lemmas.add(this.lemmatize(tokens[i], postags[i]))
        }
        return lemmas.toTypedArray()
    }

    override fun lemmatize(tokens: List<String>, posTags: List<String>): List<List<String>> {
//        val allLemmas: MutableList<List<String>> = ArrayList()
//        for (i in tokens.indices) {
//            allLemmas.add(getAllLemmas(tokens[i], posTags[i]))
//        }
//        return allLemmas
        throw RuntimeException("Not implemented")
    }

    /**
     * Lookup lemma in a dictionary. Outputs "O" if not found.
     *
     * @param word
     * the token
     * @param postag
     * the postag
     * @return the lemma
     */
    private fun lemmatize(word: String, postag: String): String {
        val lemma: String
        // val keys = getDictKeys(word, postag)
        // lookup lemma as value of the map

        val entries = trie.word(word.lowercase()).filter { it.postag == postag && !it.isLemma }
        val lemmas = entries.mapNotNull { it.lemma }

        lemma = if (lemmas.isNotEmpty()) {
            lemmas[0].node.calcWord()
        } else {
            "O"
        }
        return lemma
    }

    /**
     * Lookup every lemma for a word,pos tag in a dictionary. Outputs "O" if not
     * found.
     *
     * @param word
     * the token
     * @param postag
     * the postag
     * @return every lemma
     */
//    private fun getAllLemmas(word: String, postag: String): List<String> {
//        val lemmasList: MutableList<String> = ArrayList()
//        val keys = getDictKeys(word, postag)
//        // lookup lemma as value of the map
//        val keyValues = dictMap[keys]
//        if (keyValues != null && !keyValues.isEmpty()) {
//            lemmasList.addAll(keyValues)
//        } else {
//            lemmasList.add("O")
//        }
//        return lemmasList
//    }
}