package de.lucaspape.monstercat.music

import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.settings.Settings
import java.io.File
import java.io.FileInputStream
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import kotlin.Comparator
import kotlin.collections.ArrayList

//TODO move notification stuff to different file

var contextReference: WeakReference<Context>? = null
private var textView1Reference: WeakReference<TextView>? = null
private var textView2Reference: WeakReference<TextView>? = null
private var seekBarReference: WeakReference<SeekBar>? = null
private var barCoverImageReference: WeakReference<ImageView>? = null
private var musicBarReference: WeakReference<androidx.appcompat.widget.Toolbar>? = null
private var playButtonReference: WeakReference<ImageButton>? = null

private var mediaPlayer = MediaPlayer()
private var currentSong = 0
private var playList = ArrayList<Song>(1)
private var playing = false
private var paused = false

//notification var
const val channelID = "Music Notification"
const val notificationID = 1

const val NOTIFICATION_PREVIOUS = "de.lucaspape.monstercat.previous"
const val NOTIFICATION_DELETE = "de.lucaspape.monstercat.delete"
const val NOTIFICATION_PAUSE = "de.lucaspape.monstercat.pause"
const val NOTIFICATION_PLAY = "de.lucaspape.monstercat.play"
const val NOTIFICATION_NEXT = "de.lucaspape.monstercat.next"

private var mediaSession: MediaSessionCompat? = null

/**
 * Checks if headphones unplugged
 * TODO unRegisterReceiver
 */
class NoisyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            pause()
        }
    }
}

/**
 * Create mediaSession and listen for callbacks (pause, play buttons on headphones etc.)
 */
fun createMediaSession(context: WeakReference<Context>) {
    mediaSession = MediaSessionCompat.fromMediaSession(
        context.get()!!,
        MediaSession(context.get()!!, "de.lucaspape.monstercat.music")
    )

    mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {

        override fun onPause() {
            pause()
        }

        override fun onPlay() {
            if (paused) {
                resume()
            } else {
                play()
            }
        }

        override fun onSkipToNext() {
            next()
        }

        override fun onSkipToPrevious() {
            previous()
        }

        override fun onStop() {
            stop()
        }

        override fun onSeekTo(pos: Long) {
            mediaPlayer.seekTo(pos.toInt())
        }

        override fun onFastForward() {
            mediaPlayer.seekTo(mediaPlayer.duration)
        }

        override fun onRewind() {
            super.onRewind()
            mediaPlayer.seekTo(0)
        }

    })

    mediaSession!!.isActive = true
}


/**
 * UI update methods
 */
fun setTextView(newTextView1: TextView, newTextView2: TextView) {
    try {
        newTextView1.text = textView1Reference!!.get()!!.text
        newTextView2.text = textView2Reference!!.get()!!.text
    } catch (e: NullPointerException) {

    }

    textView1Reference = WeakReference(newTextView1)
    textView2Reference = WeakReference(newTextView2)
}

fun setSeekBar(newSeekBar: SeekBar) {
    try {
        newSeekBar.progress = seekBarReference!!.get()!!.progress
    } catch (e: NullPointerException) {

    }

    newSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser)
                mediaPlayer.seekTo(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
        }
    })

    seekBarReference = WeakReference(newSeekBar)
}

fun setBarCoverImageView(newImageView: ImageView) {
    try {
        newImageView.setImageDrawable(barCoverImageReference!!.get()!!.drawable)
    } catch (e: NullPointerException) {

    }

    barCoverImageReference = WeakReference(newImageView)
}

fun setMusicBar(newToolbar: androidx.appcompat.widget.Toolbar) {
    try {
        newToolbar.setBackgroundColor((musicBarReference!!.get()!!.background as ColorDrawable).color)
    } catch (e: NullPointerException) {

    }
    musicBarReference = WeakReference(newToolbar)
}

fun setPlayButton(newPlayButton: ImageButton) {
    try {
        newPlayButton.setImageDrawable(playButtonReference!!.get()!!.drawable)
    } catch (e: NullPointerException) {

    }

    playButtonReference = WeakReference(newPlayButton)
}

/**
 * Music control methods
 */
