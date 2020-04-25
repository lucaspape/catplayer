package de.lucaspape.monstercat.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.OkHttp3Stack
import de.lucaspape.monstercat.ui.abstract_items.AlertListItem
import okhttp3.Cookie
import okhttp3.HttpUrl
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

fun wifiConnected(context: Context): Boolean? {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val activeNetwork =
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
}

fun downloadFile(
    destination: String,
    source: String,
    tempDir: String,
    sid: String?,
    progressUpdate: (max: Int, current: Int) -> Unit
) {
    val destinationFile = File(destination)
    val tempFile = File(tempDir + "/" + UUID.randomUUID().toString())

    try {
        val urlConnection = URL(source).openConnection()
        urlConnection.setRequestProperty("Cookie", "connect.sid=$sid")
        urlConnection.connect()

        val lengthOfFile = urlConnection.contentLength

        val bufferedInputStream = BufferedInputStream(urlConnection.getInputStream(), 8192)

        val fileOutputStream = FileOutputStream(tempFile)

        val data = ByteArray(1024)
        var total: Long = 0
        var count: Int
        var updateCount = 0

        do {
            count = bufferedInputStream.read(data)

            if (count == -1) {
                break
            }

            total += count

            fileOutputStream.write(data, 0, count)

            updateCount++

            if (updateCount > 100) {
                updateCount = 0
                progressUpdate(lengthOfFile, total.toInt())
            }
        } while (true)

        fileOutputStream.flush()
        fileOutputStream.close()
        bufferedInputStream.close()

        tempFile.renameTo(destinationFile)
    } catch (e: Exception) {
        println(e.printStackTrace())
    }
}

fun displayInfo(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

fun displaySnackBar(
    view: View,
    msg: String,
    buttonText: String?,
    buttonListener: (view: View) -> Unit
) {
    try {
        val snackBar = Snackbar.make(view, msg, Snackbar.LENGTH_SHORT)

        buttonText?.let {
            snackBar.setAction(buttonText, buttonListener)
        }

        snackBar.show()
    } catch (e: IllegalArgumentException) {

    }
}

fun displayAlertDialogList(
    context: Context,
    headerItem: GenericItem?,
    listItems: ArrayList<AlertListItem>,
    onItemClick: (position: Int, item: AlertListItem) -> Unit
) {
    val alertDialogBuilder = MaterialAlertDialogBuilder(context, R.style.DialogSlideAnim)

    val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val alertListLayout = layoutInflater.inflate(R.layout.alert_list, null, false)

    alertDialogBuilder.setView(alertListLayout)
    alertDialogBuilder.setCancelable(true)
    val dialog = alertDialogBuilder.create()

    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.window?.setGravity(Gravity.BOTTOM)

    dialog.show()

    val recyclerView = alertListLayout.findViewById<RecyclerView>(R.id.alertDialogList)
    recyclerView.layoutManager =
        LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

    val headerAdapter = ItemAdapter<GenericItem>()
    val itemAdapter = ItemAdapter<AlertListItem>()
    val fastAdapter: FastAdapter<GenericItem> =
        FastAdapter.with(listOf(headerAdapter, itemAdapter))

    var indexOffset = 0

    headerItem?.let {
        headerAdapter.add(headerItem)
        indexOffset = -1
    }

    for (listItem in listItems) {
        itemAdapter.add(listItem)
    }

    recyclerView.adapter = fastAdapter

    fastAdapter.onClickListener = { _, _, _, position ->
        val index = position + indexOffset

        if (index >= 0) {
            onItemClick(index, listItems[index])
            dialog.hide()
        }

        false
    }
}

/**
 * Creates new volley queue using OkHttp3Stack and cookies from connect.monstercat.com
 */
fun newAuthorizedRequestQueue(context: Context, requestHost:String?): RequestQueue {
    return if(requestHost == null){
        Volley.newRequestQueue(
            context, OkHttp3Stack(
                context, null
            )
        )
    }else{
        Volley.newRequestQueue(
            context, OkHttp3Stack(
                context, object : PersistentCookieJar(
                    SetCookieCache(),
                    SharedPrefsCookiePersistor(context)
                ) {
                    override fun loadForRequest(url: HttpUrl): List<Cookie> {
                        return super.loadForRequest(
                            HttpUrl.Builder().scheme("https").host(requestHost).build()
                        )
                    }
                }
            )
        )
    }
}