package de.lucaspape.monstercat

import android.content.Context
import android.widget.Toast
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.MainActivity.Companion.sid
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException

class Auth {
    fun login(context:Context){
        val settings = Settings(context)

        val username = settings.getSetting("email")
        val password = settings.getSetting("password")

        if(username != null && password != null) {
            val loginPostParams = JSONObject()
            loginPostParams.put("email", username)
            loginPostParams.put("password", password)

            val loginUrl = "https://connect.monstercat.com/v2/signin"

            val loginPostRequest = object : JsonObjectRequest(Request.Method.POST,
                loginUrl, loginPostParams, Response.Listener { response ->
                    val headers = response.getJSONObject("headers")

                    try {
                        //get SID
                        sid = headers.getString("Set-Cookie").substringBefore(';').replace("connect.sid=", "")
                        Toast.makeText(
                            context,
                            "Login successful",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: JSONException) {
                        println(headers)
                        println(e)
                    }

                }, Response.ErrorListener { error ->

                }) {
                override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
                    try {
                        val jsonResponse = JSONObject()
                        jsonResponse.put("headers", JSONObject(response!!.headers as Map<*, *>))

                        return Response.success(
                            jsonResponse,
                            HttpHeaderParser.parseCacheHeaders(response)
                        )
                    } catch (e: UnsupportedEncodingException) {

                        return Response.error<JSONObject>(ParseError(e))
                    }
                }
            }

            val loginQueue = Volley.newRequestQueue(context)
            loginQueue.add(loginPostRequest)
        }

    }
}