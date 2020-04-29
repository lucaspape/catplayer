package de.lucaspape.util

import com.google.gson.reflect.TypeToken

class Cache() {
    companion object {
        @JvmStatic
        val cacheMap = HashMap<String, Any?>()
    }

    fun set(key: String, data: Any?) {
        cacheMap[key] = data
    }

    inline fun <reified T> get(key: String): T? {
        val data = cacheMap[key]

        if (data is T) {
            return data
        }

        return null
    }
}