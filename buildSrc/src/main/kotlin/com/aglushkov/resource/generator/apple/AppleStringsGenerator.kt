/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.aglushkov.resource.generator.apple

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.aglushkov.resource.generator.KeyType
import com.aglushkov.resource.generator.ObjectBodyExtendable
import com.aglushkov.resource.generator.StringsGenerator
import com.aglushkov.resource.generator.apple.AppleMRGenerator.Companion.BUNDLE_PROPERTY_NAME
import org.apache.commons.text.StringEscapeUtils
import org.gradle.api.file.FileTree
import java.io.File

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class AppleStringsGenerator(
    stringsFileTree: FileTree,
    private val baseLocalizationRegion: String
) : StringsGenerator(
    stringsFileTree = stringsFileTree
), ObjectBodyExtendable by AppleGeneratorHelper() {

    override fun getClassModifiers(): Array<KModifier> = arrayOf(KModifier.ACTUAL)

    override fun getPropertyModifiers(): Array<KModifier> = arrayOf(KModifier.ACTUAL)

    override fun getPropertyInitializer(key: String): CodeBlock? {
        return CodeBlock.of("StringResource(resourceId = %S, bundle = $BUNDLE_PROPERTY_NAME)", key)
    }

    override fun generateResources(
        resourcesGenerationDir: File,
        language: String?,
        strings: Map<KeyType, String>
    ) {
        val resDirName = when (language) {
            null -> "Base.lproj"
            else -> "$language.lproj"
        }

        val resDir = File(resourcesGenerationDir, resDirName)
        val localizableFile = File(resDir, "Localizable.strings")
        resDir.mkdirs()

        val content = strings.mapValues { (_, value) ->
            convertXmlStringToAppleLocalization(value)
        }.map { (key, value) ->
            "\"$key\" = \"$value\";"
        }.joinToString("\n")
        localizableFile.writeText(content)

        if (language == null) {
            val regionDir = File(resourcesGenerationDir, "$baseLocalizationRegion.lproj")
            regionDir.mkdirs()
            val regionFile = File(regionDir, "Localizable.strings")
            regionFile.writeText(content)
        }
    }

    private fun convertXmlStringToAppleLocalization(input: String): String {
        val xmlDecoded = StringEscapeUtils.unescapeXml(input)
        return xmlDecoded.replace("\n", "\\n")
            .replace("\"", "\\\"")
    }
}
