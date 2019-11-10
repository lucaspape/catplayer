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
    sid: String,
    shownTitle:String,
    downloadUpdate:(shownTitle:String, max:Int, current:Int) -> Unit
) {
    if(!File(location).exists()){
        val urlConnection = URL(url).openConnection()
        urlConnection.setRequestProperty("Cookie", "connect.sid=$sid")
        urlConnection.connect()

        val lengthOfFile = urlConnection.contentLength

        val bufferedInputStream = BufferedInputStream(urlConnection.getInputStream(), 8192)

        val fileOutputStream = FileOutputStream(File(location))

        val data = ByteArray(1024)
        var total:Long = 0
        var count: Int

        do {
            count = bufferedInputStream.read(data)

            if(count == -1){
                break
            }

            total += count

            fileOutputStream.write(data, 0, count)

            downloadUpdate(shownTitle, lengthOfFile, total.toInt())
        } while(true)

        fileOutputStream.flush()
        fileOutputStream.close()
        bufferedInputStream.close()
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