package com.aglushkov.wordteacher.shared.repository.suggestion

import com.aglushkov.wordteacher.shared.dicts.Dict
import com.aglushkov.wordteacher.shared.dicts.wordlist.WORDLIST_EXTENSION
import com.aglushkov.wordteacher.shared.general.extensions.collectUntilLoaded
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.asLoaded
import com.aglushkov.wordteacher.shared.general.resource.loadResourceWithProgress
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.darkrockstudios.symspellkt.impl.SymSpell
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile

class SymSpellRepository(
    private val symSpell: SymSpell,
    private val dictRepository: DictRepository,
    private val isDBReady: () -> Boolean
) {
    val dictHolder: SymSpellDictionaryHolder
        get() {
            return symSpell.dictionary as SymSpellDictionaryHolder
        }

    suspend fun load(): Flow<Pair<Float, Unit>> {
        dictRepository.dicts.collectUntilLoaded()

        var dict: Dict? = null
        dictRepository.dicts.takeWhile {
            val dicts = it.data().orEmpty()
            dict = dicts.firstOrNull { it.path.name.endsWith(WORDLIST_EXTENSION) }
            dict == null
        }.collect()

        return dictHolder.fillFromDict(dict!!)
    }

    fun lookup(value: String): List<String> {
        if (!isDBReady()) {
            return emptyList()
        }

        if (!dictHolder.isReady) {
            return emptyList()
        }

        val r = symSpell.lookup(value)
        return r.map {
            it.term
        }
    }
}