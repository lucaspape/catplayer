package de.lucaspape.persistentcookiejar.cache

import de.lucaspape.persistentcookiejar.cache.IdentifiableCookie.Companion.decorateAll
import okhttp3.Cookie
import java.util.*

/**
 * Forked from https://github.com/franmontiel/PersistentCookieJar
 */
class SetCookieCache : CookieCache {
    private val cookies: MutableSet<IdentifiableCookie>
    override fun addAll(cookies: Collection<Cookie>) {
        for (cookie in decorateAll(cookies)) {
            this.cookies.remove(cookie)
            this.cookies.add(cookie)
        }
    }

    override fun clear() {
        cookies.clear()
    }

    override fun iterator(): MutableIterator<Cookie> {
        return SetCookieCacheIterator()
    }

    private inner class SetCookieCacheIterator :
        MutableIterator<Cookie> {
        private val iterator: MutableIterator<IdentifiableCookie> = cookies.iterator()
        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }

        override fun next(): Cookie {
            return iterator.next().cookie
        }

        override fun remove() {
            iterator.remove()
        }

    }

    init {
        cookies = HashSet()
    }
}