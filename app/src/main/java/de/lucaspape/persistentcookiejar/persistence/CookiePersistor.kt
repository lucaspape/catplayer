package de.lucaspape.persistentcookiejar.persistence

import okhttp3.Cookie

/**
 * A CookiePersistor handles the persistent cookie storage.
 * Forked from https://github.com/franmontiel/PersistentCookieJar
 */
interface CookiePersistor {
    fun loadAll(): List<Cookie?>?

    /**
     * Persist all cookies, existing cookies will be overwritten.
     *
     * @param cookies cookies persist
     */
    fun saveAll(cookies: Collection<Cookie?>?)

    /**
     * Removes indicated cookies from persistence.
     *
     * @param cookies cookies to remove from persistence
     */
    fun removeAll(cookies: Collection<Cookie?>?)

    /**
     * Clear all cookies from persistence.
     */
    fun clear()
}