package de.lucaspape.monstercat.auth

import android.content.Context
import android.widget.Toast
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.settings.Settings
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException

var sid = ""
var loggedIn = false

class Auth {
    fun login(context: Context) {
        val settings = Settings(context)

        val username = settings.getSetting("email")
        val password = settings.getSetting("password")

        if (username != null && password != null) {
            val loginPostParams = JSONObject()
            loginPostParams.put("email", username)
            loginPostParams.put("password", password)

            val loginUrl = context.getString(R.string.loginUrl)

            val loginPostRequest = object : JsonObjectRequest(
                Method.POST,
                loginUrl, loginPostParams, Response.Listener { response ->
                    val headers = response.getJSONObject("headers")

                    try {
                        //get SID
                        sid = headers.getString("Set-Cookie").substringBefore(';').replace("connect.sid=", "")

                        Toast.makeText(
                            context,
                            context.getString(R.string.loginSuccessfulMsg),
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (e: JSONException) {
                    }

                }, Response.ErrorListener { error ->
                    println(error)

                    Toast.makeText(
                        context,
                        context.getString(R.string.loginFailedMsg),
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
                    return try {
                        val jsonResponse = JSONObject()
                        jsonResponse.put("headers", JSONObject(response!!.headers as Map<*, *>))

                        Response.success(
                            jsonResponse,
                            HttpHeaderParser.parseCacheHeaders(response)
                        )
                    } catch (e: UnsupportedEncodingException) {

                        Response.error(ParseError(e))
                    }
                }
            }


            val loginQueue = Volley.newRequestQueue(context)

            loginQueue.addRequestFinishedListener<Any> {
                if (sid != "") {
                    loggedIn = true
                }
            }

            //add to queue
            loginQueue.add(loginPostRequest)
        }

    }
}