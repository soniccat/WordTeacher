package com.aglushkov.wordteacher.shared.general.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.Preferences.Key
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aglushkov.wordteacher.shared.general.extensions.waitUntilDone
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isError
import com.aglushkov.wordteacher.shared.general.resource.isLoaded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class SettingStore(
    private val dataStore: DataStore<Preferences>,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefRes: StateFlow<Preferences>

    init {
        // load from the main thread to be as quick as possible
        // crash intentionally here if exception happens
        val perfs = runBlocking {
            dataStore.data.first()
        }

        // observe with loaded snapshot in default dispatcher
        prefRes = dataStore.data
            .stateIn(
                scope,
                SharingStarted.Eagerly,
                perfs,
            )
    }

    fun int(name: String) = prefRes.value[intPreferencesKey(name)]
    fun int(name: String, default: Int) = int(name) ?: default
    fun intFlow(name: String, default: Int): Flow<Int> = prefRes.map {
        it[intPreferencesKey(name)] ?: default }

    fun long(name: String) = prefRes.value[longPreferencesKey(name)]
    fun long(name: String, default: Long) = long(name) ?: default
    fun longFlow(name: String, default: Long): Flow<Long> = prefRes.map {
        it[longPreferencesKey(name)] ?: default }

    fun boolean(name: String) = prefRes.value[booleanPreferencesKey(name)]
    fun boolean(name: String, default: Boolean) = boolean(name) ?: default
    fun booleanFlow(name: String, default: Boolean): Flow<Boolean> = prefRes.map {
        it[booleanPreferencesKey(name)] ?: default }

    fun string(name: String) = prefRes.value[stringPreferencesKey(name)]
    fun string(name: String, default: String) = string(name) ?: default
    fun stringFlow(name: String, default: String): Flow<String> = prefRes.map {
        it[stringPreferencesKey(name)] ?: default }

    operator fun <T> set(key: String, value: T) = scope.launch {
        dataStore.edit {
            val prefKey = when (value) {
                is String -> stringPreferencesKey(key)
                is Boolean -> booleanPreferencesKey(key)
                is Int -> intPreferencesKey(key)
                else -> return@edit
            } as Preferences.Key<T>

            it[prefKey] = value
        }
    }

    val jsonCoder = Json {
        ignoreUnknownKeys = true
    }

    fun <T> setSerializable(key: String, value: T, strategy: SerializationStrategy<T>) = scope.launch {
        dataStore.edit {
            it[stringPreferencesKey(key)] = jsonCoder.encodeToString(strategy, value)
        }
    }

    fun <T> serializable(key: String, strategy: DeserializationStrategy<T>): T? {
        return try {
            val str = string(key, "{}")
            jsonCoder.decodeFromString(strategy, str)
        } catch (e: Exception) {
            null
        }
    }
}

inline fun <reified T> SettingStore.setSerializable(key: String, value: T)  {
    setSerializable(
        key,
        value,
        jsonCoder.serializersModule.serializer()
    )
}

inline fun <reified T> SettingStore.serializable(key: String): T? {
    return serializable(
        key,
        jsonCoder.serializersModule.serializer()
    )
}