package com.aglushkov.wordteacher.androidApp

import com.aglushkov.wordteacher.shared.features.article.vm.ArticleRouter
import com.aglushkov.wordteacher.shared.features.articles.vm.ArticlesRouter
import com.aglushkov.wordteacher.shared.features.cardset.vm.CardSetRouter
import com.aglushkov.wordteacher.shared.features.cardsets.vm.CardSetsRouter
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsRouter
import com.aglushkov.wordteacher.shared.features.learning.vm.LearningRouter
import com.aglushkov.wordteacher.shared.features.learning_session_result.vm.LearningSessionResultRouter

interface Router: ArticlesRouter,
    ArticleRouter,
    CardSetsRouter,
    CardSetRouter,
    DefinitionsRouter,
    LearningRouter,
    LearningSessionResultRouter