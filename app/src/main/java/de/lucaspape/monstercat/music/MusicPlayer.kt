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
import de.lucaspape.monstercat.activities.loadContinuousSongListAsyncTask
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.settings.Settings
import java.io.File
import java.io.FileInputStream
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import kotlin.collections.ArrayList

var contextReference: WeakReference<Context>? = null

var textView1Reference: WeakReference<TextView>? = null
var textView2Reference: WeakReference<TextView>? = null
var seekBarReference: WeakReference<SeekBar>? = null
var barCoverImageReference: WeakReference<ImageView>? = null
var musicBarReference: WeakReference<androidx.appcompat.widget.Toolbar>? = null
var playButtonReference: WeakReference<ImageButton>? = null

var fullscreenTextView1Reference: WeakReference<TextView>? = null
var fullscreenTextView2Reference: WeakReference<TextView>? = null
var fullscreenSeekBarReference: WeakReference<SeekBar>? = null
var fullscreenCoverReference: WeakReference<ImageView>? = null
var fullscreenPlayButtonReference: WeakReference<ImageButton>? = null

private var mediaPlayer = MediaPlayer()
private var currentSong = 0
private var currentContinuousPoint = 0
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

fun setFullscreenTextView(newTextView1: TextView, newTextView2: TextView) {
    try {
        newTextView1.text = textView1Reference!!.get()!!.text
        newTextView2.text = textView2Reference!!.get()!!.text
    } catch (e: NullPointerException) {

    }

    fullscreenTextView1Reference = WeakReference(newTextView1)
    fullscreenTextView2Reference = WeakReference(newTextView2)
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

fun setFullscreenSeekBar(newSeekBar: SeekBar) {
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

    fullscreenSeekBarReference = WeakReference(newSeekBar)
}

fun setBarCoverImageView(newImageView: ImageView) {
    try {
        newImageView.setImageDrawable(barCoverImageReference!!.get()!!.drawable)
    } catch (e: NullPointerException) {

    }

    barCoverImageReference = WeakReference(newImageView)
}

fun setFullscreenCoverImageView(newImageView: ImageView) {
    try {
        newImageView.setImageDrawable(barCoverImageReference!!.get()!!.drawable)
    } catch (e: NullPointerException) {

    }

    fullscreenCoverReference = WeakReference(newImageView)
}

fun setMusicBar(newToolbar: androidx.appcompat.widget.Toolbar) {
    try {
        newToolbar.setBackgroundColor((musicBarReference!!.get()!!.background as ColorDrawable).color)
    } catch (e: NullPointerException) {

    }
    musicBarReference = WeakReference(newToolbar)
}

fun setPlayButton(newPlayButton: ImageButton) {
    if (playing) {
        newPlayButton.setImageDrawable(
            ContextCompat.getDrawable(
                contextReference!!.get()!!,
                R.drawable.ic_pause_white_24dp
            )
        )

    } else {
        newPlayButton.setImageDrawable(
            ContextCompat.getDrawable(
                contextReference!!.get()!!,
                R.drawable.ic_play_arrow_white_24dp
            )
        )

    }

    playButtonReference = WeakReference(newPlayButton)
}

fun setFullscreenPlayButton(newPlayButton: ImageButton) {
    if (playing) {
        newPlayButton.setImageDrawable(
            ContextCompat.getDrawable(
                contextReference!!.get()!!,
                R.drawable.ic_pause_black_24dp
            )
        )

    } else {
        newPlayButton.setImageDrawable(
            ContextCompat.getDrawable(
                contextReference!!.get()!!,
                R.drawable.ic_play_arrow_black_24dp
            )
        )

    }

    fullscreenPlayButtonReference = WeakReference(newPlayButton)
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
            } else {
                mediaPlayer.setDataSource(url)
            }
        } else {
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

            try {
                textView1Reference!!.get()!!.text = title
            } catch (e: NullPointerException) {

            }
            try {
                textView2Reference!!.get()!!.text = title
            } catch (e: NullPointerException) {

            }

            try {
                fullscreenTextView1Reference!!.get()!!.text = title
            } catch (e: NullPointerException) {

            }

            try {
                fullscreenTextView2Reference!!.get()!!.text = title
            } catch (e: NullPointerException) {

            }

            val valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
            valueAnimator.repeatCount = Animation.INFINITE
            valueAnimator.interpolator = LinearInterpolator()
            valueAnimator.duration = 9000L
            valueAnimator.addUpdateListener { animation ->
                val progress = animation.animatedValue as Float

                var width = 0


                try {
                    width = textView1Reference!!.get()!!.width
                } catch (e: NullPointerException) {

                }


                try {
                    width = fullscreenTextView1Reference!!.get()!!.width
                } catch (e: NullPointerException) {

                }


                val translationX = width * progress

                try {
                    textView1Reference!!.get()!!.translationX = translationX
                } catch (e: NullPointerException) {

                }

                try {
                    textView2Reference!!.get()!!.translationX = translationX - width
                } catch (e: NullPointerException) {
                }

                try {
                    fullscreenTextView1Reference!!.get()!!.translationX = translationX
                } catch (e: NullPointerException) {

                }

                try {
                    fullscreenTextView2Reference!!.get()!!.translationX = translationX - width
                } catch (e: NullPointerException) {

                }
            }

            valueAnimator.start()

            mediaPlayer.setOnCompletionListener {
                next()
            }

            val seekbarUpdateHandler = Handler()

            val updateSeekBar = object : Runnable {
                override fun run() {
                    try {
                        seekBarReference!!.get()!!.max = mediaPlayer.duration
                        seekBarReference!!.get()!!.progress = mediaPlayer.currentPosition
                    } catch (e: NullPointerException) {

                    }

                    try {
                        fullscreenSeekBarReference!!.get()!!.max = mediaPlayer.duration
                        fullscreenSeekBarReference!!.get()!!.progress = mediaPlayer.currentPosition
                    } catch (e: NullPointerException) {

                    }
                    setPlayerState(mediaPlayer.currentPosition.toLong())

                    seekbarUpdateHandler.postDelayed(this, 50)
                }
            }

            seekbarUpdateHandler.postDelayed(updateSeekBar, 0)

            try {
                seekBarReference!!.get()!!.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser)
                            mediaPlayer.seekTo(progress)
                        setPlayerState(progress.toLong())
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                    }
                })
            } catch (e: NullPointerException) {

            }

            try {
                fullscreenSeekBarReference!!.get()!!.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser)
                            mediaPlayer.seekTo(progress)
                        setPlayerState(progress.toLong())
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                    }
                })
            } catch (e: NullPointerException) {

            }


            val coverFile = File(coverUrl)
            if (coverFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(coverFile.absolutePath)

                try {
                    barCoverImageReference!!.get()!!.setImageBitmap(bitmap)
                } catch (e: NullPointerException) {

                }

                try {
                    fullscreenCoverReference!!.get()!!.setImageBitmap(bitmap)
                } catch (e: NullPointerException) {

                }

                setSongMetadata(artist, title, bitmap, mediaPlayer.duration.toLong())
            } else {
                setSongMetadata(artist, title, null, mediaPlayer.duration.toLong())
            }

            showSongNotification(title, artist, coverUrl, true)

            try {

                playButtonReference!!.get()!!.setImageDrawable(
                    ContextCompat.getDrawable(
                        contextReference!!.get()!!,
                        R.drawable.ic_pause_white_24dp
                    )
                )

            } catch (e: NullPointerException) {

            }


            try {

                fullscreenPlayButtonReference!!.get()!!.setImageDrawable(
                    ContextCompat.getDrawable(
                        contextReference!!.get()!!,
                        R.drawable.ic_pause_black_24dp
                    )
                )

            } catch (e: NullPointerException) {

            }


        }
    } catch (e: IndexOutOfBoundsException) {

    }
}

