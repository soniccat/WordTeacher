package com.aglushkov.wordteacher.shared.dicts.dsl

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.Language
import com.aglushkov.wordteacher.shared.general.StringReader
import com.aglushkov.wordteacher.shared.general.extensions.trimNonLetterNonDigit
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.WordTeacherWordBuilder
import com.aglushkov.wordteacher.shared.model.fromString
import com.aglushkov.wordteacher.shared.repository.config.Config
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

    private fun readHeader() {
        var headerLineCount = 10
        fileSystem.read(path) {
            while (!exhausted() && headerLineCount > 0) {
                readUtf8Line()?.let { line ->
                    if (line.isNotEmpty() && line.first() == '#') {
                        readHeader(line)
                    }
                } ?: break

                --headerLineCount
            }
        }
    }

    private fun fillIndex(index: DslIndex) {
        fileSystem.read(path) {
            var offset = 0L
            var line = readUtf8Line()
            while (line != null) {
                if (line.isNotEmpty()) {
                    val firstChar = line.first()
                    when (firstChar) {
                        '#' -> readHeader(line)
                        '-', '\n', '\t' -> {}
                        else -> {
                            val nextLine = readUtf8Line()
                            val partOfSpeech = nextLine?.let { firstLine ->
                                val stringBuilder = StringBuilder()
                                stringReader.read(firstLine) {
                                    skip()
                                    while (!isEnd()) {
                                        if (char == '[' && !isCharEscaped) {
                                            val isCloseTag = nextChar == '/'
                                            readTag(isCloseTag)
                                        } else {
                                            stringBuilder.append(readChar())
                                        }
                                    }
                                }
                                WordTeacherWord.PartOfSpeech.fromString(stringBuilder.toString().trimNonLetterNonDigit())
                            }

                            index.add(line, partOfSpeech, offset)
                            if (nextLine != null) {
                                offset += nextLine.utf8Size() + 1
                            }
                        }
                    }
                }

                offset += line.utf8Size() + 1
                line = readUtf8Line()
            }
        }
    }

    override suspend fun define(word: String): List<WordTeacherWord> {
        return dslIndex.indexEntry(word)?.let { indexEntry ->
            define(word, indexEntry)
        } ?: emptyList()
    }

    override suspend fun define(word: String, indexEntry: Dict.Index.Entry): List<WordTeacherWord> {
        wordTeacherWordBuilder.clear()
        wordTeacherWordBuilder.setWord(word)

        val pos = indexEntry.indexValue as Long
        fileSystem.read(path) {
            skip(pos + word.length + 1)
            var line = readUtf8Line()
            var isFirstLine = true
            while (line != null && line.isNotEmpty() && line.first() == '\t') {
                readWordLine(line, isFirstLine, wordTeacherWordBuilder)
                line = readUtf8Line()
                isFirstLine = false
            }
        }

        val wordTeacherWord = wordTeacherWordBuilder.build()
        return if (wordTeacherWord != null) {
            listOf(wordTeacherWord)
        } else {
            emptyList()
        }
    }

    private fun readWordLine(line: String, isFirstLine: Boolean, builder: WordTeacherWordBuilder) {
        var isDef = false
        var isExample = false
        var isTranscription = false
        val value = StringBuilder()
        var firstLineValue: StringBuilder? = null

        stringReader.read(line) {
            skip()
            var tag: String? = null
            while (!isEnd()) {
                if (char == '[' && !isCharEscaped) {
                    val isCloseTag = nextChar == '/'
                    tag = readTag(isCloseTag)

                    if (isCloseTag) {
                        if (tag == "trn" || tag == "ex" || tag == "t") {
                            break
                        }
                    } else {
                        when (tag) {
                            "trn" -> isDef = true
                            "ex" -> isExample = true
                            "t" -> isTranscription = true
                        }
                    }
                } else {
                    val ch = readChar()
                    if (isDef || isExample || isTranscription) {
                        value.append(ch)
                    } else if (isFirstLine) {
                        if (firstLineValue == null) {
                            firstLineValue = StringBuilder()
                        }
                        firstLineValue?.append(ch)
                    }
                }
            }
        }

        if (value.isNotEmpty() || firstLineValue?.isNotEmpty() == true) {
            if (isTranscription) {
                builder.setTranscription(value.toString())
            } else if (isDef) {
                builder.addDefinition(value.toString())
            } else if(isExample) {
                builder.addExample(value.toString())
            } else if (isFirstLine) {
                val partOfSpeech = WordTeacherWord.PartOfSpeech.fromString(firstLineValue.toString().trimNonLetterNonDigit())
                wordTeacherWordBuilder.startPartOfSpeech(partOfSpeech)
            }
        }
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
            skip(1)
            val namePos = pos
            val endNamePos = readWord()

            if (char == '\t') {
                skip(1)
                if (char == '"') {
                    skip(1)
                    val valuePos = pos
                    val endValuePos = readUntil('"')

                    val nameString = line.substring(namePos, endNamePos)
                    val valueString = line.substring(valuePos, endValuePos)
                    when (nameString) {
                        "NAME" -> name = valueString
                        "INDEX_LANGUAGE" -> fromLang = Language.parse(valueString)
                        "CONTENTS_LANGUAGE" -> toLang = Language.parse(valueString)
                    }
                }
            }
        }
    }
}

private const val DSL_INDEX_SUFFIX = "_index"