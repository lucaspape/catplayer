package de.lucaspape.monstercat

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
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

class MusicPlayer(private var context: Context, private var textView1: TextView, private var textView2:TextView ,private var seekBar: SeekBar, private var barCoverImage:ImageView, private var musicBar:androidx.appcompat.widget.Toolbar, private var playButton:ImageButton) {

    private var mediaPlayer = MediaPlayer()
    private var currentSong = 0
    private var playList = ArrayList<String>()
    private var titleList = ArrayList<String>()
    private var coverList = ArrayList<String>()
    private var artistList = ArrayList<String>()
    private var playing = false

    //notification var
    private val CHANNEL_ID = "Music Notification"
    private val NOTIFICATION_ID = 1

    companion object{
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

    fun setContext(context:Context){
        this.context = context
    }

    fun setTextView(textView1:TextView, textView2: TextView){
        textView1.text = this.textView1.text
        textView2.text = this.textView2.text
        this.textView1 = textView1
        this.textView2 = textView2
    }

    fun setSeekBar(seekBar: SeekBar){
        seekBar.progress = this.seekBar.progress
        seekBar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar:SeekBar, progress:Int, fromUser:Boolean) {
                if (fromUser)
                    mediaPlayer.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar:SeekBar) {
            }
            override fun onStopTrackingTouch(seekBar:SeekBar) {
            }
        })

        this.seekBar = seekBar
    }

    fun setBarCoverImageView(imageView: ImageView){
        imageView.setImageDrawable(barCoverImage.drawable)
        this.barCoverImage = imageView
    }

    fun setMusicBar(toolbar:androidx.appcompat.widget.Toolbar){
        toolbar.setBackgroundColor((musicBar.background as ColorDrawable).color)
        musicBar = toolbar
    }

    fun setPlayButton(button:ImageButton){
        button.setImageDrawable(playButton.drawable)
        playButton = button
    }

    fun play(){
        mediaPlayer.stop()
        mediaPlayer = MediaPlayer()

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mediaPlayer.setDataSource(playList[currentSong])
        mediaPlayer.prepare()
        mediaPlayer.start()

        playing = true

        try {
            textView1.text = titleList[currentSong] + "     "
            textView2.text = titleList[currentSong] + "     "

            val valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
            valueAnimator.repeatCount = Animation.INFINITE
            valueAnimator.interpolator = LinearInterpolator()
            valueAnimator.setDuration(9000L)
            valueAnimator.addUpdateListener { animation->
                val progress = animation.getAnimatedValue() as Float
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
                    seekBar.setProgress(mediaPlayer.getCurrentPosition())
                    seekbarUpdateHandler.postDelayed(this, 50)
                }
            }

            seekbarUpdateHandler.postDelayed(updateSeekBar, 0)

            seekBar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar:SeekBar, progress:Int, fromUser:Boolean) {
                    if (fromUser)
                        mediaPlayer.seekTo(progress)
                }
                override fun onStartTrackingTouch(seekBar:SeekBar) {
                }
                override fun onStopTrackingTouch(seekBar:SeekBar) {
                }
            })

            val coverFile = File(coverList[currentSong])
            if(coverFile.exists()){
                val bitmap = BitmapFactory.decodeFile(coverFile.absolutePath)
                barCoverImage.setImageBitmap(bitmap)
            }

            showNotification(titleList[currentSong], artistList[currentSong], coverList[currentSong], true)
            playButton.setImageDrawable(context.resources.getDrawable(R.drawable.ic_pause_black_24dp))
        }catch (e: IndexOutOfBoundsException){
            //Something bad happend, resetting
            MainActivity.musicPlayer = MusicPlayer(context, textView1, textView2, seekBar, barCoverImage, musicBar, playButton)
        }
    }

    fun stop(){
        playing = false
        textView1.text = ""
        textView2.text = ""
        playButton.setImageDrawable(context.resources.getDrawable(R.drawable.ic_play_arrow_black_24dp))
        mediaPlayer.stop()
    }

    fun pause(){
        mediaPlayer.pause()
        showNotification(titleList[currentSong], artistList[currentSong], coverList[currentSong], false)
        playButton.setImageDrawable(context.resources.getDrawable(R.drawable.ic_play_arrow_black_24dp))
        playing = false
    }

    //TODO catch index out of bounds exc
    fun resume(){
        val length = mediaPlayer.currentPosition

        mediaPlayer.seekTo(length)
        mediaPlayer.start()

        showNotification(titleList[currentSong], artistList[currentSong], coverList[currentSong], true)
        playButton.setImageDrawable(context.resources.getDrawable(R.drawable.ic_pause_black_24dp))
        playing = true
    }

    fun next(){
        currentSong++

        try{
            if(playList[currentSong].isEmpty()){
                stop()
            }else{
                play()
            }
        }catch (e:IndexOutOfBoundsException){
            stop()
        }
    }

    fun previous(){
        if(currentSong != 0){
            currentSong--

            try{
                if(playList[currentSong].isEmpty()){
                    stop()
                }else{
                    play()
                }
            }catch (e:IndexOutOfBoundsException){
                stop()
            }
        }
    }

    fun addSong(url:String, title:String, artistName: String , coverUrl:String){
        playList.add(url)
        titleList.add(title)
        coverList.add(coverUrl)
        artistList.add(artistName)

        if(!playing){
            play()
        }
    }

    fun toggleMusic(){
        if(playing){
            pause()
        }else{
            resume()
        }
    }

    private fun showNotificationAndroidO(titleName:String, artistName:String ,coverUrl: String, playing:Boolean){
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            createNotificationChannel()

            var backgroundColor = Color.WHITE
            val expandedRemoteViews:RemoteViews
            //val normalRemoteViews = RemoteViews(context.packageName, R.layout.notification_normal)

            val coverFile = File(coverUrl)
            if(coverFile.exists()){
                val bitmap = BitmapFactory.decodeFile(coverFile.absolutePath)
                backgroundColor = getDominantColor(bitmap)

                if(getTextColor(backgroundColor) == Color.WHITE){
                    expandedRemoteViews = RemoteViews(context.packageName, R.layout.notification_expanded_white)
                    expandedRemoteViews.setTextColor(R.id.songname, Color.WHITE)
                    expandedRemoteViews.setTextColor(R.id.artistname, Color.WHITE)
                }else{
                    expandedRemoteViews = RemoteViews(context.packageName, R.layout.notification_expanded)
                    expandedRemoteViews.setTextColor(R.id.songname, Color.BLACK)
                    expandedRemoteViews.setTextColor(R.id.artistname, Color.BLACK)
                }

                expandedRemoteViews.setImageViewBitmap(R.id.coverimageview, bitmap)

                expandedRemoteViews.setInt(R.id.notificationlayout, "setBackgroundColor", backgroundColor)
            }else{
                expandedRemoteViews = RemoteViews(context.packageName, R.layout.notification_expanded)
            }

            expandedRemoteViews.setTextViewText(R.id.artistname, artistName)
            expandedRemoteViews.setTextViewText(R.id.songname, titleName)

            if(playing){
                expandedRemoteViews.setViewVisibility(R.id.playButton, View.GONE)
                expandedRemoteViews.setViewVisibility(R.id.pauseButton, View.VISIBLE)
            }else{
                expandedRemoteViews.setViewVisibility(R.id.playButton, View.VISIBLE)
                expandedRemoteViews.setViewVisibility(R.id.pauseButton, View.GONE)
            }

            val notificationBuilder = Notification.Builder(context, CHANNEL_ID)
            notificationBuilder.setSmallIcon(R.drawable.ic_play_circle_filled_black_24dp)
            notificationBuilder.setPriority(Notification.PRIORITY_LOW)
            notificationBuilder.setOngoing(true)

            //notificationBuilder.setColorized(true)
            //notificationBuilder.color = backgroundColor

            notificationBuilder.setStyle(Notification.DecoratedCustomViewStyle())
            //notificationBuilder.setCustomContentView(normalRemoteViews)
            notificationBuilder.setCustomBigContentView(expandedRemoteViews)

            val mediaSession = MediaSession(context, "de.lucaspape.monstercat.music")

            notificationBuilder.setStyle(Notification.MediaStyle()
                .setMediaSession(mediaSession.sessionToken))

            setListeners(expandedRemoteViews, context)

            val notificationManagerCompat = NotificationManagerCompat.from(context)
            notificationManagerCompat.notify(NOTIFICATION_ID, notificationBuilder.build())

            context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PREVIOUS))
            context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_DELETE))
            context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PAUSE))
            context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PLAY))
            context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_NEXT))
        }
    }

    //android < sdk 26
    private fun showNotification(titleName:String, artistName:String, coverUrl: String, playing:Boolean){
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O){
            createNotificationChannel()

            var backgroundColor = Color.WHITE
            val normalRemoteViews = RemoteViews(context.packageName, R.layout.notification_normal)
            val expandedRemoteViews:RemoteViews

            val coverFile = File(coverUrl)
            if(coverFile.exists()){
                val bitmap = BitmapFactory.decodeFile(coverFile.absolutePath)
                backgroundColor = getDominantColor(bitmap)

                if(getTextColor(backgroundColor) == Color.WHITE){
                    expandedRemoteViews = RemoteViews(context.packageName, R.layout.notification_expanded_white)
                    expandedRemoteViews.setTextColor(R.id.songname, Color.WHITE)
                    expandedRemoteViews.setTextColor(R.id.artistname, Color.WHITE)
                }else{
                    expandedRemoteViews = RemoteViews(context.packageName, R.layout.notification_expanded)
                    expandedRemoteViews.setTextColor(R.id.songname, Color.BLACK)
                    expandedRemoteViews.setTextColor(R.id.artistname, Color.BLACK)
                }

                expandedRemoteViews.setImageViewBitmap(R.id.coverimageview, bitmap)

                expandedRemoteViews.setInt(R.id.notificationlayout, "setBackgroundColor", backgroundColor)
            }else{
                expandedRemoteViews = RemoteViews(context.packageName, R.layout.notification_expanded)
            }

            expandedRemoteViews.setTextViewText(R.id.songname, titleName)
            expandedRemoteViews.setTextViewText(R.id.songname, titleName)

            if(playing){
                expandedRemoteViews.setViewVisibility(R.id.playButton, View.GONE)
                expandedRemoteViews.setViewVisibility(R.id.pauseButton, View.VISIBLE)
            }else{
                expandedRemoteViews.setViewVisibility(R.id.playButton, View.VISIBLE)
                expandedRemoteViews.setViewVisibility(R.id.pauseButton, View.GONE)
            }

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            notificationBuilder.setSmallIcon(R.drawable.ic_play_circle_filled_black_24dp)
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_LOW)
            notificationBuilder.setOngoing(true)

            //notificationBuilder.setColorized(true)
            //notificationBuilder.color = backgroundColor

            notificationBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            notificationBuilder.setCustomContentView(normalRemoteViews)
            notificationBuilder.setCustomBigContentView(expandedRemoteViews)

            setListeners(expandedRemoteViews, context)

            val notificationManagerCompat = NotificationManagerCompat.from(context)
            notificationManagerCompat.notify(NOTIFICATION_ID, notificationBuilder.build())

            context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PREVIOUS))
            context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_DELETE))
            context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PAUSE))
            context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PLAY))
            context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_NEXT))
        }else{
            showNotificationAndroidO(titleName, artistName, coverUrl, playing)
        }
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val channelName = "Music Notification"
            val channelDescription = "Handy dandy description"
            val importance = NotificationManager.IMPORTANCE_LOW

            val notificationChannel = NotificationChannel(CHANNEL_ID, channelName, importance)

            notificationChannel.description = channelDescription

            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)

        }
    }

    fun setListeners(view:RemoteViews, context:Context){
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
            if(intent!!.action.equals(NOTIFICATION_PREVIOUS)){
                MainActivity.musicPlayer!!.previous()
            }else if(intent.action.equals(NOTIFICATION_DELETE)){
                val notificationManager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(MainActivity.musicPlayer!!.NOTIFICATION_ID)
                MainActivity.musicPlayer!!.pause()
            }else if(intent.action.equals(NOTIFICATION_PAUSE)){
                MainActivity.musicPlayer!!.pause()
            }else if(intent.action.equals(NOTIFICATION_PLAY)){
                MainActivity.musicPlayer!!.resume()
            }else if(intent.action.equals(NOTIFICATION_NEXT)){
                MainActivity.musicPlayer!!.next()
            }
        }
    }

    fun getDominantColor(bitmap: Bitmap):Int{
        val swatchesTemp = Palette.from(bitmap).generate().swatches
        val swatches = ArrayList<Palette.Swatch>(swatchesTemp)

        Collections.sort(swatches, object:Comparator<Palette.Swatch>{
            override fun compare(swatch1:Palette.Swatch, swatch2:Palette.Swatch):Int{
                return swatch2.population - swatch1.population
            }
        })

        return if (swatches.size > 0) swatches.get(0).getRgb() else Color.WHITE
    }

    fun getTextColor(background:Int):Int{
        val backgroundRed = Color.red(background)
        val backgroundGreen = Color.green(background)
        val backgroundBlue = Color.blue(background)

        val luma = ((0.299 * backgroundRed) + (0.587 * backgroundGreen) + (0.114 * backgroundBlue)) / 255;

        return if (luma > 0.5) Color.BLACK else Color.WHITE
    }


}