package de.lucaspape.monstercat.util

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.AuthorizedRequest
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException


//static vars
private var sid = ""
var loggedIn = false

fun getSid(): String? {
    return if (loggedIn && sid != "") {
        sid
    } else {
        null
    }
}

/**
 * Auth - Login to monstercat
 */
class Auth {
    fun login(
        context: Context,
        username: String,
        password: String,
        loginSuccess: () -> Unit,
        loginFailed: () -> Unit
    ) {
        //loadLogin(context)

        val loginPostParams = JSONObject()
        loginPostParams.put("email", username)
        loginPostParams.put("password", password)

        val loginUrl = context.getString(R.string.loginUrl)

        val loginPostRequest =
            object : JsonObjectRequest(Method.POST, loginUrl, loginPostParams,
                Response.Listener { response ->
                    val cookies = response.getString("cookies")

                    sid = cookies.substringBefore(';').replace("connect.sid=", "")

                    try {
                        val networkResponse = response.get("data") as String
                        val responseJsonObject = JSONObject(networkResponse)

                        if (responseJsonObject.getString("message") == "Enter the code sent to your device") {
                            showTwoFAInput(context, loginSuccess, loginFailed)
                        } else {
                            checkLogin(context, loginSuccess, loginFailed)
                        }
                    } catch (e: JSONException) {
                        checkLogin(context, loginSuccess, loginFailed)
                    }
                },
                Response.ErrorListener {
                }) {
                override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
                    val jsonResponse = JSONObject()

                    val responseHeaders = response?.headers
                    val cookies = responseHeaders?.get("Set-Cookie")

                    val responseData = response?.data

                    responseData?.let {
                        jsonResponse.put("data", String(it))
                    }

                    cookies?.let {
                        jsonResponse.put("cookies", it)
                    }

                    return try {
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

        //add to queue
        loginQueue.add(loginPostRequest)
    }

    private fun showTwoFAInput(
        context: Context,
        loginSuccess: () -> Unit,
        loginFailed: () -> Unit
    ) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle("2FA Code")

        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val twoFAInputView = layoutInflater.inflate(R.layout.two_fa_input_layout, null)

        alertDialogBuilder.setView(twoFAInputView)
        alertDialogBuilder.setCancelable(false)

        alertDialogBuilder.setPositiveButton("OK") { _, _ ->
            val twoFAEditText = twoFAInputView.findViewById<EditText>(R.id.twoFAInput)
            val twoFACode = twoFAEditText.text.toString()

            val twoFaTokenParams = JSONObject()
            twoFaTokenParams.put("token", twoFACode)

            val twoFaPostRequest = object : JsonObjectRequest(Method.POST,
                context.getString(R.string.tokenUrl),
                twoFaTokenParams,
                Response.Listener {
                },
                Response.ErrorListener {
                }) {

                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["Cookie"] = "connect.sid=$sid"
                    return params
                }

                override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
                    val jsonResponse = JSONObject()

                    val responseData = response?.data

                    responseData?.let {
                        jsonResponse.put("data", String(it))
                    }

                    return try {
                        Response.success(
                            jsonResponse,
                            HttpHeaderParser.parseCacheHeaders(response)
                        )
                    } catch (e: UnsupportedEncodingException) {
                        Response.error(ParseError(e))
                    }
                }
            }

            val twoFAQueue = Volley.newRequestQueue(context)

            twoFAQueue.addRequestFinishedListener<Any> {
                checkLogin(context, loginSuccess, loginFailed)
            }

            twoFAQueue.add(twoFaPostRequest)

        }

        alertDialogBuilder.show()
    }

    private fun saveLogin(context: Context) {
        val settings = Settings(context)
        settings.saveSetting("sid", sid)
    }

    private fun loadLogin(context: Context, loginSuccess: () -> Unit, loginFailed: () -> Unit) {
        val settings = Settings(context)

        val sSid = settings.getSetting("sid")

        if (sSid != null) {
            sid = sSid

            checkLogin(context, loginSuccess, loginFailed)
        }
    }

    private fun checkLogin(context: Context, loginSuccess: () -> Unit, loginFailed: () -> Unit) {
        val checkLoginRequest =
            AuthorizedRequest(Request.Method.GET, context.getString
                (R.string.sessionUrl), sid,
                Response.Listener { response ->
                    val jsonResponse = JSONObject(response)

                    val userId = try {
                        val userObject = jsonResponse.getJSONObject("user")
                        userObject.getString("_id")
                    } catch (e: JSONException) {
                        ""
                    }

                    if (userId != "null" && userId != "") {
                        loggedIn = true
                        loginSuccess()
                    } else {
                        loggedIn = false
                        loginFailed()
                    }
                },
                Response.ErrorListener {
                    loggedIn = false
                    loginFailed()
                })

        val checkLoginQueue = Volley.newRequestQueue(context)
        checkLoginQueue.add(checkLoginRequest)

    }
}