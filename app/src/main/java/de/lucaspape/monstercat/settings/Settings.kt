package de.lucaspape.monstercat.settings

import android.content.Context
import de.lucaspape.monstercat.R
import java.io.*

class Settings(private val context: Context) {

    init {
        setDefaultSettings(true)
    }

    fun getSetting(key: String): String? {
        val settingsFile = File(context.getString(R.string.settingsFile, context.filesDir.toString()))
        return if (settingsFile.exists()) {
            val ois = ObjectInputStream(FileInputStream(settingsFile))
            val settingsMap = ois.readObject() as HashMap<String, String>
            ois.close()

            settingsMap[key]
        } else {
            null
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

        settingsMap[key] = setting

        val oos = ObjectOutputStream(FileOutputStream(settingsFile))
        oos.writeObject(settingsMap)
        oos.flush()
        oos.close()
    }

    private fun setDefaultSettings(overwrite: Boolean) {
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