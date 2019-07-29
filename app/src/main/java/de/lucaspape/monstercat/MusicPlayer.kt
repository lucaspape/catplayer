package de.lucaspape.monstercat

import android.media.AudioManager
import android.media.MediaPlayer

class MusicPlayer() {
    private var mediaPlayer = MediaPlayer()
    private var currentSong = 0
    private var playList = ArrayList<String>()
    private var playing = false

    fun play(){
        mediaPlayer.stop()
        mediaPlayer = MediaPlayer()

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mediaPlayer.setDataSource(playList[currentSong])
        mediaPlayer.prepare()
        mediaPlayer.start()

        playing = true

        mediaPlayer.setOnCompletionListener {
            next()
        }
    }

    fun stop(){
        playing = false
        mediaPlayer.stop()
    }

    fun next(){
        currentSong++

        if(playList[currentSong].isEmpty()){
            stop()
        }else{
            play()
        }
    }

    fun addSong(url:String){
        playList.add(url)

        if(!playing){
            play()
        }
    }
}