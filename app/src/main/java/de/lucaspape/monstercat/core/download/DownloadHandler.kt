package de.lucaspape.monstercat.core.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.core.net.toUri
import com.squareup.picasso.Picasso
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.download.DownloadService.Companion.downloadTask
import de.lucaspape.monstercat.core.util.wifiConnected
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.core.util.BackgroundAsync
import de.lucaspape.monstercat.ui.fallbackDrawable
import de.lucaspape.monstercat.ui.fallbackLowDrawable
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

//TODO cancel downloads

//this lists contain the urls that should be downloaded
val downloadList = ArrayList<SoftReference<DownloadObject>>()

var downloadedSongs = 0

val preDownloadCallbacks = HashMap<String, () -> Unit>()

private var bitmapCache: LruCache<String, Bitmap>? = null

fun addDownloadSong(context: Context, songId: String, downloadFinished: () -> Unit) {
    if (SongDatabaseHelper(context).getSong(songId)?.isDownloadable == true) {
        preDownloadCallbacks[songId]?.let {
            it()
        }

        downloadList.add(SoftReference(DownloadObject(songId, downloadFinished)))

        if (downloadTask?.active != true) {
            downloadTask = DownloadTask(WeakReference(context))
            downloadTask?.execute()
        }
    }
}

fun downloadImageUrlIntoImageReceiver(
    context: Context,
    imageReceiver: ImageReceiverInterface,
    lowRes: Boolean,
    imageId: String,
    url: String
) {
    val settings = Settings.getSettings(context)
    var resolution = if (!lowRes) {
        settings.getInt(context.getString(R.string.primaryCoverResolutionSetting))
    } else {
        settings.getInt(context.getString(R.string.secondaryCoverResolutionSetting))
    }

    if (resolution == null) {
        resolution = 512
    }

    var saveToCache = settings.getBoolean(context.getString(R.string.saveCoverImagesToCacheSetting))

    if (saveToCache == null) {
        saveToCache = true
    }

    if (bitmapCache == null) {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8

        bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {

            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.byteCount / 1024
            }
        }
    }

    val cacheBitmap = bitmapCache?.get(imageId + resolution)

    if (cacheBitmap != null) {
        imageReceiver.setBitmap(imageId, cacheBitmap)
    } else {
        val placeholder = if (lowRes) {
            fallbackLowDrawable
        } else {
            fallbackDrawable
        }

        imageReceiver.setDrawable(imageId, Drawable.createFromStream(context.contentResolver.openInputStream(placeholder.toUri()), placeholder))

        val cacheFile = File(context.cacheDir.toString() + "/$imageId-$resolution.webp")

        if (cacheFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)

            val scaledBitmap = if(bitmap != null && bitmap.width != resolution || bitmap.height != resolution){
                val tempBitmap = Bitmap.createScaledBitmap(bitmap, resolution, resolution, false)

                saveBitmapAsync(tempBitmap, cacheFile)

                tempBitmap
            }else{
                bitmap
            }

            imageReceiver.setBitmap(imageId, scaledBitmap)

            if (saveToCache) {
                bitmapCache?.put(imageId + resolution, scaledBitmap)
            }

        } else {
            if (wifiConnected(context) || settings.getBoolean(context.getString(R.string.downloadCoversOverMobileSetting)) == true) {
                val picassoTarget = object : com.squareup.picasso.Target {
                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                    }

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {

                    }

                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        bitmap?.let {
                            val scaledBitmap =
                                if (bitmap.width != resolution || bitmap.height != resolution) {
                                    Bitmap.createScaledBitmap(bitmap, resolution, resolution, false)
                                } else {
                                    bitmap
                                }

                            imageReceiver.setBitmap(imageId, scaledBitmap)

                            //possible that it changed by this time
                            if (!cacheFile.exists() && saveToCache) {

                                saveBitmapAsync(scaledBitmap, cacheFile)
                            }

                            if (saveToCache) {
                                bitmapCache?.put(imageId + resolution, scaledBitmap)
                            }
                        }
                    }
                }

                Picasso.get()
                    .load("$url?image_width=$resolution")
                    .into(picassoTarget)
                imageReceiver.setTag(picassoTarget)
            }
        }
    }
}

@Suppress("DEPRECATION")
fun saveBitmapAsync(bitmap: Bitmap, outputFile: File) {
    BackgroundAsync {
        FileOutputStream(outputFile).use { out ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, out)
            } else {
                bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out)
            }
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
        if (settings.getBoolean(context.getString(R.string.useCustomApiForCoverImagesSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV1Setting)
            ) == true
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