/**
 * Stop playback
 */
private fun stop() {
    val context = contextReference!!.get()!!
    playing = false

    try {
        textView1Reference!!.get()!!.text = ""
    } catch (e: NullPointerException) {

    }

    try {
        textView2Reference!!.get()!!.text = ""
    } catch (e: NullPointerException) {

    }

    try {
        fullscreenTextView1Reference!!.get()!!.text = ""
    } catch (e: NullPointerException) {

    }

    try {
        fullscreenTextView2Reference!!.get()!!.text = ""
    } catch (e: NullPointerException) {

    }

    try {
        playButtonReference!!.get()!!.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_play_arrow_black_24dp
            )
        )
    } catch (e: NullPointerException) {

    }

    try {
        fullscreenPlayButtonReference!!.get()!!.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_play_arrow_black_24dp
            )
        )
    } catch (e: NullPointerException) {

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


        try {
            playButtonReference!!.get()!!.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_play_arrow_white_24dp
                )
            )
        } catch (e: NullPointerException) {

        }

        try {
            fullscreenPlayButtonReference!!.get()!!.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_play_arrow_black_24dp
                )
            )
        } catch (e: NullPointerException) {

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

        try {
            playButtonReference!!.get()!!.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_pause_white_24dp
                )
            )
        } catch (e: NullPointerException) {

        }

        try {
            fullscreenPlayButtonReference!!.get()!!.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_pause_black_24dp
                )
            )
        } catch (e: NullPointerException) {

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
    playList.add(currentContinuousPoint, song)
    currentContinuousPoint++
}

fun clearContinuous() {
    try {
        loadContinuousSongListAsyncTask!!.cancel(true)
    } catch (e: NullPointerException) {

    }

    playList = ArrayList(playList.subList(0, currentContinuousPoint))
    currentSong = playList.size
    currentContinuousPoint = 0
}

fun addContinuous(song: Song) {
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
