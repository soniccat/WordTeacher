package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.okio.newLineSize
import okio.FileSystem
import okio.Path
import okio.utf8Size
import opennlp.tools.lemmatizer.Lemmatizer
import java.io.*
import java.util.*

class MyLemmatizer(
    private val lemmatizerUseBlock: ( (InputStream) -> Unit ) -> Unit,
    private val indexPath: Path,
    private val fileSystem: FileSystem
) : Lemmatizer {
    private val elemRegexp = "\t".toRegex()
    private lateinit var index: MyLemmatizerIndex

    fun load() {
        val anIndex = MyLemmatizerIndex(
            indexPath,
            fileSystem
        )

        if (anIndex.isEmpty()) {
            fillIndex(anIndex)
            anIndex.save()
        }

        index = anIndex
    }

    fun fillIndex(anIndex: MyLemmatizerIndex) = lemmatizerUseBlock { stream ->
        val breader = BufferedReader(
            InputStreamReader(stream)
        )
        var line: String

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

    override fun lemmatize(tokens: Array<String>, postags: Array<String>): Array<String> {
        val lemmas: MutableList<String> = ArrayList()
        for (i in tokens.indices) {
            lemmas.add(this.lemmatize(tokens[i], postags[i]))
        }
        return lemmas.toTypedArray()
    }

    override fun lemmatize(tokens: List<String>, posTags: List<String>): List<List<String>> {
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
        var resultLemma: String = NLPConstants.UNKNOWN_LEMMA

        lemmatizerUseBlock { stream ->
            index.offset(word)?.let { offset ->
                stream.skip(offset)
                val breader = BufferedReader(
                    InputStreamReader(stream)
                )

                while (true) {
                    val line = breader.readLine() ?: break
                    val elems = line.split(elemRegexp).toTypedArray()
                    if (elems[0] != word) {
                        break
                    }
                    if (elems[1] == postag) {
                        resultLemma = elems[2]
                        break
                    }
                }
            }
        }

        return resultLemma
    }
}