private fun play() {
    try {
        val song = playList[currentSong]
        val settings = Settings(contextReference!!.get()!!)
        val url = song.getUrl()

        mediaPlayer.stop()
        mediaPlayer = MediaPlayer()

        val connectivityManager =
            contextReference!!.get()!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        if (!File(url).exists()) {
            if (wifi != null && !wifi.isConnected && settings.getSetting("streamOverMobile") != "true") {
                //TODO msg
                println("DONT STREAM")
                return
            }else{
                mediaPlayer.setDataSource(url)
            }
        }else{
            val fis = FileInputStream(File(url))
            mediaPlayer.setDataSource(fis.fd)
        }

        //Prepare the player
        mediaPlayer.prepareAsync()

        //if mediaPlayer is finished preparing
        mediaPlayer.setOnPreparedListener {
            val primaryResolution = settings.getSetting("primaryCoverResolution")

            mediaPlayer.start()

            playing = true

            val title = song.title
            val artist = song.artist
            val coverUrl =
                contextReference!!.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution

            textView1Reference!!.get()!!.text = title
            textView2Reference!!.get()!!.text = title
            textView2Reference!!.get()!!.text = title

            val valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
            valueAnimator.repeatCount = Animation.INFINITE
            valueAnimator.interpolator = LinearInterpolator()
            valueAnimator.duration = 9000L
            valueAnimator.addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val width = textView1Reference!!.get()!!.width
                val translationX = width * progress
                textView1Reference!!.get()!!.translationX = translationX
                textView2Reference!!.get()!!.translationX = translationX - width
            }

            valueAnimator.start()

            mediaPlayer.setOnCompletionListener {
                next()
            }

            val seekbarUpdateHandler = Handler()

            val updateSeekBar = object : Runnable {
                override fun run() {
                    seekBarReference!!.get()!!.max = mediaPlayer.duration
                    seekBarReference!!.get()!!.progress = mediaPlayer.currentPosition
                    setPlayerState(mediaPlayer.currentPosition.toLong())

                    seekbarUpdateHandler.postDelayed(this, 50)
                }
            }

            seekbarUpdateHandler.postDelayed(updateSeekBar, 0)

            seekBarReference!!.get()!!.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser)
                        mediaPlayer.seekTo(progress)
                    setPlayerState(progress.toLong())
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

            val coverFile = File(coverUrl)
            if (coverFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(coverFile.absolutePath)
                barCoverImageReference!!.get()!!.setImageBitmap(bitmap)
                setSongMetadata(artist, title, bitmap, mediaPlayer.duration.toLong())
            } else {
                setSongMetadata(artist, title, null, mediaPlayer.duration.toLong())
            }

            showSongNotification(title, artist, coverUrl, true)
            playButtonReference!!.get()!!.setImageDrawable(
                ContextCompat.getDrawable(
                    contextReference!!.get()!!,
                    R.drawable.ic_pause_white_24dp
                )
            )
        }


    } catch (e: IndexOutOfBoundsException) {
        //TODO do something idk
    }
}

/**
 * SetPlayerState
 */
private fun setPlayerState(progress: Long) {
    val stateBuilder = PlaybackStateCompat.Builder()

    val state: Int = if (playing) {
        PlaybackState.STATE_PLAYING
    } else {
        PlaybackState.STATE_PAUSED
    }

    stateBuilder.setState(state, progress, 1.0f)
    stateBuilder.setActions(
        PlaybackStateCompat.ACTION_PLAY +
                PlaybackStateCompat.ACTION_PAUSE +
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT +
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS +
                PlaybackStateCompat.ACTION_STOP +
                PlaybackStateCompat.ACTION_PLAY_PAUSE +
                PlaybackStateCompat.ACTION_SEEK_TO +
                PlaybackStateCompat.ACTION_FAST_FORWARD +
                PlaybackStateCompat.ACTION_REWIND
    )
    mediaSession!!.setPlaybackState(stateBuilder.build())
}

/**
 * Set song metadata
 */
private fun setSongMetadata(artist: String, title: String, coverImage: Bitmap?, duration: Long) {
    val mediaMetadata = MediaMetadataCompat.Builder()
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
    mediaMetadata.putString(MediaMetadata.METADATA_KEY_TITLE, title)
    mediaMetadata.putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, coverImage)
    mediaMetadata.putBitmap(MediaMetadata.METADATA_KEY_ART, coverImage)
    mediaSession!!.setMetadata(mediaMetadata.build())
}

/**
 * Stop playback
 */
