package de.lucaspape.monstercat.util

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.EditText
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.music.connectSid
import de.lucaspape.monstercat.request.newCheckLoginRequest
import de.lucaspape.monstercat.request.newLoginRequest
import de.lucaspape.monstercat.request.newTwoFaRequest
import de.lucaspape.persistentcookiejar.PersistentCookieJar
import de.lucaspape.persistentcookiejar.cache.SetCookieCache
import de.lucaspape.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import de.lucaspape.util.Settings
import okhttp3.HttpUrl
import org.json.JSONException
import java.util.*
import kotlin.ConcurrentModificationException
import kotlin.collections.ArrayList

var loggedIn = false
    private set

var waitingForLogin = false
    private set

var offline = false
    private set

var username = ""
    private set

var loggedInStateChangedListeners = ArrayList<LoggedInStateChangedListener>()

private fun runCallbacks(){
    try {
        val iterator = loggedInStateChangedListeners.iterator()

        while(iterator.hasNext()){
            val listener = iterator.next()

            listener.run()

            loggedInStateChangedListeners.removeIf { it.removeOnCalled &&  it.listenerId == listener.listenerId }
        }
    }catch (e: ConcurrentModificationException){

    }
}

class LoggedInStateChangedListener(val run: () -> Unit, val removeOnCalled: Boolean){
    val listenerId = UUID.randomUUID().toString()
}

private fun getSid(context: Context): String {
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
        waitingForLogin = true

        val loginQueue =
            getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

        //add to queue
        loginQueue.add(newLoginRequest(context, username, password, {
            try {
                if (it.getString("message") == "Enter the code sent to your device") {
                    showTwoFAInput(context, loginSuccess, loginFailed)
                } else {
                    checkLogin(context, loginSuccess, loginFailed)
                }
            } catch (e: JSONException) {
                checkLogin(context, loginSuccess, loginFailed)
            }
        }, {
            waitingForLogin = false
        }))
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

            val twoFAQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            twoFAQueue.add(newTwoFaRequest(context, twoFACode, {
                //all good
                checkLogin(context, loginSuccess, loginFailed)
            }, {
                //TODO show 2FA again/request new code
            }))
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

        val checkLoginQueue =
            getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))
        checkLoginQueue.add(newCheckLoginRequest(context, {
            val userId = try {
                val userObject = it.getJSONObject("user")
                userObject.getString("id")
            } catch (e: JSONException) {
                ""
            }

            username = try {
                val userObject = it.getJSONObject("user")
                userObject.getString("email")
            } catch (e: JSONException){
                ""
            }

            waitingForLogin = false

            if (userId != "null" && userId != "" && username != "null" && username != "") {
                loggedIn = true

                connectSid = getSid(context)

                loginSuccess()

                runCallbacks()
            } else {
                loggedIn = false
                loginFailed()

                runCallbacks()
            }
        }, {
            waitingForLogin = false
            loggedIn = false
            offline = true
            loginFailed()

            runCallbacks()
        }))
    }

    fun logout(context: Context){
        waitingForLogin = false
        loggedIn = false

        val settings = Settings.getSettings(context)
        settings.setString(context.getString(R.string.emailSetting), "")
        settings.setString(context.getString(R.string.passwordSetting), "")

        SharedPrefsCookiePersistor(context).clear()
        
        connectSid = ""

        runCallbacks()
    }
}