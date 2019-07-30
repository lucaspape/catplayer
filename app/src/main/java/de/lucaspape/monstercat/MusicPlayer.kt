package de.lucaspape.monstercat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.lang.IndexOutOfBoundsException

class MusicPlayer(private var context: Context, private var textView: TextView, private var seekBar: SeekBar) {

    private var mediaPlayer = MediaPlayer()
    private var currentSong = 0
    private var playList = ArrayList<String>()
    private var titleList = ArrayList<String>()
    private var playing = false

    //notification var
    private val CHANNEL_ID = "Music Notification"
    private val NOTIFICATION_ID = 1

    fun play(){
        mediaPlayer.stop()
        mediaPlayer = MediaPlayer()

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mediaPlayer.setDataSource(playList[currentSong])
        mediaPlayer.prepare()
        mediaPlayer.start()

        playing = true

        textView.text = titleList[currentSong]

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

        showNotification()

    }

    fun stop(){
        playing = false
        textView.text = ""
        mediaPlayer.stop()
    }

    fun pause(){
        mediaPlayer.pause()
        playing = false
    }

    fun resume(){
        val length = mediaPlayer.currentPosition

        mediaPlayer.seekTo(length)
        mediaPlayer.start()

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

    fun addSong(url:String, title:String){
        playList.add(url)
        titleList.add(title)

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

    private fun showNotification(){
        createNotificationChannel()
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
        notificationBuilder.setSmallIcon(R.drawable.ic_play_circle_filled_black_24dp)
        notificationBuilder.setContentTitle("Monstercat")
        notificationBuilder.setContentTitle("Notification (hint, its amazing)")
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_LOW)
        notificationBuilder.setOngoing(true)


        val notificationManagerCompat = NotificationManagerCompat.from(context)
        notificationManagerCompat.notify(NOTIFICATION_ID, notificationBuilder.build())
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

}