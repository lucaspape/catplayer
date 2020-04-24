package de.lucaspape.monstercat.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import com.squareup.picasso.Picasso
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.download.DownloadService.Companion.downloadTask
import de.lucaspape.monstercat.util.Settings
import de.lucaspape.monstercat.util.wifiConnected
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

//TODO cancel downloads

//this lists contain the urls that should be downloaded
internal val downloadList = ArrayList<SoftReference<DownloadObject>>()

internal var downloadedSongs = 0

internal val targetList = ArrayList<SoftReference<com.squareup.picasso.Target>>()
internal val bitmapCache = HashMap<String, SoftReference<Bitmap?>>()

fun addDownloadSong(context: Context, songId: String, downloadFinished: () -> Unit) {
    downloadList.add(SoftReference(DownloadObject(songId, downloadFinished)))

    if (downloadTask?.status != AsyncTask.Status.RUNNING) {
        downloadTask = DownloadTask(WeakReference(context))
        downloadTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}

internal fun downloadImageUrlIntoImageReceiver(
    context: Context,
    imageReceiver: ImageReceiverInterface,
    lowRes: Boolean,
    imageId: String,
    url: String
) {
    val settings = Settings(context)
    val resolution = if (!lowRes) {
        settings.getInt(context.getString(R.string.primaryCoverResolutionSetting))
    } else {
        settings.getInt(context.getString(R.string.secondaryCoverResolutionSetting))
    }

    var saveToCache = settings.getBoolean(context.getString(R.string.saveCoverImagesToCacheSetting))

    if(saveToCache == null){
        saveToCache = true
    }

    val cacheBitmap = bitmapCache[imageId + resolution]?.get()

    if (cacheBitmap != null) {
        imageReceiver.setBitmap(imageId, cacheBitmap)
    } else {
        val placeholder = if (!lowRes) {
            Drawable.createFromPath(context.dataDir.toString() + "/fallback.webp")
        } else {
            Drawable.createFromPath(context.dataDir.toString() + "/fallback_low.webp")
        }

        val cacheFile = File(context.cacheDir.toString() + "/$imageId-$resolution.webp")

        if (cacheFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
            imageReceiver.setBitmap(imageId, bitmap)
            bitmapCache[imageId + resolution] = SoftReference(bitmap)
        } else {
            if (wifiConnected(context) == true || settings.getBoolean(context.getString(R.string.downloadCoversOverMobileSetting)) == true) {
                val picassoTarget = object : com.squareup.picasso.Target {
                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                        imageReceiver.setDrawable(imageId, placeHolderDrawable)
                    }

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        imageReceiver.setDrawable(imageId, errorDrawable)
                    }

                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        imageReceiver.setBitmap(imageId, bitmap)

                        //possible that it changed by this time
                        if (!cacheFile.exists() && saveToCache) {
                            FileOutputStream(cacheFile).use { out ->
                                bitmap?.compress(Bitmap.CompressFormat.WEBP, 100, out)
                            }
                        }

                        bitmapCache[imageId + resolution] = SoftReference(bitmap)
                    }
                }

                //to prevent garbage collect
                targetList.add(SoftReference(picassoTarget))

                if (placeholder != null) {
                    Picasso.get()
                        .load("$url?image_width=$resolution")
                        .error(placeholder)
                        .placeholder(placeholder)
                        .into(picassoTarget)
                } else {
                    Picasso.get()
                        .load("$url?image_width=$resolution")
                        .into(picassoTarget)
                }
            } else {
                imageReceiver.setDrawable(imageId, placeholder)
            }
        }
    }
}

fun downloadArtistImageIntoImageReceiver(
    context: Context,
    imageReceiver: ImageReceiverInterface,
    artistId: String
) {
    downloadImageUrlIntoImageReceiver(
        context,
        imageReceiver,
        true,
        artistId,
        context.getString(R.string.artistContentUrl) + "$artistId/image"
    )
}

fun downloadCoverIntoImageReceiver(
    context: Context,
    imageReceiver: ImageReceiverInterface,
    albumId: String,
    lowRes: Boolean
) {
    val url = if(Settings(context).getBoolean(context.getString(R.string.useCustomApiSetting)) == true){
        context.getString(R.string.customApiBaseUrl) + "release/" + albumId + "/cover"
    }else{
        context.getString(R.string.trackContentUrl) + "$albumId/cover"
    }

    downloadImageUrlIntoImageReceiver(
        context,
        imageReceiver,
        lowRes,
        albumId,
        url
    )
}