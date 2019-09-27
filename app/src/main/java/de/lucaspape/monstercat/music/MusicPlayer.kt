package de.lucaspape.monstercat.music

import android.animation.ValueAnimator
import android.app.Notification
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
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.os.Build
import android.os.Handler
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.palette.graphics.Palette
import de.lucaspape.monstercat.R
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import kotlin.Comparator
import kotlin.collections.ArrayList

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
private const val channelID = "Music Notification"
private const val notificationID = 1

const val NOTIFICATION_PREVIOUS = "de.lucaspape.monstercat.previous"
const val NOTIFICATION_DELETE = "de.lucaspape.monstercat.delete"
const val NOTIFICATION_PAUSE = "de.lucaspape.monstercat.pause"
const val NOTIFICATION_PLAY = "de.lucaspape.monstercat.play"
const val NOTIFICATION_NEXT = "de.lucaspape.monstercat.next"

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

private fun play() {
    val song = playList[currentSong]

    val url = song.getUrl()
    val title = song.title
    val artist = song.artist
    val coverUrl = song.coverLocation

    mediaPlayer.stop()
    mediaPlayer = MediaPlayer()

    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
    mediaPlayer.setDataSource(url)
    mediaPlayer.prepare()
    mediaPlayer.start()

    playing = true

    textView1Reference!!.get()!!.text = title
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
            seekbarUpdateHandler.postDelayed(this, 50)
        }
    }

    seekbarUpdateHandler.postDelayed(updateSeekBar, 0)

    seekBarReference!!.get()!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser)
                mediaPlayer.seekTo(progress)
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
    }

    showNotification(title, artist, coverUrl, true)
    playButtonReference!!.get()!!.setImageDrawable(
        contextReference!!.get()!!.resources.getDrawable(
            R.drawable.ic_pause_white_24dp
        )
    )

}

private fun stop() {
    val context = contextReference!!.get()!!
    playing = false
    textView1Reference!!.get()!!.text = ""
    textView2Reference!!.get()!!.text = ""
    playButtonReference!!.get()!!.setImageDrawable(context.resources.getDrawable(R.drawable.ic_play_arrow_white_24dp))
    mediaPlayer.stop()
}

fun pause() {
    val context = contextReference!!.get()!!

    try {
        val song = playList[currentSong]

        val title = song.title
        val artist = song.artist
        val coverUrl = song.coverLocation

        mediaPlayer.pause()
        showNotification(title, artist, coverUrl, false)
        playButtonReference!!.get()!!.setImageDrawable(context.resources.getDrawable(R.drawable.ic_play_arrow_white_24dp))
        playing = false
        paused = true
    } catch (e: IndexOutOfBoundsException) {

    }
}

fun resume() {
    val context = contextReference!!.get()!!

    try {
        val song = playList[currentSong]

        val title = song.title
        val artist = song.artist
        val coverUrl = song.coverLocation

        val length = mediaPlayer.currentPosition

        if (paused) {
            mediaPlayer.seekTo(length)
            mediaPlayer.start()

            paused = false
        } else {
            play()
        }

        showNotification(title, artist, coverUrl, true)
        playButtonReference!!.get()!!.setImageDrawable(context.resources.getDrawable(R.drawable.ic_pause_white_24dp))
        playing = true
    } catch (e: IndexOutOfBoundsException) {

    }
}

fun next() {
    currentSong++
    play()

}

fun previous() {
    if (currentSong != 0) {
        currentSong--

        play()
    }
}

fun playNow(song:Song) {
    try {
        playList.add(currentSong + 1, song)
        currentSong++
    } catch (e: IndexOutOfBoundsException) {
        playList.add(song)
    }


    play()
}

fun addSong(song:Song) {
    playList.add(song)
}

fun toggleMusic() {
    if (playing) {
        pause()
    } else {
        resume()
    }
}

private fun showNotificationAndroidO(
    titleName: String,
    artistName: String,
    coverUrl: String,
    playing: Boolean
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val context = contextReference!!.get()!!

        createNotificationChannel()

        val backgroundColor: Int
        val expandedRemoteViews: RemoteViews
        //val normalRemoteViews = RemoteViews(context.packageName, R.layout.notification_normal)

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

        val notificationBuilder = Notification.Builder(context, channelID)
        notificationBuilder.setSmallIcon(R.drawable.ic_play_circle_filled_black_24dp)
        notificationBuilder.setPriority(Notification.PRIORITY_LOW)
        notificationBuilder.setOngoing(true)

        //notificationBuilder.setColorized(true)
        //notificationBuilder.color = backgroundColor

        notificationBuilder.style = Notification.DecoratedCustomViewStyle()
        //notificationBuilder.setCustomContentView(normalRemoteViews)
        notificationBuilder.setCustomBigContentView(expandedRemoteViews)

        val mediaSession = MediaSession(context, "de.lucaspape.monstercat.music")

        notificationBuilder.style = Notification.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)

        setListeners(expandedRemoteViews, context)

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        notificationManagerCompat.notify(notificationID, notificationBuilder.build())

        context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PREVIOUS))
        context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_DELETE))
        context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PAUSE))
        context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PLAY))
        context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_NEXT))
    }
}

//android < sdk 26
private fun showNotification(
    titleName: String,
    artistName: String,
    coverUrl: String,
    playing: Boolean
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        val context = contextReference!!.get()!!

        createNotificationChannel()

        val backgroundColor: Int
        val normalRemoteViews = RemoteViews(
            context.packageName,
            R.layout.notification_normal
        )
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

        expandedRemoteViews.setTextViewText(R.id.songname, titleName)
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

        //notificationBuilder.setColorized(true)
        //notificationBuilder.color = backgroundColor

        notificationBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        notificationBuilder.setCustomContentView(normalRemoteViews)
        notificationBuilder.setCustomBigContentView(expandedRemoteViews)

        setListeners(expandedRemoteViews, context)

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        notificationManagerCompat.notify(notificationID, notificationBuilder.build())

        context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PREVIOUS))
        context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_DELETE))
        context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PAUSE))
        context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PLAY))
        context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_NEXT))
    } else {
        showNotificationAndroidO(titleName, artistName, coverUrl, playing)
    }
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

private fun getDominantColor(bitmap: Bitmap): Int {
    val swatchesTemp = Palette.from(bitmap).generate().swatches
    val swatches = ArrayList<Palette.Swatch>(swatchesTemp)

    swatches.sortWith(Comparator { swatch1, swatch2 -> swatch2.population - swatch1.population })

    return if (swatches.size > 0) swatches[0].rgb else Color.WHITE
}

private fun getTextColor(background: Int): Int {
    val backgroundRed = Color.red(background)
    val backgroundGreen = Color.green(background)
    val backgroundBlue = Color.blue(background)

    val luma =
        ((0.299 * backgroundRed) + (0.587 * backgroundGreen) + (0.114 * backgroundBlue)) / 255

    return if (luma > 0.5) Color.BLACK else Color.WHITE
}

