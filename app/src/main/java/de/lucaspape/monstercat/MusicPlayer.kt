package de.lucaspape.monstercat

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.widget.SeekBar
import android.widget.TextView
import java.lang.IndexOutOfBoundsException
import androidx.core.os.HandlerCompat.postDelayed



class MusicPlayer(private var textView: TextView, private var seekBar: SeekBar) {

    private var mediaPlayer = MediaPlayer()
    private var currentSong = 0
    private var playList = ArrayList<String>()
    private var titleList = ArrayList<String>()
    private var playing = false

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

        var seekbarUpdateHandler = Handler()

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

    }

    fun stop(){
        playing = false
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

}