package de.lucaspape.persistentcookiejar.cache

import okhttp3.Cookie

/**
 * A CookieCache handles the volatile cookie session storage.
 * Forked from https://github.com/franmontiel/PersistentCookieJar
 */
interface CookieCache : MutableIterable<Cookie> {
    /**
     * Add all the new cookies to the session, existing cookies will be overwritten.
     *
     * @param cookies
     */
    fun addAll(cookies: Collection<Cookie>)

    /**
     * Clear all the cookies from the session.
     */
    fun clear()
}