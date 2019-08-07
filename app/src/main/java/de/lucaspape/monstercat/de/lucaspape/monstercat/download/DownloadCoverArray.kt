package de.lucaspape.monstercat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.widget.SimpleAdapter
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadCoverArray(private val covers: ArrayList<HashMap<String, Any?>?>,
                         private val simpleAdapter: SimpleAdapter
): AsyncTask<Void, Void, String>(){

    override fun doInBackground(vararg p0: Void?): String? {
        for(i in covers.indices){
            if(covers[i] != null){
                val primaryResolution = covers[i]!!["primaryRes"] as String
                val secondaryResolution = covers[i]!!["secondaryRes"] as String

                val url = URL(covers[i]!!["coverUrl"] as String + "?image_width=" + primaryResolution)
                val location = covers[i]!!["location"] as String

                println("Downloading... $url")

                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val primaryBitmap = BitmapFactory.decodeStream(input)

                FileOutputStream(location + primaryResolution).use { out ->
                    primaryBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val secondaryBitmap = Bitmap.createScaledBitmap(primaryBitmap, secondaryResolution.toInt(), secondaryResolution.toInt(), false)

                FileOutputStream(location + secondaryResolution).use { out ->
                    secondaryBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                println("Finished download!")
            }

        }

        return null
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        simpleAdapter.notifyDataSetChanged()
    }

}