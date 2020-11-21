package de.lucaspape.monstercat.request

import android.content.Context
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import de.lucaspape.persistentcookiejar.PersistentCookieJar
import de.lucaspape.persistentcookiejar.cache.SetCookieCache
import de.lucaspape.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import okhttp3.Cookie
import okhttp3.HttpUrl

val requestQueues = HashMap<String, RequestQueue?>()

/**
 * Get volley queue from hashmap and create new if it does not exists
 */
fun getAuthorizedRequestQueue(context: Context, requestHost: String?): RequestQueue {
    var queue = if(requestHost == null){
        requestQueues["default"]
    }else{
        requestQueues[requestHost]
    }

    if(queue == null){
        if (requestHost == null) {
            queue = Volley.newRequestQueue(
                context, OkHttp3Stack(
                    context, null
                )
            )
        } else {
            queue = Volley.newRequestQueue(
                context, OkHttp3Stack(
                    context, object : PersistentCookieJar(
                        SetCookieCache(),
                        SharedPrefsCookiePersistor(context)
                    ) {
                        override fun loadForRequest(url: HttpUrl): List<Cookie> {
                            return super.loadForRequest(
                                HttpUrl.Builder().scheme("https").host(requestHost).build()
                            )
                        }
                    }
                )
            )
        }

        if(requestHost == null){
            requestQueues["default"] = queue
        }else{
            requestQueues[requestHost] = queue
        }
    }

    return queue!!
}