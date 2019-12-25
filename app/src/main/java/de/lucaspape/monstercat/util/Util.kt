package de.lucaspape.monstercat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

fun wifiConnected(context: Context): Boolean? {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

    return wifi?.isConnected
}

fun downloadCover(
    downloadUrl: String,
    location: String,
    primaryRes: String,
    secondaryRes: String
): Boolean {
    try {
        if (!File(location + primaryRes).exists() || !File(location + secondaryRes).exists()) {
            val connection =
                URL("$downloadUrl?image_width=$primaryRes").openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val primaryBitmap = BitmapFactory.decodeStream(input)

            FileOutputStream(location + primaryRes).use { out ->
                primaryBitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val secondaryBitmap =
                Bitmap.createScaledBitmap(
                    primaryBitmap,
                    secondaryRes.toInt(),
                    secondaryRes.toInt(),
                    false
                )

            FileOutputStream(location + secondaryRes).use { out ->
                secondaryBitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            connection.disconnect()
        }

        return true
    } catch (e: IOException) {
        // Log exception
        return false
    }
}

fun downloadFile(destination:String, source:String, tempDir:String, sid:String, progressUpdate: (max:Int, current:Int) -> Unit){
    val destinationFile = File(destination)
    val tempFile = File(tempDir + "/" + UUID.randomUUID().toString())

    if(!File(destination).exists()){
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

            if(updateCount > 100){
                updateCount = 0
                progressUpdate(lengthOfFile, total.toInt())
            }
        } while (true)

        fileOutputStream.flush()
        fileOutputStream.close()
        bufferedInputStream.close()

        tempFile.renameTo(destinationFile)
    }
}