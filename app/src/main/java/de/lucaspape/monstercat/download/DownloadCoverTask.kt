package de.lucaspape.monstercat.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.view.View
import android.widget.ListView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL

//TODO add imageview updater
class DownloadCoverTask(private val weakReference: WeakReference<Context>) : AsyncTask<Void, Void, String>(){
    override fun doInBackground(vararg p0: Void?): String? {
        val context = weakReference.get()

        var downloadedCovers = 0
        var downloadedCoverArrays = 0

        while(true){
            try{
                if(DownloadHandler.downloadCoverList[downloadedCovers].isNotEmpty()){
                    val cover = DownloadHandler.downloadCoverList[downloadedCovers]

                    val url = cover["coverUrl"] as String
                    val location = cover["location"] as String

                    val primaryRes = cover["primaryRes"] as String
                    val secondaryRes = cover["secondaryRes"] as String

                    downloadCover(url, location, primaryRes, secondaryRes)
                    downloadedCovers++
                }
            }catch(e:IndexOutOfBoundsException){
            }

            try {
                if(DownloadHandler.downloadCoverArrayListList[downloadedCoverArrays].isNotEmpty()){
                    val coverArray = DownloadHandler.downloadCoverArrayListList[downloadedCoverArrays]

                    for(i in coverArray.indices){
                        val cover = coverArray[i]

                        val url = cover["coverUrl"] as String
                        val location = cover["location"] as String

                        val primaryRes = cover["primaryRes"] as String
                        val secondaryRes = cover["secondaryRes"] as String

                        downloadCover(url, location, primaryRes, secondaryRes)
                    }

                    downloadedCoverArrays++
                }
            }catch(e:IndexOutOfBoundsException){

            }

            Thread.sleep(500)
        }
    }

    private fun downloadCover(downloadUrl: String, location: String, primaryRes:String, secondaryRes:String):Boolean{
        try {
            if(!File(location + primaryRes).exists() || !File(location + secondaryRes).exists()){
                val url = URL("$downloadUrl?image_width=$primaryRes")
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val primaryBitmap = BitmapFactory.decodeStream(input)

                FileOutputStream(location + primaryRes).use { out ->
                    primaryBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val secondaryBitmap = Bitmap.createScaledBitmap(primaryBitmap, secondaryRes.toInt(), secondaryRes.toInt(), false)

                FileOutputStream(location + secondaryRes).use { out ->
                    secondaryBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }

            return true
        } catch (e: IOException) {
            // Log exception
            return false
        }
    }

}