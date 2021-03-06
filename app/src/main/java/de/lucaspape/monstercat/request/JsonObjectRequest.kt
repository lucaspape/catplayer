package de.lucaspape.monstercat.request

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.JsonRequest
import org.json.JSONObject
import java.lang.NullPointerException

/**
 * JsonObjectRequest which does not return an error if response is empty
 */
class JsonObjectRequest(
    method: Int,
    url: String,
    jsonObject: JSONObject,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener

) : JsonObjectRequest(method, url, jsonObject, listener, errorListener) {

    /**
     * Server can return empty response if request success
     */
    override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
        response?.let {
            val jsonString = String(
                response.data,
                charset(
                    HttpHeaderParser.parseCharset(
                        response.headers,
                        JsonRequest.PROTOCOL_CHARSET
                    )
                )
            )

            val jsonObject = if (jsonString.isEmpty()) {
                JSONObject()
            } else {
                JSONObject(jsonString)
            }

            return Response.success(
                jsonObject, HttpHeaderParser.parseCacheHeaders(response)
            )
        }

        return Response.error(ParseError(NullPointerException()))
    }
}