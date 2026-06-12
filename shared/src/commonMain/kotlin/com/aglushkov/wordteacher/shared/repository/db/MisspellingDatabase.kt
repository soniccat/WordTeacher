package com.aglushkov.wordteacher.shared.repository.db

import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.aglushkov.wordteacher.wordfrequencydb.WordFrequencyDB
import com.aglushkov.wordteacher.wordfrequencydb.WordFrequencyDB.Companion.invoke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import okio.Path

class MisspellingDatabase(
    private val driverFactory: DatabaseDriverFactory,
    private val dbPreparer: () -> Path,
    private val settings: SettingStore,
) {
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var driver = driverFactory.createMisspellingDBDriver()
    private var db = WordFrequencyDB(driver)

    val state = MutableStateFlow<Resource<WordFrequencyDatabase>>(Resource.Uninitialized())

}