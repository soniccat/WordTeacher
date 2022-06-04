package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.okio.newLineSize
import com.aglushkov.wordteacher.shared.repository.dict.Trie
import com.aglushkov.wordteacher.shared.repository.dict.TrieNode
import okio.FileSystem
import okio.Path
import okio.utf8Size
import opennlp.tools.lemmatizer.Lemmatizer
import java.io.*
import java.util.*

class MyLemmatizer(
    private val stream: InputStream,
    private val indexPath: Path,
    private val fileSystem: FileSystem
) : Lemmatizer {
    private lateinit var index: MyLemmatizerIndex

    fun load() {
        val anIndex = MyLemmatizerIndex(
            stream,
            indexPath,
            fileSystem
        )

        if (anIndex.isEmpty()) {
            fillIndex(anIndex)
            anIndex.save()
        }

        index = anIndex
    }

    fun fillIndex(anIndex: MyLemmatizerIndex) {
        val breader = BufferedReader(
            InputStreamReader(stream)
        )
        var line: String

        val elemRegexp = "\t".toRegex()
        val lemmasRegexp = "#".toRegex()

        try {
            var lastWord = ""
            var offset = 0L
            while (breader.readLine().also { line = it } != null) {
                val elems = line.split(elemRegexp).toTypedArray()
                if (lastWord != elems[0]) {
                    lastWord = elems[0]
                    anIndex.add(elems[0], offset)
                }

                offset += line.utf8Size() + newLineSize
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
        val lemma: String = ""

//        val entries = trie.word(word.lowercase()).filter { it.postags.contains(postag) && !it.isLemma }
//        val lemmas = entries.mapNotNull { it.lemma }
//
//        lemma = if (lemmas.isNotEmpty()) {
//            lemmas[0].node.calcWord()
//        } else {
//            "O"
//        }
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