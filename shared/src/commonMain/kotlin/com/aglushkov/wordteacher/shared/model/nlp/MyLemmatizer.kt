package com.aglushkov.wordteacher.shared.model.nlp

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.okio.newLineSize
import com.aglushkov.wordteacher.shared.general.okio.writeToWithLockFile
import okio.BufferedSource
import okio.FileHandle
import okio.FileSystem
import okio.Path
import okio.Sink
import okio.Source
import okio.buffer
import okio.utf8Size
import java.io.*
import java.util.*

//private class FilePool(val filePath: String) {
//    private val maxCount = 3
//    private val availableFiles = mutableListOf<File>()
//    private val busyFiles = mutableListOf<File>()
//
//    fun take(): File {
//    }
//
//}

class MyLemmatizer(
    private val source: Source,
    private val nlpPath: Path,
    private val fileSystem: FileSystem
) : NLPLemmatizer {
    // first we need to decompress bundled raw lemmatizer as a raw resource requires StreamingZipInflater
    // and its skip operation is very expensive
    private val unzippedLemmatizerPath = nlpPath.div("unzipped_lemmatizer")
    private val indexPath = nlpPath.div("unizppedlemmatizer_index")
    private var randomAccessFile: FileHandle? = null//RandomAccessFile? = null
    private var randomAccessFileSource: BufferedSource? = null

    private val elemRegexp = "\t".toRegex()
    private lateinit var index: MyLemmatizerIndex

    fun load() {
        fileSystem.writeToWithLockFile(source, unzippedLemmatizerPath)

        val anIndex = MyLemmatizerIndex(
            indexPath,
            fileSystem
        )

        if (anIndex.isEmpty()) {
            fillIndex(anIndex)
            anIndex.save()
        }

        index = anIndex
        randomAccessFile = fileSystem.openReadOnly(unzippedLemmatizerPath) //RandomAccessFile(unzippedLemmatizerPath.toFile(), "r")
        randomAccessFileSource = randomAccessFile?.source(0L)?.buffer()
    }

    private fun fillIndex(anIndex: MyLemmatizerIndex) = fileSystem.read(unzippedLemmatizerPath) {
        var line = ""

        try {
            var lastWord = ""
            var offset = 0
            while (readUtf8Line()?.also { line = it } != null) {
                val elems = line.split(elemRegexp).toTypedArray()
                if (lastWord != elems[0]) {
                    lastWord = elems[0]
                    anIndex.add(elems[0], offset)
                }
                offset += line.utf8Size().toInt() + newLineSize.toInt()
            }
        } catch (t: Throwable) {
            Logger.e(t.message.orEmpty())
        }
    }

    override fun lemmatize(tokens: List<String>, postags: List<String>): Array<String> {
        val lemmas: MutableList<String> = ArrayList()
        for (i in tokens.indices) {
            lemmas.add(this.lemmatize(tokens[i], postags[i]))
        }
        return lemmas.toTypedArray()
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
        val safeRandomAccessFile = randomAccessFile ?: return NLPConstants.UNKNOWN_LEMMA
        val safeSource = randomAccessFileSource ?: return NLPConstants.UNKNOWN_LEMMA
        var resultLemma: String = NLPConstants.UNKNOWN_LEMMA
        index.offset(word)?.let { offset ->
            synchronized(safeRandomAccessFile) { // TODO: consider using file pool not to wait here
                safeRandomAccessFile.reposition(safeSource, offset.toLong())

                while (true) {
                    val line = safeSource.readUtf8Line() ?: break
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
