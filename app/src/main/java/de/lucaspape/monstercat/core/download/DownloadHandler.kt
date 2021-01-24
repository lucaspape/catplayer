package de.lucaspape.monstercat.core.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import com.squareup.picasso.Picasso
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.download.DownloadService.Companion.downloadTask
import de.lucaspape.monstercat.core.util.wifiConnected
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.core.util.BackgroundAsync
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

//TODO cancel downloads

//this lists contain the urls that should be downloaded
internal val downloadList = ArrayList<SoftReference<DownloadObject>>()

internal var downloadedSongs = 0

internal val bitmapCache = HashMap<String, SoftReference<Bitmap?>>()

internal val preDownloadCallbacks = HashMap<String, ()->Unit>()

var fallbackFile = File("")
var fallbackFileLow = File("")

fun addDownloadSong(context: Context, songId: String, downloadFinished: () -> Unit) {
    preDownloadCallbacks[songId]?.let {
        it()
    }

    downloadList.add(SoftReference(DownloadObject(songId, downloadFinished)))

    if (downloadTask?.active != true) {
        downloadTask = DownloadTask(WeakReference(context))
        downloadTask?.execute()
    }
}

internal fun downloadImageUrlIntoImageReceiver(
    context: Context,
    imageReceiver: ImageReceiverInterface,
    lowRes: Boolean,
    imageId: String,
    url: String
) {
    val settings = Settings.getSettings(context)
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
            Drawable.createFromPath(fallbackFile.absolutePath)
        } else {
            Drawable.createFromPath(fallbackFileLow.absolutePath)
        }

        val cacheFile = File(context.cacheDir.toString() + "/$imageId-$resolution.webp")

        if (cacheFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
            imageReceiver.setBitmap(imageId, bitmap)

            if(saveToCache){
                bitmapCache[imageId + resolution] = SoftReference(bitmap)
            }

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
                            bitmap?.let {
                                saveBitmapAsync(it, cacheFile)
                            }
                        }

                        if(saveToCache){
                            bitmapCache[imageId + resolution] = SoftReference(bitmap)
                        }
                    }
                }

                if (placeholder != null) {
                    Picasso.get()
                        .load("$url?image_width=$resolution")
                        .error(placeholder)
                        .placeholder(placeholder)
                        .into(picassoTarget)
                    imageReceiver.setTag(picassoTarget)
                } else {
                    Picasso.get()
                        .load("$url?image_width=$resolution")
                        .into(picassoTarget)
                    imageReceiver.setTag(picassoTarget)
                }
            } else {
                imageReceiver.setDrawable(imageId, placeholder)
            }
        }
    }
}

fun saveBitmapAsync(bitmap: Bitmap, outputFile: File){
    BackgroundAsync {
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
    }.execute()
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
    val settings = Settings.getSettings(context)

    val url =
        if (settings.getBoolean(context.getString(R.string.useCustomApiSetting)) == true && settings.getBoolean(context.getString(R.string.customApiSupportsV1Setting)) == true
        ) {
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v1/release/" + albumId + "/cover"
        } else {
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