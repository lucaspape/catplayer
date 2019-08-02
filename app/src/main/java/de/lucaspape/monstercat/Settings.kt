package de.lucaspape.monstercat

import android.content.Context
import java.io.*

class Settings(private val context: Context) {

    init {
        setDefaultSettings(true)
    }

    fun getSetting(key: String): String? {
        val settingsFile = File(context.getString(R.string.settingsFile, context.filesDir.toString()))
        if (settingsFile.exists()) {
            val ois = ObjectInputStream(FileInputStream(settingsFile))
            val settingsMap = ois.readObject() as HashMap<String, String>
            ois.close()

            return settingsMap.get(key)
        } else {
            return null
        }

    }

    fun saveSetting(key: String, setting: String) {
        val settingsFile = File(context.getString(R.string.settingsFile, context.filesDir.toString()))
        var settingsMap = HashMap<String, String>()

        if (settingsFile.exists()) {
            val ois = ObjectInputStream(FileInputStream(settingsFile))
            settingsMap = ois.readObject() as HashMap<String, String>
            ois.close()
        }

        settingsMap.put(key, setting)

        val oos = ObjectOutputStream(FileOutputStream(settingsFile))
        oos.writeObject(settingsMap)
        oos.flush()
        oos.close()
    }

    fun setDefaultSettings(overwrite: Boolean) {
        if (getSetting("audioQuality") == null) {
            saveSetting("downloadType", "mp3")
            saveSetting("downloadQuality", "320")
        }

        if(getSetting("primaryCoverResolution") == null){
            saveSetting("primaryCoverResolution", "512")
            saveSetting("secondaryCoverResolution", "64")
        }

        if (overwrite) {
            saveSetting("downloadType", "mp3")
            saveSetting("downloadQuality", "320")

            saveSetting("primaryCoverResolution", "512")
            saveSetting("secondaryCoverResolution", "64")
        }

    }

}