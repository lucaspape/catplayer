package de.lucaspape.monstercat.request


import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

/**
 * Modified stringRequest
 */
class TwitchJsonObjectRequest(
    method: Int,
    url: String,
    jsonObject: JSONObject,
    private val clientId: String?,
    private val userAgent: String,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(method, url, jsonObject, listener, errorListener) {

    override fun getHeaders(): Map<String, String> {
        return if (clientId != null) {
            val params = HashMap<String, String>()
            params["Client-ID"] = "$clientId"
            params["User-Agent"] = userAgent
            params
        } else {
            super.getHeaders()
        }
    }
}