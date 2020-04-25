package de.lucaspape.persistentcookiejar

import okhttp3.CookieJar

/**
 * This interface extends [okhttp3.CookieJar] and adds methods to clear the cookies.
 * Forked from https://github.com/franmontiel/PersistentCookieJar
 */
interface ClearableCookieJar : CookieJar {
    /**
     * Clear all the session cookies while maintaining the persisted ones.
     */
    fun clearSession()

    /**
     * Clear all the cookies from persistence and from the cache.
     */
    fun clear()
}