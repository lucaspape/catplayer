package de.lucaspape.monstercat.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import com.squareup.picasso.Picasso
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.background.BackgroundService
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
internal var streamDownloadedSongs = 0

internal val targetList = ArrayList<SoftReference<com.squareup.picasso.Target>>()
internal val bitmapCache = HashMap<String, SoftReference<Bitmap?>>()

fun addDownloadSong(context: Context, songId: String, downloadFinished: () -> Unit) {
    downloadList.add(SoftReference(DownloadObject(songId, downloadFinished)))

    if (BackgroundService.downloadTask?.status != AsyncTask.Status.RUNNING) {
        BackgroundService.downloadTask = DownloadTask(WeakReference(context))
        BackgroundService.downloadTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}

fun downloadArtistImageIntoImageReceiver(
    context: Context,
    imageReceiver: ImageReceiverInterface,
    artistId: String
) {
    val cacheBitmap = bitmapCache[artistId]?.get()

    val settings = Settings(context)

    if (cacheBitmap != null) {
        imageReceiver.setBitmap(artistId, cacheBitmap)
    } else {
        //TODO placeholder

        val url = context.getString(R.string.artistContentUrl) + "$artistId/image"

        val cacheFile = File(context.cacheDir.toString() + "/$artistId.png")

        if (cacheFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
            imageReceiver.setBitmap(artistId, bitmap)
            bitmapCache[artistId] = SoftReference(bitmap)
        } else {
            if (wifiConnected(context) == true || settings.getBoolean(context.getString(R.string.downloadCoversOverMobileSetting)) == true) {
                val picassoTarget = object : com.squareup.picasso.Target {
                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                        imageReceiver.setDrawable(artistId, placeHolderDrawable)
                    }

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        imageReceiver.setDrawable(artistId, errorDrawable)
                    }

                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        imageReceiver.setBitmap(artistId, bitmap)

                        //possible that it changed by this time
                        if (!cacheFile.exists()) {
                            FileOutputStream(cacheFile).use { out ->
                                bitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                        }

                        bitmapCache[artistId] = SoftReference(bitmap)
                    }
                }

                //to prevent garbage collect
                targetList.add(SoftReference(picassoTarget))

                Picasso.get()
                    .load("$url?image_width=256")
                    .into(picassoTarget)
            } else {
                imageReceiver.setDrawable(artistId, null)
            }
        }
    }
}

fun downloadCoverIntoImageReceiver(
    context: Context,
    imageReceiver: ImageReceiverInterface,
    albumId: String,
    lowRes: Boolean
) {
    val settings = Settings(context)
    val resolution = if (!lowRes) {
        settings.getInt(context.getString(R.string.primaryCoverResolutionSetting))
    } else {
        settings.getInt(context.getString(R.string.secondaryCoverResolutionSetting))
    }

    val cacheBitmap = bitmapCache[albumId + resolution]?.get()

    if (cacheBitmap != null) {
        imageReceiver.setBitmap(albumId, cacheBitmap)
    } else {
        val placeholder = if (!lowRes) {
            Drawable.createFromPath(context.dataDir.toString() + "/fallback.jpg")
        } else {
            Drawable.createFromPath(context.dataDir.toString() + "/fallback_low.jpg")
        }

        val url = context.getString(R.string.trackContentUrl) + "$albumId/cover"

        val cacheFile = File(context.cacheDir.toString() + "/$albumId.png.$resolution")

        if (cacheFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
            imageReceiver.setBitmap(albumId, bitmap)
            bitmapCache[albumId + resolution] = SoftReference(bitmap)
        } else {
            if (wifiConnected(context) == true || settings.getBoolean(context.getString(R.string.downloadCoversOverMobileSetting)) == true) {
                val picassoTarget = object : com.squareup.picasso.Target {
                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                        imageReceiver.setDrawable(albumId, placeHolderDrawable)
                    }

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        imageReceiver.setDrawable(albumId, errorDrawable)
                    }

                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        imageReceiver.setBitmap(albumId, bitmap)

                        //possible that it changed by this time
                        if (!cacheFile.exists()) {
                            FileOutputStream(cacheFile).use { out ->
                                bitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                        }

                        bitmapCache[albumId + resolution] = SoftReference(bitmap)
                    }
                }

                //to prevent garbage collect
                targetList.add(SoftReference(picassoTarget))

                if (placeholder != null) {
                    Picasso.get()
                        .load("$url?image_width=$resolution")
                        .placeholder(placeholder)
                        .into(picassoTarget)
                } else {
                    Picasso.get()
                        .load("$url?image_width=$resolution")
                        .into(picassoTarget)
                }
            } else {
                imageReceiver.setDrawable(albumId, placeholder)
            }
        }
    }
}