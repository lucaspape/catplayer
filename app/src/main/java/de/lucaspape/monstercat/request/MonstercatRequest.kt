package de.lucaspape.monstercat.request

import com.android.volley.Response
import com.android.volley.toolbox.StringRequest

/**
 * Modified stringRequest
 */
class MonstercatRequest(
    method:Int,
    url: String,
    private val sid:String,
    listener: Response.Listener<String>,
    errorListener: Response.ErrorListener
) : StringRequest(method, url, listener, errorListener) {

    override fun getHeaders(): Map<String, String> {
        val params = HashMap<String, String>()
        params["Cookie"] = "connect.sid=$sid"

        return params
    }
}