private fun stop() {
    val context = contextReference!!.get()!!
    playing = false
    textView1Reference!!.get()!!.text = ""
    textView2Reference!!.get()!!.text = ""
    playButtonReference!!.get()!!.setImageDrawable(
        ContextCompat.getDrawable(
            context,
            R.drawable.ic_play_arrow_white_24dp
        )
    )
    mediaPlayer.stop()
}

/**
 * Pause playback
 */
fun pause() {
    val context = contextReference!!.get()!!
    val settings = Settings(context)
    val primaryResolution = settings.getSetting("primaryCoverResolution")

    try {
        val song = playList[currentSong]

        val title = song.title
        val artist = song.artist
        val coverUrl = context.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution

        mediaPlayer.pause()
        showSongNotification(title, artist, coverUrl, false)
        playButtonReference!!.get()!!.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_play_arrow_white_24dp
            )
        )
        playing = false
        paused = true
    } catch (e: IndexOutOfBoundsException) {

    }
}

/**
 * Resume playback
 */
fun resume() {
    val context = contextReference!!.get()!!
    val settings = Settings(context)
    val primaryResolution = settings.getSetting("primaryCoverResolution")

    try {
        val song = playList[currentSong]

        val title = song.title
        val artist = song.artist
        val coverUrl = context.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution

        val length = mediaPlayer.currentPosition

        if (paused) {
            mediaPlayer.seekTo(length)
            mediaPlayer.start()

            paused = false

            val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(NoisyReceiver(), intentFilter)
        } else {
            play()
        }

        showSongNotification(title, artist, coverUrl, true)
        playButtonReference!!.get()!!.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_pause_white_24dp
            )
        )
        playing = true
    } catch (e: IndexOutOfBoundsException) {

    }
}

/**
 * Next song
 */
fun next() {
    currentSong++
    play()

}

/**
 * Previous song
 */
fun previous() {
    if (currentSong != 0) {
        currentSong--

        play()
    }
}

/**
 * Play song now
 */
fun playNow(song: Song) {
    try {
        playList.add(currentSong + 1, song)
        currentSong++
    } catch (e: IndexOutOfBoundsException) {
        playList.add(song)
    }


    play()
}

/**
 * Play song after
 */
fun addSong(song: Song) {
    playList.add(song)
}

/**
 * Toggle pause/play
 */
fun toggleMusic() {
    if (playing) {
        pause()
    } else {
        resume()
    }
}

/**
 * Show notification
 */
private fun showSongNotification(
    titleName: String,
    artistName: String,
    coverUrl: String,
    playing: Boolean
) {
    val context = contextReference!!.get()!!

    createNotificationChannel()

    val backgroundColor: Int
    val expandedRemoteViews: RemoteViews

    val coverFile = File(coverUrl)
    if (coverFile.exists()) {
        val bitmap = BitmapFactory.decodeFile(coverFile.absolutePath)
        backgroundColor = getDominantColor(bitmap)

        if (getTextColor(backgroundColor) == Color.WHITE) {
            expandedRemoteViews = RemoteViews(
                context.packageName,
                R.layout.notification_expanded_white
            )
            expandedRemoteViews.setTextColor(R.id.songname, Color.WHITE)
            expandedRemoteViews.setTextColor(R.id.artistname, Color.WHITE)
        } else {
            expandedRemoteViews = RemoteViews(
                context.packageName,
                R.layout.notification_expanded
            )
            expandedRemoteViews.setTextColor(R.id.songname, Color.BLACK)
            expandedRemoteViews.setTextColor(R.id.artistname, Color.BLACK)
        }

        expandedRemoteViews.setImageViewBitmap(R.id.coverimageview, bitmap)

        expandedRemoteViews.setInt(
            R.id.notificationlayout,
            "setBackgroundColor",
            backgroundColor
        )
    } else {
        expandedRemoteViews = RemoteViews(
            context.packageName,
            R.layout.notification_expanded
        )
    }

    expandedRemoteViews.setTextViewText(R.id.artistname, artistName)
    expandedRemoteViews.setTextViewText(R.id.songname, titleName)

    if (playing) {
        expandedRemoteViews.setViewVisibility(R.id.playButton, View.GONE)
        expandedRemoteViews.setViewVisibility(R.id.pauseButton, View.VISIBLE)
    } else {
        expandedRemoteViews.setViewVisibility(R.id.playButton, View.VISIBLE)
        expandedRemoteViews.setViewVisibility(R.id.pauseButton, View.GONE)
    }

    val notificationBuilder = NotificationCompat.Builder(context, channelID)
    notificationBuilder.setSmallIcon(R.drawable.ic_play_circle_filled_black_24dp)
    notificationBuilder.priority = NotificationCompat.PRIORITY_LOW
    notificationBuilder.setOngoing(true)

    notificationBuilder.setStyle(
        androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(
            mediaSession!!.sessionToken
        )
    )
    notificationBuilder.setCustomBigContentView(expandedRemoteViews)

    setListeners(expandedRemoteViews, context)

    val notificationManagerCompat = NotificationManagerCompat.from(context)
    notificationManagerCompat.notify(notificationID, notificationBuilder.build())

    context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PREVIOUS))
    context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_DELETE))
    context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PAUSE))
    context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PLAY))
    context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_NEXT))
}

