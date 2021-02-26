package de.lucaspape.monstercat.core.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.squareup.picasso.Picasso
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.download.DownloadService.Companion.downloadTask
import de.lucaspape.monstercat.core.util.wifiConnected
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.core.util.BackgroundAsync
import de.lucaspape.monstercat.core.util.downloadFile
import de.lucaspape.monstercat.ui.fallbackBlackFile
import de.lucaspape.monstercat.ui.fallbackBlackFileLow
import de.lucaspape.monstercat.ui.fallbackWhiteFile
import de.lucaspape.monstercat.ui.fallbackWhiteFileLow
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

//TODO cancel downloads

//this lists contain the urls that should be downloaded
val downloadList = ArrayList<SoftReference<DownloadObject>>()

var downloadedSongs = 0

val bitmapCache = HashMap<String, SoftReference<Bitmap?>>()

val preDownloadCallbacks = HashMap<String, ()->Unit>()

var fallbackFile = File("")
var fallbackFileLow = File("")

fun addDownloadSong(context: Context, songId: String, downloadFinished: () -> Unit) {
    if(SongDatabaseHelper(context).getSong(context, songId)?.isDownloadable == true){
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

        if (placeholder != null) {
            imageReceiver.setBitmap(imageId, (placeholder as BitmapDrawable).bitmap)
        }

        val cacheFile = File(context.cacheDir.toString() + "/$imageId-$resolution.webp")

        if (cacheFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
            imageReceiver.setBitmap(imageId, bitmap)

            if(saveToCache){
                bitmapCache[imageId + resolution] = SoftReference(bitmap)
            }

        } else {
            if (wifiConnected(context) || settings.getBoolean(context.getString(R.string.downloadCoversOverMobileSetting)) == true) {
                val picassoTarget = object : com.squareup.picasso.Target {
                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                    }

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {

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

                Picasso.get()
                    .load("$url?image_width=$resolution")
                    .into(picassoTarget)
                imageReceiver.setTag(picassoTarget)
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
        if (settings.getBoolean(context.getString(R.string.useCustomApiForCoverImagesSetting)) == true && settings.getBoolean(context.getString(R.string.customApiSupportsV1Setting)) == true
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

fun downloadFallbackCoverImagesAsync(context: Context, callback:() -> Unit) {
    if (!fallbackBlackFile.exists() || !fallbackBlackFileLow.exists()) {
        BackgroundAsync({
            downloadFile(
                fallbackBlackFile.absolutePath,
                context.getString(R.string.fallbackCoverBlackUrl),
                context.cacheDir.toString(),
                "",
                ""
            ) { _, _ ->
            }
        }, {
            FileOutputStream(fallbackBlackFileLow).use { out ->
                val originalBitmap = BitmapFactory.decodeFile(fallbackBlackFile.absolutePath)
                originalBitmap?.let {
                    Bitmap.createScaledBitmap(it, 128, 128, false)
                        .compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
            }

            callback()
        }).execute()
    }

    if (!fallbackWhiteFile.exists() || !fallbackWhiteFileLow.exists()) {
        BackgroundAsync({
            downloadFile(
                fallbackWhiteFile.absolutePath,
                context.getString(R.string.fallbackCoverUrl),
                context.cacheDir.toString(),
                "",
                ""
            ) { _, _ ->
            }
        }, {
            FileOutputStream(fallbackWhiteFileLow).use { out ->
                val originalBitmap = BitmapFactory.decodeFile(fallbackWhiteFile.absolutePath)
                originalBitmap?.let {
                    Bitmap.createScaledBitmap(it, 128, 128, false)
                        .compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
            }

            callback()
        }).execute()
    }
}