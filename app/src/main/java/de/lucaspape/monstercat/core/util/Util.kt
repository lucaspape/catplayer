package de.lucaspape.monstercat.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.net.URL
import java.util.*

fun wifiConnected(context: Context): Boolean {
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
    sid: String,
    cid: String,
    progressUpdate: (max: Int, current: Int) -> Unit
) {
    val destinationFile = File(destination)
    val tempFile = File(tempDir + "/" + UUID.randomUUID().toString())

    try {
        val urlConnection = URL(source).openConnection()
        urlConnection.setRequestProperty("Cookie", "connect.sid=$sid;cid=$cid")
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