package com.transcendiverse.digitaltwin.data

import android.content.Context
import android.content.SharedPreferences
import com.transcendiverse.digitaltwin.model.MobileToday
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class CachedToday(
    val today: MobileToday,
    val cachedAtEpochMillis: Long,
)

interface TodayCacheStore {
    fun load(): CachedToday?
    fun save(today: MobileToday, cachedAtEpochMillis: Long = System.currentTimeMillis())
    fun clear()
}

class SharedPreferencesTodayCacheStore(
    context: Context,
    json: Json = todayCacheJson,
) : TodayCacheStore {
    private val delegate = JsonTodayCacheStore(
        storage = SharedPreferencesTodayCacheStorage(
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
        ),
        json = json,
    )

    override fun load(): CachedToday? = delegate.load()

    override fun save(today: MobileToday, cachedAtEpochMillis: Long) {
        delegate.save(today, cachedAtEpochMillis)
    }

    override fun clear() {
        delegate.clear()
    }

    private companion object {
        const val PREFERENCES_NAME = "mobile_today_cache"
    }
}

class InMemoryTodayCacheStore(
    rawTodayJson: String? = null,
    cachedAtEpochMillis: Long = 0,
    json: Json = todayCacheJson,
) : TodayCacheStore {
    private val delegate = JsonTodayCacheStore(
        storage = InMemoryTodayCacheStorage(rawTodayJson, cachedAtEpochMillis),
        json = json,
    )

    override fun load(): CachedToday? = delegate.load()

    override fun save(today: MobileToday, cachedAtEpochMillis: Long) {
        delegate.save(today, cachedAtEpochMillis)
    }

    override fun clear() {
        delegate.clear()
    }
}

private class JsonTodayCacheStore(
    private val storage: TodayCacheStorage,
    private val json: Json,
) : TodayCacheStore {
    override fun load(): CachedToday? {
        val rawToday = storage.getString(KEY_TODAY_JSON) ?: return null
        val cachedAt = storage.getLong(KEY_CACHED_AT) ?: return null

        return try {
            CachedToday(
                today = json.decodeFromString<MobileToday>(rawToday),
                cachedAtEpochMillis = cachedAt,
            )
        } catch (error: SerializationException) {
            clear()
            null
        } catch (error: IllegalArgumentException) {
            clear()
            null
        }
    }

    override fun save(today: MobileToday, cachedAtEpochMillis: Long) {
        storage.putString(KEY_TODAY_JSON, json.encodeToString(today))
        storage.putLong(KEY_CACHED_AT, cachedAtEpochMillis)
    }

    override fun clear() {
        storage.remove(KEY_TODAY_JSON)
        storage.remove(KEY_CACHED_AT)
    }

    private companion object {
        const val KEY_TODAY_JSON = "today_json"
        const val KEY_CACHED_AT = "cached_at_epoch_millis"
    }
}

private interface TodayCacheStorage {
    fun getString(key: String): String?
    fun getLong(key: String): Long?
    fun putString(key: String, value: String)
    fun putLong(key: String, value: Long)
    fun remove(key: String)
}

private class SharedPreferencesTodayCacheStorage(
    private val preferences: SharedPreferences,
) : TodayCacheStorage {
    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun getLong(key: String): Long? =
        if (preferences.contains(key)) preferences.getLong(key, 0) else null

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}

private class InMemoryTodayCacheStorage(
    rawTodayJson: String?,
    cachedAtEpochMillis: Long,
) : TodayCacheStorage {
    private val values = mutableMapOf<String, Any>()

    init {
        if (rawTodayJson != null) {
            values["today_json"] = rawTodayJson
            values["cached_at_epoch_millis"] = cachedAtEpochMillis
        }
    }

    override fun getString(key: String): String? = values[key] as? String

    override fun getLong(key: String): Long? = values[key] as? Long

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}

private val todayCacheJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
