package com.aglushkov.wordteacher.android_app.di

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.aglushkov.wordteacher.android_app.BuildConfig
import com.aglushkov.wordteacher.android_app.R
import com.aglushkov.wordteacher.android_app.features.add_article.ContentProviderRepository
import com.aglushkov.wordteacher.android_app.features.add_article.toArticleContentExtractor
import com.aglushkov.wordteacher.android_app.features.settings.FileSharerRepository
import com.aglushkov.wordteacher.android_app.features.settings.toFileSharer
import com.aglushkov.wordteacher.android_app.general.crypto.SecureCodecBuilder
import com.aglushkov.wordteacher.android_app.helper.EmailOpenerImpl
import com.aglushkov.wordteacher.android_app.helper.FileOpenControllerImpl
import com.aglushkov.wordteacher.android_app.helper.GoogleAuthControllerImpl
import com.aglushkov.wordteacher.android_app.helper.VKAuthControllerImpl
import com.aglushkov.wordteacher.android_app.helper.WebLinkOpenerImpl
import com.aglushkov.wordteacher.android_app.helper.YandexAuthControllerImpl
import com.aglushkov.wordteacher.shared.analytics.AnalyticEngine
import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.analytics.AppMetricaEngine
import com.aglushkov.wordteacher.shared.di.*
import com.aglushkov.wordteacher.shared.dicts.wordlist.WORDLIST_EXTENSION
import com.aglushkov.wordteacher.shared.features.add_article.vm.ArticleContentExtractor
import com.aglushkov.wordteacher.shared.features.add_article.vm.toArticleContentExtractor
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsVM
import com.aglushkov.wordteacher.shared.features.settings.vm.FileSharer
import com.aglushkov.wordteacher.shared.general.*
import com.aglushkov.wordteacher.shared.general.auth.GoogleAuthController
import com.aglushkov.wordteacher.shared.general.auth.VKAuthController
import com.aglushkov.wordteacher.shared.general.auth.YandexAuthController
import com.aglushkov.wordteacher.shared.general.crypto.SecureCodec
import com.aglushkov.wordteacher.shared.general.okio.writeTo
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.shared.model.nlp.NLPCore
import com.aglushkov.wordteacher.shared.repository.article.ArticleParserRepository
import com.aglushkov.wordteacher.shared.repository.article.ArticlesRepository
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import com.aglushkov.wordteacher.shared.repository.db.AppDatabase
import com.aglushkov.wordteacher.shared.repository.db.DatabaseDriverFactory
import com.aglushkov.wordteacher.shared.repository.db.FREQUENCY_DB_NAME
import com.aglushkov.wordteacher.shared.repository.db.FREQUENCY_DB_NAME_TMP
import com.aglushkov.wordteacher.shared.repository.db.WordFrequencyDatabase
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.dict.DslDictValidator
import com.aglushkov.wordteacher.shared.repository.dict.OnNewDictAddedHandler
import com.aglushkov.wordteacher.shared.repository.space.SpaceAuthRepository
import com.aglushkov.wordteacher.shared.repository.toggles.ToggleRepository
import com.aglushkov.wordteacher.shared.res.MR
import com.aglushkov.wordteacher.shared.tasks.AddArticleSampleTask
import com.aglushkov.wordteacher.shared.tasks.AddCardSetSampleTask
import com.aglushkov.wordteacher.shared.tasks.ArticleSample
import com.aglushkov.wordteacher.shared.tasks.CopyDictTask
import com.aglushkov.wordteacher.shared.tasks.LoadNLPCoreTask
import com.aglushkov.wordteacher.shared.tasks.Task
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.channels.Channel
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import okio.internal.commonToUtf8String
import okio.source
import okio.use


@Module(includes = [SharedAppModule::class])
class AppModule {

    @IsDebug
    @AppComp
    @Provides
    fun isDebug(): Boolean = false//BuildConfig.DEBUG

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

    @ToggleUrl
    @AppComp
    @Provides
    fun togglesUrl(
        context: Context
    ): String = context.getString(MR.strings.toggles_url.resourceId)

    @ToggleUrl2
    @AppComp
    @Provides
    fun togglesUrl2(
        context: Context
    ): String = context.getString(MR.strings.toggles_url2.resourceId)

    @ApiBaseUrl
    @AppComp
    @Provides
    fun apiBaseUrl(
        context: Context,
        toggleRepository: ToggleRepository
    ): String {
        return if (toggleRepository.toggles.topLevelDomain == "com") {
            context.getString(MR.strings.api_base_url_com.resourceId)
        } else {
            context.getString(MR.strings.api_base_url.resourceId)
        }
    }

    @Email
    @AppComp
    @Provides
    fun email(
        context: Context
    ): String = context.getString(MR.strings.support_email.resourceId)

    @PrivacyPolicyUrl
    @AppComp
    @Provides
    fun privacyPolicy(
        context: Context
    ): String = context.getString(MR.strings.privacy_policy_url.resourceId)

    @AppComp
    @Provides
    fun settingStore(
        context: Context
    ): SettingStore {
        return SettingStore(PreferenceDataStoreFactory.create {
            context.dataStoreFile("settings.preferences_pb")
        })
    }

