package de.lucaspape.monstercat.download

import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
    context: Context
) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val downloadUri = Uri.parse(url)

    val downloadRequest = DownloadManager.Request(downloadUri)

    downloadRequest.addRequestHeader("Cookie", "connect.sid=$sid")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationUri(Uri.parse("file://$location"))

    downloadManager.enqueue(downloadRequest)
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