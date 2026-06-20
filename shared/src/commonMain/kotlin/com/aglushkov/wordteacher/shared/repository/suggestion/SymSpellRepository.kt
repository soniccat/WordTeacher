package com.aglushkov.wordteacher.shared.repository.suggestion

import com.aglushkov.wordteacher.shared.dicts.wordlist.WORDLIST_EXTENSION
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilLoaded
import com.aglushkov.wordteacher.shared.general.resource.SimpleResourceRepository
import com.aglushkov.wordteacher.shared.general.resource.asLoaded
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.darkrockstudios.symspellkt.impl.SymSpell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SymSpellRepository(
    private val symSpell: SymSpell,
    private val dictRepository: DictRepository,
): SimpleResourceRepository<SymSpell, Unit>() {

    override suspend fun loadInternal(arg: Unit): SymSpell {
        dictRepository.dicts.waitUntilLoaded()

        val dicts = dictRepository.dicts.value.asLoaded()
            ?: throw RuntimeException("dicts aren't loaded")
        val dict = dicts.data.firstOrNull { it.path.name.endsWith(WORDLIST_EXTENSION) }
            ?: throw RuntimeException("no wordlilst dict")

        (symSpell.dictionary as SymSpellDictionaryHolder).fillFromDict(dict)
        return symSpell
    }

    fun lookup(value: String): List<String> {
        if (!(symSpell.dictionary as SymSpellDictionaryHolder).isReady) {
            return emptyList()
        }

        val r = symSpell.lookup(value)
        return r.map {
            it.term
        }
    }
}