private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val context = contextReference!!.get()!!

        val channelName = "Music Notification"
        val channelDescription = "Handy dandy description"
        val importance = NotificationManager.IMPORTANCE_LOW

        val notificationChannel = NotificationChannel(channelID, channelName, importance)

        notificationChannel.description = channelDescription

        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

    }
}

/**
 * Notification button listeners
 */
private fun setListeners(view: RemoteViews, context: Context) {
    val previous = Intent(NOTIFICATION_PREVIOUS)
    val delete = Intent(NOTIFICATION_DELETE)
    val pause = Intent(NOTIFICATION_PAUSE)
    val play = Intent(NOTIFICATION_PLAY)
    val next = Intent(NOTIFICATION_NEXT)

    val pendingPrevious =
        PendingIntent.getBroadcast(context, 0, previous, PendingIntent.FLAG_CANCEL_CURRENT)
    view.setOnClickPendingIntent(R.id.prevButton, pendingPrevious)

    val pendingDelete =
        PendingIntent.getBroadcast(context, 0, delete, PendingIntent.FLAG_CANCEL_CURRENT)
    view.setOnClickPendingIntent(R.id.closeButton, pendingDelete)

    val pendingPause =
        PendingIntent.getBroadcast(context, 0, pause, PendingIntent.FLAG_CANCEL_CURRENT)
    view.setOnClickPendingIntent(R.id.pauseButton, pendingPause)

    val pendingPlay =
        PendingIntent.getBroadcast(context, 0, play, PendingIntent.FLAG_CANCEL_CURRENT)
    view.setOnClickPendingIntent(R.id.playButton, pendingPlay)

    val pendingNext =
        PendingIntent.getBroadcast(context, 0, next, PendingIntent.FLAG_CANCEL_CURRENT)
    view.setOnClickPendingIntent(R.id.nextButton, pendingNext)
}

class IntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when {
            intent!!.action.equals(NOTIFICATION_PREVIOUS) -> previous()
            intent.action.equals(NOTIFICATION_DELETE) -> {
                val notificationManager =
                    context!!.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                pause()
                notificationManager.cancel(notificationID)
            }
            intent.action.equals(NOTIFICATION_PAUSE) -> pause()
            intent.action.equals(NOTIFICATION_PLAY) -> resume()
            intent.action.equals(NOTIFICATION_NEXT) -> next()
        }
    }
}

/**
 * Get the dominant color of a bitmap image (use for notification background color)
 */
private fun getDominantColor(bitmap: Bitmap): Int {
    val swatchesTemp = Palette.from(bitmap).generate().swatches
    val swatches = ArrayList<Palette.Swatch>(swatchesTemp)

    swatches.sortWith(Comparator { swatch1, swatch2 -> swatch2.population - swatch1.population })

    return if (swatches.size > 0) swatches[0].rgb else Color.WHITE
}

/**
 * Get either BLACK or WHITE, depending on the background color for best readability
 */
private fun getTextColor(background: Int): Int {
    val backgroundRed = Color.red(background)
    val backgroundGreen = Color.green(background)
    val backgroundBlue = Color.blue(background)

    val luma =
        ((0.299 * backgroundRed) + (0.587 * backgroundGreen) + (0.114 * backgroundBlue)) / 255

    return if (luma > 0.5) Color.BLACK else Color.WHITE
}


