package com.aglushkov.wordteacher.android_app

import com.aglushkov.wordteacher.shared.features.article.vm.ArticleRouter
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesRouter
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetRouter
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsRouter
import com.aglushkov.wordteacher.shared.features.settings.vm.SettingsRouter

interface Router: ArticlesRouter,
    ArticleRouter,
    CardSetsRouter,
    CardSetRouter