package de.lucaspape.persistentcookiejar.cache

import okhttp3.Cookie
import java.util.*

/**
 * This class decorates a Cookie to re-implements equals() and hashcode() methods in order to identify
 * the cookie by the following attributes: name, domain, path, secure & hostOnly.
 *
 *
 *
 * This new behaviour will be useful in determining when an already existing cookie in session must be overwritten.
 * Forked from https://github.com/franmontiel/PersistentCookieJar
 */
internal class IdentifiableCookie(val cookie: Cookie) {

    override fun equals(other: Any?): Boolean {
        if (other !is IdentifiableCookie) return false
        return other.cookie.name == cookie.name && other.cookie.domain == cookie.domain && other.cookie.path == cookie.path && other.cookie.secure == cookie.secure && other.cookie.hostOnly == cookie.hostOnly
    }

    override fun hashCode(): Int {
        var hash = 17
        hash = 31 * hash + cookie.name.hashCode()
        hash = 31 * hash + cookie.domain.hashCode()
        hash = 31 * hash + cookie.path.hashCode()
        hash = 31 * hash + if (cookie.secure) 0 else 1
        hash = 31 * hash + if (cookie.hostOnly) 0 else 1
        return hash
    }

    companion object {
        @JvmStatic
        fun decorateAll(cookies: Collection<Cookie?>): List<IdentifiableCookie> {
            val identifiableCookies: MutableList<IdentifiableCookie> =
                ArrayList(cookies.size)
            for (cookie in cookies) {
                cookie?.let {
                    identifiableCookies.add(
                        IdentifiableCookie(
                            cookie
                        )
                    )
                }
            }
            return identifiableCookies
        }
    }

}