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
    private val lemmatizerResourceReader: ( (InputStream) -> Unit ) -> Unit,
    private val nlpPath: Path,
    private val fileSystem: FileSystem
) : Lemmatizer {
    // first we need to decompress bundled raw lemmatizer as a raw resource requires StreamingZipInflater
    // and its skip operation is very expensive
    private val unzippedLemmatizerPath = nlpPath.div("unzipped_lemmatizer")
    private val indexPath = nlpPath.div("unizppedlemmatizer_index")

    private val elemRegexp = "\t".toRegex()
    private lateinit var index: MyLemmatizerIndex

    fun load() {
        createUnzippedLemmatizerIfNeeded()

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

    private fun createUnzippedLemmatizerIfNeeded() {
        if (fileSystem.exists(unzippedLemmatizerPath)) {
            return
        }

        val inProgressPath = nlpPath.div("unzipped_lemmatizer_in_progress")
        if (fileSystem.exists(inProgressPath)) {
            fileSystem.delete(inProgressPath)
        }

        lemmatizerResourceReader { stream ->
            val byteArray = ByteArray(DEFAULT_BUFFER_SIZE)
            fileSystem.write(inProgressPath) {
                while (stream.read(byteArray) != -1) {
                    write(byteArray)
                }
            }
        }

        fileSystem.atomicMove(inProgressPath, unzippedLemmatizerPath)
    }

    private fun fillIndex(anIndex: MyLemmatizerIndex) = fileSystem.read(unzippedLemmatizerPath) {
        var line = ""

        try {
            var lastWord = ""
            var offset = 0L
            while (readUtf8Line()?.also { line = it } != null) {
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
        return RandomAccessFile(unzippedLemmatizerPath.toFile(), "r").use { file ->
            var resultLemma: String = NLPConstants.UNKNOWN_LEMMA
            index.offset(word)?.let { offset ->
                file.channel.position(offset)

                while (true) {
                    val line = file.readLine() ?: break
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

            resultLemma
        }
    }
}
