package com.aglushkov.wordteacher.android_app.di

import android.content.Context
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.aglushkov.wordteacher.android_app.BuildConfig
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.android_app.features.add_article.ContentProviderRepository
import com.aglushkov.wordteacher.android_app.features.add_article.toArticleContentExtractor
import com.aglushkov.wordteacher.android_app.features.settings.FileSharerRepository
import com.aglushkov.wordteacher.android_app.features.settings.toFileSharer
import com.aglushkov.wordteacher.android_app.general.crypto.SecureCodecBuilder
import com.aglushkov.wordteacher.android_app.helper.FileOpenControllerImpl
import com.aglushkov.wordteacher.android_app.helper.GoogleAuthControllerImpl
import com.aglushkov.wordteacher.android_app.helper.VKAuthControllerImpl
import com.aglushkov.wordteacher.shared.di.*
import com.aglushkov.wordteacher.shared.features.add_article.vm.ArticleContentExtractor
import com.aglushkov.wordteacher.shared.features.add_article.vm.toArticleContentExtractor
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.settings.vm.FileSharer
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.auth.GoogleAuthController
import com.aglushkov.wordteacher.shared.general.auth.VKAuthController
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import com.aglushkov.wordteacher.shared.general.okio.writeTo
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticleParserRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseDriverFactory
import com.aglushkov.wordteacher.shared.repository.db.FREQUENCY_DB_NAME
import com.aglushkov.wordteacher.shared.repository.db.FREQUENCY_DB_NAME_TMP
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyDatabase
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.dict.DslDictValidator
import com.aglushkov.wordteacher.shared.repository.dict.OnNewDictAddedHandler
import com.aglushkov.wordteacher.shared.res.MR
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import okio.FileSystem
import dagger.Module
import dagger.Provides
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import okio.source

@Module(includes = [SharedAppModule::class])
class AppModule {

    @IsDebug
    @AppComp
    @Provides
    fun isDebug(): Boolean = BuildConfig.DEBUG

    @Platform
    @AppComp
    @Provides
    fun platform(): String = "android"

    @BasePath
    @AppComp
    @Provides
    fun basePath(
        context: Context
    ): Path = context.filesDir.absolutePath.toPath()

    @ApiBaseUrl
    @AppComp
    @Provides
    fun apiBaseUrl(
        context: Context
    ): String = context.getString(MR.strings.api_base_url.resourceId)

    @AppComp
    @Provides
    fun settings(
        context: Context
    ): FlowSettings {
        return DataStoreSettings(
            PreferenceDataStoreFactory.create {
                context.dataStoreFile("settings.preferences_pb")
            }
        )
    }

    @AppComp
    @Provides
    fun appInfo(
        @Platform platform: String,
    ): AppInfo = AppInfo(BuildConfig.VERSION_NAME, platform)

    // TODO: replace with bind
    @AppComp
    @Provides
    fun googleAuthRepositoryImpl(
        context: Context
    ): GoogleAuthControllerImpl =
        GoogleAuthControllerImpl(context.getString(R.string.default_web_client_id))

    @AppComp
    @Provides
    fun googleAuthController(
        impl: GoogleAuthControllerImpl
    ): GoogleAuthController = impl

    @AppComp
    @Provides
    fun vkAuthControllerImpl(): VKAuthControllerImpl = VKAuthControllerImpl()

    // TODO: replace with bind
    @AppComp
    @Provides
    fun vkAuthController(
        impl: VKAuthControllerImpl
    ): VKAuthController = impl

    @AppComp
    @Provides
    fun databaseFactory(context: Context) = DatabaseDriverFactory(context)

    @AppComp
    @Provides
    fun nlpCore(context: Context, fileSystem: FileSystem): NLPCore {
        val nlpIndexPath = context.filesDir.absolutePath.toPath().div("nlp")
        if (!fileSystem.exists(nlpIndexPath)) {
            fileSystem.createDirectory(nlpIndexPath)
        }
        return NLPCore(
            context.resources,
            R.raw.en_sent,
            R.raw.en_token,
            R.raw.en_pos_maxent,
            R.raw.en_lemmatizer_dict,
            R.raw.en_chunker,
            nlpIndexPath,
            fileSystem
        )
    }

    @AppComp
    @Provides
    fun secureCodec(): SecureCodec {
        return SecureCodecBuilder().build()
    }

    @AppComp
    @Provides
    fun contentExtractors(
        context: Context,
    ): Array<ArticleContentExtractor> {
        return arrayOf(
            ArticleParserRepository().toArticleContentExtractor(),
            ContentProviderRepository(context).toArticleContentExtractor(),
        )
    }

    @AppComp
    @Provides
    fun fileSharer(context: Context): FileSharer {
        return FileSharerRepository(context).toFileSharer()
    }

    @WordFrequencyPreparer
    @AppComp
    @Provides
    fun wordFrequencyPreparer(context: Context, fileSystem: FileSystem): () -> Path {
        return {
            // copy db in database folder if needed
            val dbPath = context.getDatabasePath("word_frequency.db").toOkioPath()
            if (!fileSystem.exists(dbPath)) {
                fileSystem.write(dbPath) {
                    MR.assets.word_frequency.getInputStream(context).source().writeTo(this)
                }
            }
            "".toPath()
        }
    }

    @WordFrequencyFileOpener
    @AppComp
    @Provides
    fun wordFrequencyFileOpener(
        context: Context,
        wordFrequencyDB: WordFrequencyDatabase,
        mainDB: AppDatabase,
    ): FileOpenController {
        val tmpDestinationPath = context.getDatabasePath(FREQUENCY_DB_NAME_TMP).toOkioPath()
        val dstPath = context.getDatabasePath(FREQUENCY_DB_NAME).toOkioPath()
        return FileOpenControllerImpl(
            "WordFrequencyFileOpener",
            listOf("application/octet-stream"),
            tmpDestinationPath,
            dstPath,
            wordFrequencyDB.Validator(),
            FileOpenCompositeSuccessHandler(
                listOf(
                    wordFrequencyDB.UpdateHandler(),
                    mainDB.WordFrequencyUpdateHandler()
                )
            )
        )
    }

    @DslFileOpener
    @AppComp
    @Provides
    fun dslFileOpener(
        @DictPath dictPath: Path,
        context: Context,
        fileSystem: FileSystem,
        dictRepository: DictRepository
    ): FileOpenController {
        val tmpDestinationPath = context.cacheDir.toOkioPath()
        return FileOpenControllerImpl(
            "DslFileOpener",
            listOf("application/octet-stream"),
            tmpDestinationPath,
            dictPath,
            DslDictValidator(fileSystem),
            FileOpenCompositeSuccessHandler(
                listOf(
                    OnNewDictAddedHandler(dictRepository)
                )
            )
        )
    }

    // Features

    @AppComp
    @Provides
    fun cardSetsFeatures(): CardSetsVM.Features {
        return CardSetsVM.Features(
            canImportCardSetFromJson = false,
        )
    }
}