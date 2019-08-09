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
import de.lucaspape.monstercat.MainActivity
import de.lucaspape.monstercat.R
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference
import kotlin.Comparator
import kotlin.collections.ArrayList

/**
 * This class is meant to be static
 */
class MusicPlayer(
    private var weakReference: WeakReference<Context>,
    private var textView1: TextView,
    private var textView2: TextView,
    private var seekBar: SeekBar,
    private var barCoverImage: ImageView,
    private var musicBar: androidx.appcompat.widget.Toolbar,
    private var playButton: ImageButton
) {

    private var mediaPlayer = MediaPlayer()
    private var currentSong = 0
    private var playList = ArrayList<HashMap<String, Any?>>(1)
    private var playing = false
    private var paused = false

    //notification var
    private val channelID = "Music Notification"
    private val notificationID = 1

    companion object {
        @JvmStatic
        val NOTIFICATION_PREVIOUS = "de.lucaspape.monstercat.previous"

        @JvmStatic
        val NOTIFICATION_DELETE = "de.lucaspape.monstercat.delete"

        @JvmStatic
        val NOTIFICATION_PAUSE = "de.lucaspape.monstercat.pause"

        @JvmStatic
        val NOTIFICATION_PLAY = "de.lucaspape.monstercat.play"

        @JvmStatic
        val NOTIFICATION_NEXT = "de.lucaspape.monstercat.next"
    }

    fun setReference(reference: WeakReference<Context>) {
        this.weakReference = reference
    }

    fun setTextView(textView1: TextView, textView2: TextView) {
        textView1.text = this.textView1.text
        textView2.text = this.textView2.text
        this.textView1 = textView1
        this.textView2 = textView2
    }

    fun setSeekBar(seekBar: SeekBar) {
        seekBar.progress = this.seekBar.progress
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    mediaPlayer.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        this.seekBar = seekBar
    }

    fun setBarCoverImageView(imageView: ImageView) {
        imageView.setImageDrawable(barCoverImage.drawable)
        this.barCoverImage = imageView
    }

    fun setMusicBar(toolbar: androidx.appcompat.widget.Toolbar) {
        toolbar.setBackgroundColor((musicBar.background as ColorDrawable).color)
        musicBar = toolbar
    }

    fun setPlayButton(button: ImageButton) {
        button.setImageDrawable(playButton.drawable)
        playButton = button
    }

    private fun play() {
        val context = weakReference.get()!!
        val song: HashMap<String, Any?>

        try {
            if (playList[currentSong].isNotEmpty()) {
                song = playList[currentSong]
            } else {
                return
            }
        } catch (e: IndexOutOfBoundsException) {
            return
        }


        val url = song["url"] as String
        val title = song["title"] as String
        val artist = song["artist"] as String
        val coverUrl = song["coverUrl"] as String

        mediaPlayer.stop()
        mediaPlayer = MediaPlayer()

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mediaPlayer.setDataSource(url)
        mediaPlayer.prepare()
        mediaPlayer.start()

        playing = true

        try {
            textView1.text = title
            textView2.text = title

            val valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
            valueAnimator.repeatCount = Animation.INFINITE
            valueAnimator.interpolator = LinearInterpolator()
            valueAnimator.duration = 9000L
            valueAnimator.addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val width = textView1.width
                val translationX = width * progress
                textView1.translationX = translationX
                textView2.translationX = translationX - width
            }

            valueAnimator.start()

            mediaPlayer.setOnCompletionListener {
                next()
            }

            val seekbarUpdateHandler = Handler()

            val updateSeekBar = object : Runnable {
                override fun run() {
                    seekBar.max = mediaPlayer.duration
                    seekBar.progress = mediaPlayer.currentPosition
                    seekbarUpdateHandler.postDelayed(this, 50)
                }
            }

            seekbarUpdateHandler.postDelayed(updateSeekBar, 0)

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
                barCoverImage.setImageBitmap(bitmap)
            }

            showNotification(title, artist, coverUrl, true)
            playButton.setImageDrawable(context.resources.getDrawable(R.drawable.ic_pause_white_24dp))
        } catch (e: IndexOutOfBoundsException) {
            //Something bad happend, resetting
            MainActivity.musicPlayer = MusicPlayer(
                weakReference,
                textView1,
                textView2,
                seekBar,
                barCoverImage,
                musicBar,
                playButton
            )
        }
    }

    private fun stop() {
        val context = weakReference.get()!!
        playing = false
        textView1.text = ""
        textView2.text = ""
        playButton.setImageDrawable(context.resources.getDrawable(R.drawable.ic_play_arrow_white_24dp))
        mediaPlayer.stop()
    }

    fun pause() {
        val context = weakReference.get()!!

        try {
            val song = playList[currentSong]

            val title = song["title"] as String
            val artist = song["artist"] as String
            val coverUrl = song["coverUrl"] as String

            mediaPlayer.pause()
            showNotification(title, artist, coverUrl, false)
            playButton.setImageDrawable(context.resources.getDrawable(R.drawable.ic_play_arrow_white_24dp))
            playing = false
            paused = true
        } catch (e: IndexOutOfBoundsException) {

        }
    }

    fun resume() {
        val context = weakReference.get()!!

        try {
            val song = playList[currentSong]

            val title = song["title"] as String
            val artist = song["artist"] as String
            val coverUrl = song["coverUrl"] as String

            val length = mediaPlayer.currentPosition

            if (paused) {
                mediaPlayer.seekTo(length)
                mediaPlayer.start()

                paused = false
            } else {
                play()
            }

            showNotification(title, artist, coverUrl, true)
            playButton.setImageDrawable(context.resources.getDrawable(R.drawable.ic_pause_white_24dp))
            playing = true
        } catch (e: IndexOutOfBoundsException) {

        }
    }

    fun next() {
        try {
            if (playList[currentSong + 1].isNotEmpty()) {
                currentSong++
                play()
            } else {
                stop()
            }
        } catch (e: IndexOutOfBoundsException) {
            stop()
        }

    }

    fun previous() {
        if (currentSong != 0) {
            currentSong--

            try {
                if (playList[currentSong].isEmpty()) {
                    stop()
                } else {
                    play()
                }
            } catch (e: IndexOutOfBoundsException) {
                stop()
            }
        }
    }

    fun playNow(url: String, title: String, artistName: String, coverUrl: String) {
        val song = HashMap<String, Any?>()
        song["url"] = url
        song["title"] = title
        song["artist"] = artistName
        song["coverUrl"] = coverUrl
        song["playNow"] = true

        try {
            playList.add(currentSong + 1, song)
            currentSong++
        } catch (e: IndexOutOfBoundsException) {
            playList.add(song)
        }


        play()
    }

    fun addSong(url: String, title: String, artistName: String, coverUrl: String) {
        val song = HashMap<String, Any?>()
        song["url"] = url
        song["title"] = title
        song["artist"] = artistName
        song["coverUrl"] = coverUrl

        playList.add(song)
    }

    fun toggleMusic() {
        if (playing) {
            pause()
        } else {
            resume()
        }
    }

    private fun showNotificationAndroidO(titleName: String, artistName: String, coverUrl: String, playing: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = weakReference.get()!!

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

                expandedRemoteViews.setInt(R.id.notificationlayout, "setBackgroundColor", backgroundColor)
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
    private fun showNotification(titleName: String, artistName: String, coverUrl: String, playing: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val context = weakReference.get()!!

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

                expandedRemoteViews.setInt(R.id.notificationlayout, "setBackgroundColor", backgroundColor)
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
            val context = weakReference.get()!!

            val channelName = "Music Notification"
            val channelDescription = "Handy dandy description"
            val importance = NotificationManager.IMPORTANCE_LOW

            val notificationChannel = NotificationChannel(channelID, channelName, importance)

            notificationChannel.description = channelDescription

            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)

        }
    }

    private fun setListeners(view: RemoteViews, context: Context) {
        val previous = Intent(NOTIFICATION_PREVIOUS)
        val delete = Intent(NOTIFICATION_DELETE)
        val pause = Intent(NOTIFICATION_PAUSE)
        val play = Intent(NOTIFICATION_PLAY)
        val next = Intent(NOTIFICATION_NEXT)

        val pendingPrevious = PendingIntent.getBroadcast(context, 0, previous, PendingIntent.FLAG_CANCEL_CURRENT)
        view.setOnClickPendingIntent(R.id.prevButton, pendingPrevious)

        val pendingDelete = PendingIntent.getBroadcast(context, 0, delete, PendingIntent.FLAG_CANCEL_CURRENT)
        view.setOnClickPendingIntent(R.id.closeButton, pendingDelete)

        val pendingPause = PendingIntent.getBroadcast(context, 0, pause, PendingIntent.FLAG_CANCEL_CURRENT)
        view.setOnClickPendingIntent(R.id.pauseButton, pendingPause)

        val pendingPlay = PendingIntent.getBroadcast(context, 0, play, PendingIntent.FLAG_CANCEL_CURRENT)
        view.setOnClickPendingIntent(R.id.playButton, pendingPlay)

        val pendingNext = PendingIntent.getBroadcast(context, 0, next, PendingIntent.FLAG_CANCEL_CURRENT)
        view.setOnClickPendingIntent(R.id.nextButton, pendingNext)
    }

    class IntentReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when {
                intent!!.action.equals(NOTIFICATION_PREVIOUS) -> MainActivity.musicPlayer!!.previous()
                intent.action.equals(NOTIFICATION_DELETE) -> {
                    val notificationManager = context!!.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(MainActivity.musicPlayer!!.notificationID)
                    MainActivity.musicPlayer!!.pause()
                }
                intent.action.equals(NOTIFICATION_PAUSE) -> MainActivity.musicPlayer!!.pause()
                intent.action.equals(NOTIFICATION_PLAY) -> MainActivity.musicPlayer!!.resume()
                intent.action.equals(NOTIFICATION_NEXT) -> MainActivity.musicPlayer!!.next()
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

        val luma = ((0.299 * backgroundRed) + (0.587 * backgroundGreen) + (0.114 * backgroundBlue)) / 255

        return if (luma > 0.5) Color.BLACK else Color.WHITE
    }


}