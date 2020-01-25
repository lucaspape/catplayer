package de.lucaspape.monstercat.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.squareup.picasso.Picasso
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.Settings
import de.lucaspape.monstercat.util.wifiConnected
import java.io.File
import java.io.FileOutputStream

//this lists contain the urls that should be downloaded
internal val downloadList = ArrayList<HashMap<String, Any?>?>()

internal val targetList = ArrayList<com.squareup.picasso.Target>()

fun addDownloadSong(url: String, location: String, shownTitle: String) {
    val downloadTrack = HashMap<String, Any?>()
    downloadTrack["url"] = url
    downloadTrack["location"] = location
    downloadTrack["shownTitle"] = shownTitle

    downloadList.add(downloadTrack)
}

fun downloadCoverIntoImageView(
    context: Context,
    imageView: ImageView,
    albumId: String,
    lowRes: Boolean
) {
    val settings = Settings(context)
    val resolution = if (!lowRes) {
        settings.getSetting("primaryResolution")
    } else {
        settings.getSetting("secondaryResolution")
    }

    val placeholder = if(!lowRes){
        Drawable.createFromPath(context.dataDir.toString() + "/fallback.jpg")
    }else{
        Drawable.createFromPath(context.dataDir.toString() + "/fallback_low.jpg")
    }

    val url = context.getString(R.string.trackContentUrl) + "$albumId/cover"

    val cacheFile = File(context.cacheDir.toString() + "/$albumId.png.$resolution")

    if (cacheFile.exists()) {
        val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
        imageView.setImageBitmap(bitmap)
    } else {
        if (wifiConnected(context) == true || settings.getSetting("downloadCoversOverMobile") == "true") {
            val picassoTarget = object : com.squareup.picasso.Target {
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                    imageView.setImageDrawable(placeHolderDrawable)
                }

                override fun onBitmapFailed(errorDrawable: Drawable?) {
                    imageView.setImageDrawable(errorDrawable)
                }

                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    imageView.setImageBitmap(bitmap)

                    //possible that it changed by this time
                    if (!cacheFile.exists()) {
                        FileOutputStream(cacheFile).use { out ->
                            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                }
            }

            //to prevent garbage collect
            targetList.add(picassoTarget)

            Picasso.with(context)
                .load("$url?image_width=$resolution")
                .placeholder(placeholder)
                .into(picassoTarget)
        } else {
            imageView.setImageDrawable(placeholder)
        }
    }
}

fun downloadCoverIntoBitmap(
    context: Context,
    downloadFinished: (bitmap: Bitmap) -> Unit,
    albumId: String,
    lowRes: Boolean
) {
    val settings = Settings(context)

    val resolution = if (!lowRes) {
        settings.getSetting("primaryResolution")
    } else {
        settings.getSetting("secondaryResolution")
    }

    val url = context.getString(R.string.trackContentUrl) + "$albumId/cover"

    val cacheFile = File(context.cacheDir.toString() + "/$albumId.png.$resolution")

    if (cacheFile.exists()) {
        val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
        downloadFinished(bitmap)
    } else {
        if (wifiConnected(context) == true || settings.getSetting("downloadCoversOverMobile") == "true") {
            val picassoTarget = object : com.squareup.picasso.Target {
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                }

                override fun onBitmapFailed(errorDrawable: Drawable?) {
                }

                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    bitmap?.let {
                        downloadFinished(bitmap)

                        //possible that it changed by this time
                        if (!cacheFile.exists()) {
                            FileOutputStream(cacheFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                        }
                    }
                }
            }

            //prevent garbage collect
            targetList.add(picassoTarget)

            Picasso.with(context)
                .load("$url?image_width=$resolution")
                .into(picassoTarget)
        } else {
            val bitmap = BitmapFactory.decodeFile(context.dataDir.toString() + "/fallback.jpg")
            downloadFinished(bitmap)
        }
    }
}