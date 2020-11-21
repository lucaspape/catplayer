package de.lucaspape.monstercat.core.twitch

import com.android.volley.Response
import com.android.volley.toolbox.StringRequest

/**
 * Modified stringRequest
 */
class TwitchRequest(
    method: Int,
    url: String,
    private val clientId: String?,
    listener: Response.Listener<String>,
    errorListener: Response.ErrorListener
) : StringRequest(method, url, listener, errorListener) {

    override fun getHeaders(): Map<String, String> {
        return if (clientId != null) {
            val params = HashMap<String, String>()
            params["Client-ID"] = "$clientId"
            params
        } else {
            super.getHeaders()
        }
    }
}
