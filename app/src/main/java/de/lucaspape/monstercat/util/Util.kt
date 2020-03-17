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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.handlers.abstract_items.AlertListItem
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
        println(e.stackTrace)
    }
}

fun displayInfo(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}

fun displaySnackbar(
    view: View,
    msg: String,
    buttonText: String?,
    buttonListener: (view: View) -> Unit
) {
    val snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG)

    buttonText?.let {
        snackbar.setAction(buttonText, buttonListener)
    }
    
    snackbar.show()
}

fun displayAlertDialogList(context: Context, title:String, listItems:ArrayList<AlertListItem>, onItemClick:(position:Int, item:AlertListItem) -> Unit){
    val alertDialogBuilder = MaterialAlertDialogBuilder(context, R.style.DialogSlideAnim)
    alertDialogBuilder.setTitle(title)

    val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val alertListLayout = layoutInflater.inflate(R.layout.alert_list, null, false)

    alertDialogBuilder.setView(alertListLayout)
    val dialog = alertDialogBuilder.create()

    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.window?.setGravity(Gravity.BOTTOM)

    dialog.show()

    val recyclerView = alertListLayout.findViewById<RecyclerView>(R.id.alertDialogList)
    recyclerView.layoutManager =
        LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

    val itemAdapter = ItemAdapter<AlertListItem>()
    val fastAdapter: FastAdapter<GenericItem> =
        FastAdapter.with(listOf(itemAdapter))

    for(listItem in listItems){
        itemAdapter.add(listItem)
    }

    recyclerView.adapter = fastAdapter

    fastAdapter.onClickListener = { _, _, _, position ->
        onItemClick(position, listItems[position])
        dialog.hide()
        false
    }
}