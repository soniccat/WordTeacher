package com.aglushkov.wordteacher.shared.model.nlp

import opennlp.tools.lemmatizer.Lemmatizer
import java.io.*
import java.util.*

class MyLemmatizer : Lemmatizer {
    /**
     * The hashmap containing the dictionary.
     */
    private val dictMap: MutableMap<List<String>, List<String>> = HashMap()

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
        while (breader.readLine().also { line = it } != null) {
            val elems = line.split("\t".toRegex()).toTypedArray()
            val lemmas = elems[2].split("#".toRegex()).toTypedArray()
            dictMap[Arrays.asList(elems[0], elems[1])] = Arrays.asList(*lemmas)
        }
    }

    /**
     * Get the Map containing the dictionary.
     *
     * @return dictMap the Map
     */
    fun getDictMap(): Map<List<String>, List<String>> {
        return dictMap
    }

    /**
     * Get the dictionary keys (word and postag).
     *
     * @param word
     * the surface form word
     * @param postag
     * the assigned postag
     * @return returns the dictionary keys
     */
    private fun getDictKeys(word: String, postag: String): List<String> {
        return ArrayList(Arrays.asList(word.lowercase(Locale.getDefault()), postag))
    }

    override fun lemmatize(tokens: Array<String>, postags: Array<String>): Array<String> {
        val lemmas: MutableList<String> = ArrayList()
        for (i in tokens.indices) {
            lemmas.add(this.lemmatize(tokens[i], postags[i]))
        }
        return lemmas.toTypedArray()
    }

    override fun lemmatize(tokens: List<String>, posTags: List<String>): List<List<String>> {
        val allLemmas: MutableList<List<String>> = ArrayList()
        for (i in tokens.indices) {
            allLemmas.add(getAllLemmas(tokens[i], posTags[i]))
        }
        return allLemmas
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
        val keys = getDictKeys(word, postag)
        // lookup lemma as value of the map
        val keyValues = dictMap[keys]
        lemma = if (keyValues != null && !keyValues.isEmpty()) {
            keyValues[0]
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
    private fun getAllLemmas(word: String, postag: String): List<String> {
        val lemmasList: MutableList<String> = ArrayList()
        val keys = getDictKeys(word, postag)
        // lookup lemma as value of the map
        val keyValues = dictMap[keys]
        if (keyValues != null && !keyValues.isEmpty()) {
            lemmasList.addAll(keyValues)
        } else {
            lemmasList.add("O")
        }
        return lemmasList
    }
}