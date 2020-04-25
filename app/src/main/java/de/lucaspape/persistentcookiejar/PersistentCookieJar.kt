package de.lucaspape.persistentcookiejar

import de.lucaspape.persistentcookiejar.cache.CookieCache
import de.lucaspape.persistentcookiejar.persistence.CookiePersistor
import okhttp3.Cookie
import okhttp3.HttpUrl
import java.util.*

/**
 * Forked from https://github.com/franmontiel/PersistentCookieJar
 */
open class PersistentCookieJar(
    private val cache: CookieCache,
    private val persistor: CookiePersistor
) :
    ClearableCookieJar {
    @Synchronized
    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>
    ) {
        cache.addAll(cookies)
        persistor.saveAll(filterPersistentCookies(cookies))
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookiesToRemove: MutableList<Cookie> =
            ArrayList()
        val validCookies: MutableList<Cookie> =
            ArrayList()
        val it = cache.iterator()
        while (it.hasNext()) {
            it.next().let { currentCookie ->
                if (isCookieExpired(currentCookie)) {
                    cookiesToRemove.add(currentCookie)
                    it.remove()
                } else if (currentCookie.matches(url)) {
                    validCookies.add(currentCookie)
                }
            }
        }
        persistor.removeAll(cookiesToRemove)
        return validCookies
    }

    @Synchronized
    override fun clearSession() {
        cache.clear()
        cache.addAll(persistor.loadAll())
    }

    @Synchronized
    override fun clear() {
        cache.clear()
        persistor.clear()
    }

    companion object {
        private fun filterPersistentCookies(cookies: List<Cookie>): List<Cookie> {
            val persistentCookies: MutableList<Cookie> =
                ArrayList()
            for (cookie in cookies) {
                if (cookie.persistent) {
                    persistentCookies.add(cookie)
                }
            }
            return persistentCookies
        }

        private fun isCookieExpired(cookie: Cookie): Boolean {
            return cookie.expiresAt < System.currentTimeMillis()
        }
    }

    init {
        cache.addAll(persistor.loadAll())
    }
}