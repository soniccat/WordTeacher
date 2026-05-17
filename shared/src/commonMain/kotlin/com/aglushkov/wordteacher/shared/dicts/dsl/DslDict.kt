package com.aglushkov.wordteacher.shared.dicts.dsl

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.Language
import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.StringReader
import com.aglushkov.wordteacher.shared.general.UNICODE_INVISIBLE_SPACE
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.extensions.trimNonLetterNonDigit
import com.aglushkov.wordteacher.shared.general.okio.newLineSize
import com.aglushkov.wordteacher.shared.general.v
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.WordTeacherWordBuilder
import com.aglushkov.wordteacher.shared.model.fromString
import com.aglushkov.wordteacher.shared.repository.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.utf8Size

class DslDict(
    override val path: Path,
    private val fileSystem: FileSystem
): Dict {
    override var type = Config.Type.Local
    override var name = ""
    override var fromLang = Language.EN
    override var toLang = Language.EN

    private val stringReader = StringReader()
    private val wordTeacherWordBuilder = WordTeacherWordBuilder()
    private lateinit var dslIndex: DslIndex

    override val index: Dict.Index
        get() = dslIndex

    override suspend fun load() {
        val dslIndex = DslIndex(this, (path.toString() + DSL_INDEX_SUFFIX).toPath(), fileSystem)
        if (dslIndex.isEmpty()) {
            fillIndex(dslIndex)
            dslIndex.save()
        } else {
            readHeader()
        }

        this.dslIndex = dslIndex
    }

    fun validate(): Boolean {
        fromLang = Language.UNKNOWN
        toLang = Language.UNKNOWN
        readHeader()

        return name.isNotEmpty() || fromLang != Language.UNKNOWN || toLang != Language.UNKNOWN
    }

    private fun readHeader() {
        var headerLineCount = 10
        fileSystem.read(path) {
            while (!exhausted() && headerLineCount > 0) {
                readUtf8Line()?.let { line ->
                    if (line.isNotEmpty() && (line.first() == '#' || line.length > 2 && line.first() == UNICODE_INVISIBLE_SPACE && line[1] == '#' )) {
                        readHeader(line)
                    }
                } ?: break

                --headerLineCount
            }
        }
    }

    private fun fillIndex(index: DslIndex) {
        fileSystem.read(path) {
            var offset = 0
            var line = readUtf8Line()
            var readLine: String? = null

            while (line != null || readLine != null) {
                val resultLine: String = readLine ?: (line ?: "")
                if (readLine != null) {
                    readLine = null
                }

                if (resultLine.isNotEmpty()) {
                    val firstChar = resultLine.first()
                    when (firstChar) {
                        '#', UNICODE_INVISIBLE_SPACE -> readHeader(resultLine)
                        '-', '\n', '\t' -> {}
                        else -> {
                            wordTeacherWordBuilder.clear()
                            wordTeacherWordBuilder.setWord(resultLine)

                            val (readBytes, nl) = readWord(wordTeacherWordBuilder)
                            val word = wordTeacherWordBuilder.build()

                            if (word?.definitions?.isNotEmpty() == true) {
                                val partOfSpeeches = word.definitions.map { it.key }
                                index.add(word.word, partOfSpeeches, offset)
                            }

                            readLine = nl
                            offset += (readBytes + resultLine.utf8Size() + newLineSize).toInt()
                        }
                    }
                }

                if (readLine == null) {
                    offset += (resultLine.utf8Size() + newLineSize).toInt()
                    line = readUtf8Line()
                }
            }
        }
    }

    override suspend fun define(words: List<String>): List<WordTeacherWord> {
        return words.map { word ->
            dslIndex.indexEntry(word)?.let { indexEntry ->
                define(word, indexEntry)
            } ?: emptyList()
        }.flatten()
    }

    override suspend fun define(word: String, indexEntry: Dict.Index.Entry): List<WordTeacherWord> {
        return withContext(Dispatchers.IO) {
            val builder = WordTeacherWordBuilder()
            builder.setWord(indexEntry.word)

            val pos = indexEntry.indexValue as Int
            fileSystem.read(path) {
                skip(pos + word.length + newLineSize)
                readWord(builder)
            }

            val wordTeacherWord = builder.build()
            if (wordTeacherWord != null) {
                listOf(wordTeacherWord)
            } else {
                emptyList()
            }
        }
    }

    private fun BufferedSource.readWord(wordTeacherWordBuilder: WordTeacherWordBuilder): Pair<Int, String?> {
        var readBytes = 0
        var line = readUtf8Line()
        while (line != null && line.isNotEmpty() && line.first() == '\t') {
            readWordLine(line, wordTeacherWordBuilder)

            readBytes += (line.utf8Size() + newLineSize).toInt()
            line = readUtf8Line()
        }

        // don't add size of the last read line to readBytes on purpose
        // that should do a caller if it's required
        return Pair(readBytes, line)
    }

    private fun readWordLine(line: String, builder: WordTeacherWordBuilder) {
        var isDef = false
        var isExample = false
        var isTranscription = false
        var isLabel = false
        val label = StringBuilder()
        val value = StringBuilder()

        stringReader.read(line) {
            skip()
            var tag: String? = null
            while (!isEnd()) {
                if (char == '[' && !isCharEscaped) {
                    val isCloseTag = nextChar == '/'
                    tag = readTag(isCloseTag)

                    if (isCloseTag) {
                        if (tag == "p") {
                            isLabel = false
                            if (label.isNotEmpty()) {
                                if (tryToAddPartOfSpeech(label.toString().trimNonLetterNonDigit(), builder)) {
                                    label.clear()
                                } else {
                                    builder.addLabel(label.toString())
                                    label.clear()
                                }
                            }
                        } else if (tag == "trn" || tag == "ex" || tag == "t") {
                            break
                        }
                    } else {
                        when (tag) {
                            "trn" -> {
                                value.clear()
                                isDef = true
                            }
                            "ex" -> {
                                value.clear()
                                isExample = true
                            }
                            "t" -> {
                                value.clear()
                                isTranscription = true
                            }
                            "p" -> {
                                if (!isExample) {
                                    value.clear()
                                    isLabel = true
                                }
                            }
                        }
                    }
                } else {
                    val ch = readChar()
                    if (isLabel && !isExample) {
                        label.append(ch)
                    } else /*if (isDef || isExample || isTranscription)*/ {
                        value.append(ch)
                    }
                }
            }
        }

        if (value.isNotEmpty()) {
            val text = value.toString()
            if (isTranscription) {
                builder.setTranscription(text)
            } else if (isDef) {
                builder.addDefinition(text)
            } else if(isExample) {
                builder.addExample(text)
            } else if (text.lowercase().startsWith("syn")) {
                builder.setIsSynonimsBlock(true)
            } else {
                val isHandled = tryToAddPartOfSpeech(text.trimNonLetterNonDigit(), builder)
                if (!isHandled && text.trimNonLetterNonDigit().isNotEmpty()) {
                    builder.addText(text)
                }
            }
        }
    }

    private fun tryToAddPartOfSpeech(
        text: String,
        builder: WordTeacherWordBuilder,
    ): Boolean {
        val partOfSpeech = WordTeacherWord.PartOfSpeech.fromString(text.trimNonLetterNonDigit())
        if (partOfSpeech != WordTeacherWord.PartOfSpeech.Undefined) {
            builder.startPartOfSpeech(partOfSpeech)
            return true
        }

        return false
    }

    private fun StringReader.readTag(isClose: Boolean): String? {
        val startTagPos = readUntil('[', needSkip = true)
        val endTagPos = readUntil(']')

        val tag = string.substring(startTagPos + if (isClose) 1 else 0, endTagPos)
        if (!isEnd()) {
            skip()
        }

        return if (tag.isNotEmpty()) {
            tag
        } else {
            null
        }
    }

    private fun readHeader(line: String) {
        stringReader.read(line) {
            skip(1, withInvisibleSpace = true)
            val namePos = pos
            val endNamePos = readWord()

            if (char == '\t' || char == ' ') {
                skip(1)
                if (char == '"') {
                    skip(1)
                    val valuePos = pos
                    val endValuePos = readUntil('"')

                    val nameString = line.substring(namePos, endNamePos)
                    val valueString = line.substring(valuePos, endValuePos)
                    when (nameString) {
                        "NAME" -> name = valueString
                        "INDEX_LANGUAGE" -> fromLang = Language.parse(valueString.lowercase())
                        "CONTENTS_LANGUAGE" -> toLang = Language.parse(valueString.lowercase())
                    }
                }
            }
        }
    }
}

private const val DSL_INDEX_SUFFIX = "_index"