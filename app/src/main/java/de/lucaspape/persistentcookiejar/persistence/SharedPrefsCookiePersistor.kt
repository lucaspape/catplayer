package de.lucaspape.persistentcookiejar.persistence

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import java.util.*

/**
 * Forked from https://github.com/franmontiel/PersistentCookieJar
 */
@SuppressLint("CommitPrefEdits")
class SharedPrefsCookiePersistor(private val sharedPreferences: SharedPreferences) :
    CookiePersistor {

    constructor(context: Context) : this(
        context.getSharedPreferences(
            "CookiePersistence",
            Context.MODE_PRIVATE
        )
    )

    override fun loadAll(): List<Cookie?>? {
        val cookies: MutableList<Cookie?> =
            ArrayList(sharedPreferences.all.size)
        for ((_, value) in sharedPreferences.all) {
            val serializedCookie = value as String
            val cookie =
                SerializableCookie()
                    .decode(serializedCookie)
            if (cookie != null) {
                cookies.add(cookie)
            }
        }
        return cookies
    }

    override fun saveAll(cookies: Collection<Cookie?>?) {
        val editor = sharedPreferences.edit()
        for (cookie in cookies!!) {
            editor.putString(
                createCookieKey(
                    cookie
                ),
                SerializableCookie()
                    .encode(cookie)
            )
        }
        editor.apply()
    }

    override fun removeAll(cookies: Collection<Cookie?>?) {
        val editor = sharedPreferences.edit()
        for (cookie in cookies!!) {
            editor.remove(
                createCookieKey(
                    cookie
                )
            )
        }
        editor.apply()
    }

    override fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private fun createCookieKey(cookie: Cookie?): String {
            return (if (cookie!!.secure) "https" else "http") + "://" + cookie.domain + cookie.path + "|" + cookie.name
        }
    }

}