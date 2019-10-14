package de.lucaspape.monstercat.music

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.content.ContextCompat
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.settings.Settings
import java.io.File
import java.io.FileInputStream
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import kotlin.collections.ArrayList

var contextReference: WeakReference<Context>? = null
var blackbuttons = false

var textView1Reference: WeakReference<TextView>? = null
var textView2Reference: WeakReference<TextView>? = null
var seekBarReference: WeakReference<SeekBar>? = null
var barCoverImageReference: WeakReference<ImageView>? = null
var musicBarReference: WeakReference<androidx.appcompat.widget.Toolbar>? = null
var playButtonReference: WeakReference<ImageButton>? = null

private var mediaPlayer = MediaPlayer()
private var currentSong = 0
private var playList = ArrayList<Song>(1)
private var playing = false
private var paused = false

var mediaSession: MediaSessionCompat? = null

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
    println("SETTTTITNGA")
    if(playing){
        if(blackbuttons){
            newPlayButton.setImageDrawable(
                ContextCompat.getDrawable(
                    contextReference!!.get()!!,
                    R.drawable.ic_pause_black_24dp
                )
            )
        }else{
            newPlayButton.setImageDrawable(
                ContextCompat.getDrawable(
                    contextReference!!.get()!!,
                    R.drawable.ic_pause_white_24dp
                )
            )
        }
    }else{
        if(blackbuttons){
            newPlayButton.setImageDrawable(
                ContextCompat.getDrawable(
                    contextReference!!.get()!!,
                    R.drawable.ic_play_arrow_black_24dp
                )
            )
        }else{
            newPlayButton.setImageDrawable(
                ContextCompat.getDrawable(
                    contextReference!!.get()!!,
                    R.drawable.ic_play_arrow_white_24dp
                )
            )
        }
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
            if(blackbuttons){
                playButtonReference!!.get()!!.setImageDrawable(
                    ContextCompat.getDrawable(
                        contextReference!!.get()!!,
                        R.drawable.ic_pause_black_24dp
                    )
                )
            }else{
                playButtonReference!!.get()!!.setImageDrawable(
                    ContextCompat.getDrawable(
                        contextReference!!.get()!!,
                        R.drawable.ic_pause_white_24dp
                    )
                )
            }

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

    if(blackbuttons){
        playButtonReference!!.get()!!.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_play_arrow_black_24dp
            )
        )
    }else{
        playButtonReference!!.get()!!.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_play_arrow_white_24dp
            )
        )
    }

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

        if (blackbuttons){
            playButtonReference!!.get()!!.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_play_arrow_black_24dp
                )
            )
        }else{
            playButtonReference!!.get()!!.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_play_arrow_white_24dp
                )
            )
        }

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

        if(blackbuttons){
            playButtonReference!!.get()!!.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_pause_black_24dp
                )
            )
        }else{
            playButtonReference!!.get()!!.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_pause_white_24dp
                )
            )
        }

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

