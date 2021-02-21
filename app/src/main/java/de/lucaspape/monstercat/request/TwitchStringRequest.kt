package de.lucaspape.monstercat.request

import com.android.volley.Response
import com.android.volley.toolbox.StringRequest

class TwitchStringRequest(
    method: Int,
    url: String,
    private val userAgent: String,
    listener: Response.Listener<String>,
    errorListener: Response.ErrorListener
) : StringRequest(method, url, listener, errorListener) {

    override fun getHeaders(): Map<String, String> {
        val params = HashMap<String, String>()
        params["User-Agent"] = userAgent
        return params
    }
}