    @AppComp
    @ArticleSettingStore
    @Provides
    fun articleSettingStore(
        context: Context
    ): SettingStore {
        return SettingStore(PreferenceDataStoreFactory.create {
            context.dataStoreFile("article.preferences_pb")
        })
    }

    @AppComp
    @Provides
    fun appInfo(
        @Platform platform: String,
        @Email email: String,
        @PrivacyPolicyUrl privacyPolicyUrl: String,
    ): AppInfo = AppInfo(BuildConfig.VERSION_NAME, platform, email, privacyPolicyUrl)

    // TODO: replace with bind
    @AppComp
    @Provides
    fun googleAuthRepositoryImpl(
        context: Context,
        @IsDebug isDebug: Boolean,
    ): GoogleAuthControllerImpl =
        GoogleAuthControllerImpl(
            context.getString(R.string.default_web_client_id),
            isDebug
        )

    @AppComp
    @Provides
    fun googleAuthController(
        impl: GoogleAuthControllerImpl
    ): GoogleAuthController = impl

    @AppComp
    @Provides
    fun vkAuthControllerImpl(
        timeSource: TimeSource
    ): VKAuthControllerImpl = VKAuthControllerImpl(timeSource)

    @AppComp
    @Provides
    fun yandexAuthControllerImpl(
        timeSource: TimeSource
    ): YandexAuthControllerImpl = YandexAuthControllerImpl(timeSource)

    // TODO: replace with bind
    @AppComp
    @Provides
    fun vkAuthController(
        impl: VKAuthControllerImpl
    ): VKAuthController = impl

    @AppComp
    @Provides
    fun yandexAuthController(
        impl: YandexAuthControllerImpl
    ): YandexAuthController = impl

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
                    MR.assets.word_frequency_db.getInputStream(context).source().writeTo(this)
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
        analytics: Analytics,
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
                    wordFrequencyDB.UpdateHandler(analytics),
                    mainDB.WordFrequencyUpdateHandler(analytics)
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
        dictRepository: DictRepository,
        analytics: Analytics,
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
                    OnNewDictAddedHandler(dictRepository, analytics)
                )
            )
        )
    }

    @AppComp
    @Provides
    fun analyticEngines(
        appMetricaEngine: AppMetricaEngine,
    ): Array<AnalyticEngine> {
        return arrayOf(
            appMetricaEngine
        )
    }

    @AppComp
    @Provides
    fun appMetricaEngine(
        app: Application,
        toggleRepositoryProvider: Lazy<ToggleRepository>,
        spaceAuthRepository: SpaceAuthRepository,
    ): AppMetricaEngine {
        return AppMetricaEngine(
            app.getString(R.string.app_metrica_key),
            app,
            { toggleRepositoryProvider.get() },
            spaceAuthRepository
        )
    }

    @AppComp
    @Provides
    fun emailOpener(): EmailOpener {
        return EmailOpenerImpl()
    }

    @AppComp
    @Provides
    fun startupTasks(
        context: Context,
        @DictPath dictPath: Path,
        nlpCore: NLPCore,
        dictRepository: DictRepository,
        articlesRepository: ArticlesRepository,
        cardSetsRepository: CardSetsRepository,
        timeSource: TimeSource,
        fileSystem: FileSystem,
        settings: SettingStore,
    ): Array<Task> {
        return arrayOf(
            CopyDictTask(
                context.resources.openRawResource(R.raw.words)
                    .buffered(100 * 1024).source(),
                dictPath.div("words.$WORDLIST_EXTENSION"),
                fileSystem,
                dictRepository,
                BuildConfig.defaultWordlistVersion,
                settings
            ),
            LoadNLPCoreTask(nlpCore),
            AddArticleSampleTask(
                articlesRepository,
                settings
            ) {
                ArticleSample(
                    "Article Sample. CORECURSIVE #099, Code, Kickflips and Crunch Time",
                    context.resources.openRawResource(R.raw.article_sample).use {
                        it.readBytes().commonToUtf8String()
                    },
                    context.getString(MR.strings.sample_article_url.resourceId)
                )
            },
            AddCardSetSampleTask(
                settings,
                cardSetsRepository,
                timeSource
            ) {
                context.resources.openRawResource(R.raw.cardset_sample).use {
                    it.readBytes().commonToUtf8String()
                }
            }
        )
    }

    @AppComp
    @Provides
    fun webLinkOpenerImpl(): WebLinkOpenerImpl {
        return WebLinkOpenerImpl()
    }

    @AppComp
    @Provides
    fun webLinkOpener(impl: WebLinkOpenerImpl): WebLinkOpener {
        return impl
    }

    @AppComp
    @Provides
    fun audioService(
        context: Context,
    ): AudioService {
        return AudioServiceImpl(context)
    }

    // Features

    @AppComp
    @Provides
    fun cardSetsFeatures(
        @IsDebug isDebug: Boolean,
    ): CardSetsVM.Features {
        return CardSetsVM.Features(
            canImportCardSetFromJson = isDebug,
        )
    }
}
