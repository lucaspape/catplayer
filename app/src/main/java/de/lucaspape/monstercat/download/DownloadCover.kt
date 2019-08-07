package de.lucaspape.monstercat.download

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.widget.SimpleAdapter
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class DownloadCover(private val url: String, private val location: String, private val simpleAdapter: SimpleAdapter) : AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg params: Void?): String? {
        try {

            val url = URL(url)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)

            FileOutputStream(location).use { out ->
                bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
            }


        } catch (e: IOException) {
            // Log exception
            return null
        }

        return null
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        simpleAdapter.notifyDataSetChanged()
    }
}