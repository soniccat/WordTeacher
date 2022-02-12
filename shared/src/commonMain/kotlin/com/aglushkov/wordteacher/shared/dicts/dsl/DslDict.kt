package com.aglushkov.wordteacher.shared.dicts.dsl

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.Language
import com.aglushkov.wordteacher.shared.general.StringReader
import com.aglushkov.wordteacher.shared.model.WordTeacherWord
import com.aglushkov.wordteacher.shared.model.WordTeacherWordBuilder
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class DslDict(
    override val path: Path,
    private val fileSystem: FileSystem
): Dict {
    override var name = ""
    override var fromLang = Language.EN
    override var toLang = Language.EN

    private val stringReader = StringReader()
    private val wordTeacherWordBuilder = WordTeacherWordBuilder()
    private var dslIndex = DslIndex("".toPath(), fileSystem)

    override suspend fun load() {
        val dslIndex = DslIndex((path.name + DSL_INDEX_SUFFIX).toPath(), fileSystem)
        if (dslIndex.isEmpty()) {
            fillIndex(dslIndex)
            dslIndex.save()
            this.dslIndex = dslIndex
        }
    }

    private fun fillIndex(index: DslIndex) {
        fileSystem.read(path) {
            var pos = 0L
            var line = readUtf8Line()
            while (line != null) {
                if (line.isNotEmpty()) {
                    val firstChar = line.first()
                    when (firstChar) {
                        '#' -> readHeader(line)
                        '-', '\n', '\t' -> {}
                        else -> {
                            index.add(line, pos)
                        }
                    }
                }

                pos += line.length + 1
                line = readUtf8Line()
            }
        }
    }

    override suspend fun define(term: String): WordTeacherWord? {
        wordTeacherWordBuilder.clear()
        wordTeacherWordBuilder.setWord(term)
        val pos = dslIndex.get(term) ?: return null

        fileSystem.read(path) {
            skip(pos + term.length + 1)
            var line = readUtf8Line()
            while (line != null && line.isNotEmpty() && line.first() == '\t') {
                readWordLine(line, wordTeacherWordBuilder)
                line = readUtf8Line()
            }
        }

        return wordTeacherWordBuilder.build()
    }

    private fun readWordLine(line: String, builder: WordTeacherWordBuilder) {
        var isDef = false
        var isExample = false
        val value = StringBuilder()

        stringReader.read(line) {
            skip()
            var tag: String? = null
            while (!isEnd()) {
                if (char == '[') {
                    val isCloseTag = nextChar == '/'
                    tag = readTag(isCloseTag)

                    if (isCloseTag) {
                        if (tag == "trn" || tag == "ex") {
                            break;
                        }
                    } else {
                        when (tag) {
                            "trn" -> isDef = true
                            "ex" -> isExample = true
                        }
                    }
                } else {
                    val ch = readChar()
                    if (isDef || isExample) {
                        value.append(ch)
                    }
                }
            }
        }

        if (value.isNotEmpty()) {
            if (isDef) {
                builder.addDefinition(value.toString())
            } else if(isExample) {
                builder.addExample(value.toString())
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