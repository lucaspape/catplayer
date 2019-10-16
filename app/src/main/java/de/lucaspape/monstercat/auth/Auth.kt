package de.lucaspape.monstercat.auth

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
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

//static vars
var sid = ""
var loggedIn = false

/**
 * Auth - Login to monstercat
 */
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

            //login post request
            val loginPostRequest = object : JsonObjectRequest(
                Method.POST,
                loginUrl, loginPostParams, Response.Listener { response ->
                    try {
                        println(response)

                        val headers = response.getJSONObject("headers")
                        sid = headers.getString("Set-Cookie").substringBefore(';')
                            .replace("connect.sid=", "")

                        if (response.getJSONObject("response").getString("message") == "Enter the code sent to your device") {
                            showTwoFAInput(context)
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.loginSuccessfulMsg),
                                Toast.LENGTH_SHORT
                            ).show()
                        }


                    } catch (e: JSONException) {
                        println("f")
                    }

                }, Response.ErrorListener { error ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.loginFailedMsg),
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                //put the login data
                override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
                    return try {
                        println(response)

                        val jsonResponse = JSONObject()
                        jsonResponse.put("headers", JSONObject(response!!.headers as Map<*, *>))
                        jsonResponse.put("response", response)

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

    private fun showTwoFAInput(context: Context) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle("2FA Code")

        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val twoFAInputView = layoutInflater.inflate(R.layout.two_fa_input_layout, null)

        alertDialogBuilder.setView(twoFAInputView)
        alertDialogBuilder.setCancelable(false)

        alertDialogBuilder.setPositiveButton("OK"){ dialog, which ->
            val twoFAEditText = twoFAInputView.findViewById<EditText>(R.id.twoFAInput)
            val twoFACode = twoFAEditText.text.toString()

            val twoFaTokenParams = JSONObject()
            twoFaTokenParams.put("token", twoFACode)

            val twoFAPostRequest = object :
                JsonObjectRequest(Method.POST, context.getString(R.string.tokenUrl), twoFaTokenParams,
                    Response.Listener { response ->
                        val headers = response.getJSONObject("headers")
                        sid = headers.getString("Set-Cookie").substringBefore(';')
                            .replace("connect.sid=", "")
                    },
                    Response.ErrorListener {
                        println("Well fuck.")
                    }) {
                //put the login data
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

                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["Cookie"] = "connect.sid=$sid"

                    return params
                }
            }

            val twoFAQueue = Volley.newRequestQueue(context)

            twoFAQueue.addRequestFinishedListener<Any> {
                if (sid != "") {
                    loggedIn = true
                }
            }

            twoFAQueue.add(twoFAPostRequest)

        }
    }
}