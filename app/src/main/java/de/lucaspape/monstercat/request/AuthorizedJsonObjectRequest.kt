package de.lucaspape.monstercat.request

import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject
import java.nio.charset.Charset

class AuthorizedJsonObjectRequest(
    method: Int,
    url: String,
    private val sid: String?,
    private val jsonObject: JSONObject,
    listener: Response.Listener<String>,
    errorListener: Response.ErrorListener

) : StringRequest(method, url, listener, errorListener) {
    override fun getHeaders(): Map<String, String> {
        return if (sid != null) {
            val params = HashMap<String, String>()
            params["Cookie"] = "connect.sid=$sid"
            params
        } else {
            super.getHeaders()
        }
    }

    override fun getBody(): ByteArray {
        return jsonObject.toString().toByteArray(Charset.forName("UTF-8"))
    }

    override fun getBodyContentType(): String {
        return "application/json; charset=utf-8"
    }
}