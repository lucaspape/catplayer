package de.lucaspape.monstercat.util

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.EditText
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.JsonObjectRequest
import okhttp3.HttpUrl
import org.json.JSONException
import org.json.JSONObject

var loggedIn = false
    private set

fun getSid(context: Context): String {
    val cookieJar =
        PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context))

    val cookies = cookieJar.loadForRequest(
        HttpUrl.Builder().scheme("https").host("connect.monstercat.com").build()
    )

    var sid = ""

    for (cookie in cookies) {
        if (cookie.name == "connect.sid") {
            sid = cookie.value
        }
    }

    return sid
}

/**
 * Auth - Login to monstercat
 */
class Auth {

    /**
     * Posts username and password to API, goal is to get sid cookie
     */
    fun login(
        context: Context,
        username: String,
        password: String,
        loginSuccess: () -> Unit,
        loginFailed: () -> Unit
    ) {
        val loginPostParams = JSONObject()
        loginPostParams.put("email", username)
        loginPostParams.put("password", password)

        val loginUrl = context.getString(R.string.loginUrl)

        val loginPostRequest =
            JsonObjectRequest(
                Request.Method.POST, loginUrl, loginPostParams,
                Response.Listener { response ->
                    //check if 2FA is needed
                    try {
                        if (response.getString("message") == "Enter the code sent to your device") {
                            showTwoFAInput(context, loginSuccess, loginFailed)
                        } else {
                            checkLogin(context, loginSuccess, loginFailed)
                        }
                    } catch (e: JSONException) {
                        checkLogin(context, loginSuccess, loginFailed)
                    }

                },
                Response.ErrorListener {

                })

        val loginQueue = newRequestQueue(context)

        //add to queue
        loginQueue.add(loginPostRequest)
    }

    /**
     * Shows 2FA input, if this fails the sid will be invalid
     */
    private fun showTwoFAInput(
        context: Context,
        loginSuccess: () -> Unit,
        loginFailed: () -> Unit
    ) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle(context.getString(R.string.twoFaCode))

        val layoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val twoFAInputView = layoutInflater.inflate(R.layout.two_fa_input_layout, null)

        alertDialogBuilder.setView(twoFAInputView)
        alertDialogBuilder.setCancelable(false)

        alertDialogBuilder.setPositiveButton(context.getString(R.string.ok)) { _, _ ->
            //post token to API

            val twoFAEditText = twoFAInputView.findViewById<EditText>(R.id.twoFAInput)
            val twoFACode = twoFAEditText.text.toString()

            val twoFaTokenParams = JSONObject()
            twoFaTokenParams.put("token", twoFACode)

            val twoFaPostRequest = JsonObjectRequest(
                Request.Method.POST,
                context.getString(R.string.tokenUrl),
                twoFaTokenParams,
                Response.Listener {
                    //all good
                },
                Response.ErrorListener {
                    //TODO show 2FA again/request new code
                })

            val twoFAQueue = newRequestQueue(context)

            twoFAQueue.addRequestFinishedListener<Any> {
                checkLogin(context, loginSuccess, loginFailed)
            }

            twoFAQueue.add(twoFaPostRequest)
        }

        val dialog = alertDialogBuilder.create()
        dialog.show()

        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

        val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.setTextColor(typedValue.data)
    }

    /**
     * Checks if sid is valid
     */
    fun checkLogin(context: Context, loginSuccess: () -> Unit, loginFailed: () -> Unit) {

        //check if sid is valid, get session and user id, if it is null or "" the sid is NOT valid -> login fail
        val checkLoginRequest =
            StringRequest(Request.Method.GET, context.getString
                (R.string.sessionUrl),
                Response.Listener { response ->
                    val jsonResponse = JSONObject(response)

                    val userId = try {
                        val userObject = jsonResponse.getJSONObject("user")
                        userObject.getString("id")
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


        val checkLoginQueue = newRequestQueue(context)
        checkLoginQueue.add(checkLoginRequest)
    }
}