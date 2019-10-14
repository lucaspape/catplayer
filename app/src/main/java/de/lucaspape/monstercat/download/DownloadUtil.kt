package de.lucaspape.monstercat.download

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal fun downloadSong(
    url: String,
    location: String,
    sid: String
): Boolean {
    try {
        val urlConnection = URL(url).openConnection() as HttpURLConnection
        urlConnection.setRequestProperty("Cookie", "connect.sid=$sid")

        urlConnection.doInput = true
        urlConnection.connect()

        val bis = BufferedInputStream(urlConnection.inputStream)
        val fos = FileOutputStream(File(location))

        val dataBuffer = ByteArray(1024)
        var bytesRead:Int

        bytesRead = bis.read(dataBuffer, 0, 1024)

        while (bytesRead != -1) {
            fos.write(dataBuffer, 0, bytesRead)
            bytesRead = bis.read(dataBuffer, 0, 1024)
        }

        return true

    } catch (e: IOException) {
        println(e)
        return false
    }
}

internal fun downloadCover(
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
                primaryBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val secondaryBitmap =
                Bitmap.createScaledBitmap(
                    primaryBitmap,
                    secondaryRes.toInt(),
                    secondaryRes.toInt(),
                    false
                )

            FileOutputStream(location + secondaryRes).use { out ->
                secondaryBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            connection.disconnect()
        }

        return true
    } catch (e: IOException) {
        // Log exception
        return false
    }
}