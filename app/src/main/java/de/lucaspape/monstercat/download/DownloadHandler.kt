package de.lucaspape.monstercat.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.widget.ImageView
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.background.BackgroundService
import de.lucaspape.monstercat.handlers.abstract_items.ViewHolderInterface
import de.lucaspape.monstercat.util.Settings
import de.lucaspape.monstercat.util.wifiConnected
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

//TODO cancel downloads

//this lists contain the urls that should be downloaded
internal val downloadList = ArrayList<SoftReference<DownloadObject>>()
internal val streamDownloadList = ArrayList<SoftReference<DownloadObject>>()

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

fun addStreamDownloadSong(context: Context, songId: String, downloadFinished: () -> Unit) {
    streamDownloadList.add(SoftReference(DownloadObject(songId, downloadFinished)))

    if (BackgroundService.downloadTask?.status != AsyncTask.Status.RUNNING) {
        BackgroundService.downloadTask = DownloadTask(WeakReference(context))
        BackgroundService.downloadTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}

fun downloadCoverIntoAbstractItem(
    context: Context,
    viewHolder: ViewHolderInterface,
    albumId: String,
    lowRes: Boolean
) {
    val settings = Settings(context)
    val resolution = if (!lowRes) {
        settings.getSetting(context.getString(R.string.primaryCoverResolutionSetting))
    } else {
        settings.getSetting(context.getString(R.string.secondaryCoverResolutionSetting))
    }

    val cacheBitmap = bitmapCache[albumId + resolution]?.get()

    if (cacheBitmap != null) {
        viewHolder.setCoverBitmap(albumId, cacheBitmap)
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
            viewHolder.setCoverBitmap(albumId, bitmap)
            bitmapCache[albumId + resolution] = SoftReference(bitmap)
        } else {
            if (wifiConnected(context) == true || settings.getSetting(context.getString(R.string.downloadCoversOverMobileSetting)) == "true") {
                val picassoTarget = object : com.squareup.picasso.Target {
                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                        viewHolder.setCoverDrawable(albumId, placeHolderDrawable)
                    }

                    override fun onBitmapFailed(errorDrawable: Drawable?) {
                        viewHolder.setCoverDrawable(albumId, errorDrawable)
                    }

                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        viewHolder.setCoverBitmap(albumId, bitmap)

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

                Picasso.with(context)
                    .load("$url?image_width=$resolution")
                    .placeholder(placeholder)
                    .into(picassoTarget)
            } else {
                viewHolder.setCoverDrawable(albumId, placeholder)
            }
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
        settings.getSetting(context.getString(R.string.primaryCoverResolutionSetting))
    } else {
        settings.getSetting(context.getString(R.string.secondaryCoverResolutionSetting))
    }

    val cacheBitmap = bitmapCache[albumId + resolution]?.get()

    if (cacheBitmap != null) {
        downloadFinished(cacheBitmap)
    } else {
        val url = context.getString(R.string.trackContentUrl) + "$albumId/cover"

        val cacheFile = File(context.cacheDir.toString() + "/$albumId.png.$resolution")

        if (cacheFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
            downloadFinished(bitmap)
            bitmapCache[albumId + resolution] = SoftReference(bitmap)
        } else {
            if (wifiConnected(context) == true || settings.getSetting(context.getString(R.string.downloadCoversOverMobileSetting)) == "true") {
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

                            bitmapCache[albumId + resolution] = SoftReference(bitmap)
                        }
                    }
                }

                //prevent garbage collect
                targetList.add(SoftReference(picassoTarget))

                Picasso.with(context)
                    .load("$url?image_width=$resolution")
                    .into(picassoTarget)
            } else {
                val bitmap = BitmapFactory.decodeFile(context.dataDir.toString() + "/fallback.jpg")
                downloadFinished(bitmap)
            }
        }
    }
}