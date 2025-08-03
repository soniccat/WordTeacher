package com.aglushkov.wordteacher.shared.general.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

val settingJsonCoder = Json {
    ignoreUnknownKeys = true
}

val settingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

class SettingStore(
    val dataStore: DataStore<Preferences>,
) {
    val prefs: StateFlow<Preferences>

    init {
        // load from the main thread to be as quick as possible
        // crash intentionally here if exception happens
        val perfSnapshot = runBlocking {
            dataStore.data.first()
        }

        // observe with loaded snapshot in default dispatcher
        prefs = dataStore.data
            .stateIn(
                settingScope,
                SharingStarted.Eagerly,
                perfSnapshot,
            )
    }

    fun int(name: String) = prefs.value.int(name)
    fun int(name: String, default: Int) = prefs.value.int(name, default)
    fun intFlow(name: String, default: Int): Flow<Int> = prefs.map {
        it.int(name) ?: default }

    fun long(name: String) = prefs.value.long(name)
    fun long(name: String, default: Long) = prefs.value.long(name, default)
    fun longFlow(name: String, default: Long): Flow<Long> = prefs.map {
        it.long(name) ?: default }

    fun boolean(name: String) = prefs.value.boolean(name)
    fun boolean(name: String, default: Boolean) = prefs.value.boolean(name, default)
    fun booleanFlow(name: String, default: Boolean): Flow<Boolean> = prefs.map {
        it.boolean(name) ?: default }

    fun string(name: String) = prefs.value.string(name)
    fun string(name: String, default: String) = prefs.value.string(name, default)
    fun stringFlow(name: String, default: String): Flow<String> = prefs.map {
        it.string(name) ?: default }

    operator fun <T> set(key: String, value: T) = dataStore.set(key, value)

    fun <T> setSerializable(key: String, value: T, strategy: SerializationStrategy<T>) =
        dataStore.setSerializable(key, value, strategy)

    fun <T> serializable(key: String, strategy: DeserializationStrategy<T>): T? =
        prefs.value.serializable(key, strategy)

    fun edit(
        transform: suspend (MutablePreferences) -> Unit
    ) {
        settingScope.launch {
            dataStore.edit(transform)
        }
    }
}

inline fun <reified T> SettingStore.setSerializable(key: String, value: T)  {
    setSerializable(
        key,
        value,
        settingJsonCoder.serializersModule.serializer()
    )
}

inline fun <reified T> SettingStore.serializable(key: String): T? {
    return serializable(
        key,
        settingJsonCoder.serializersModule.serializer()
    )
}

fun Preferences.int(name: String) = this[intPreferencesKey(name)]
fun Preferences.int(name: String, default: Int) = int(name) ?: default

fun Preferences.long(name: String) = this[longPreferencesKey(name)]
fun Preferences.long(name: String, default: Long) = long(name) ?: default

fun Preferences.boolean(name: String) = this[booleanPreferencesKey(name)]
fun Preferences.boolean(name: String, default: Boolean) = boolean(name) ?: default

fun Preferences.string(name: String) = this[stringPreferencesKey(name)]
fun Preferences.string(name: String, default: String) = string(name) ?: default

operator fun <T> DataStore<Preferences>.set(key: String, value: T) = settingScope.launch {
    val prefKey = when (value) {
        is String -> stringPreferencesKey(key)
        is Boolean -> booleanPreferencesKey(key)
        is Int -> intPreferencesKey(key)
        is Long -> longPreferencesKey(key)
        else -> throw RuntimeException("DataStore<Preferences>.set: unsupported value type $value")
    } as Preferences.Key<T>
    edit {
        it[prefKey] = value
    }
}

fun <T> Preferences.serializable(key: String, strategy: DeserializationStrategy<T>): T? {
    return try {
        val str = string(key, "{}")
        settingJsonCoder.decodeFromString(strategy, str)
    } catch (e: Exception) {
        null
    }
}

inline fun <reified T> Preferences.serializable(key: String): T? {
    return serializable(
        key,
        settingJsonCoder.serializersModule.serializer()
    )
}

fun <T> DataStore<Preferences>.setSerializable(key: String, value: T, strategy: SerializationStrategy<T>) = settingScope.launch {
    edit {
        it[stringPreferencesKey(key)] = settingJsonCoder.encodeToString(strategy, value)
    }
}

inline fun <reified T> DataStore<Preferences>.setSerializable(key: String, value: T)  {
    setSerializable(
        key,
        value,
        settingJsonCoder.serializersModule.serializer()
    )
}
