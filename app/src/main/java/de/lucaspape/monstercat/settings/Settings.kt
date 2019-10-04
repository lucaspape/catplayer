package de.lucaspape.monstercat.settings

import android.content.Context
import de.lucaspape.monstercat.R
import java.io.*

class Settings(private val context: Context) {

    init {
        setDefaultSettings(false)
    }

    fun getSetting(key: String): String? {
        try {
            return try {
                val settingsFile = File(context.getString(R.string.settingsFile, context.filesDir.toString()))
                if (settingsFile.exists()) {
                    val ois = ObjectInputStream(FileInputStream(settingsFile))
                    val settingsMap = ois.readObject() as HashMap<String, String>
                    ois.close()

                    settingsMap[key]
                } else {
                    null
                }
            } catch (e: EOFException) {
                //ah shit, here we go again
                getSetting(key)
            }
        } catch (e: StreamCorruptedException) {
            return getSetting(key)
        }

    }

    fun saveSetting(key: String, setting: String) {
        try {
            try {
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
            } catch (e: EOFException) {
                //ah shit, here we go again
                saveSetting(key, setting)
            }
        } catch (e: StreamCorruptedException) {
            saveSetting(key, setting)
        }
    }

    private fun setDefaultSettings(overwrite: Boolean) {
        if (getSetting("audioQuality") == null) {
            saveSetting("downloadType", "mp3")
            saveSetting("downloadQuality", "320")
        }

        if (getSetting("primaryCoverResolution") == null) {
            saveSetting("primaryCoverResolution", "512")
            saveSetting("secondaryCoverResolution", "64")
        }

        if(getSetting("streamOverMobile") == null){
            saveSetting("streamOverMobile", "false")
        }

        if(getSetting("downloadOverMobile") == null){
            saveSetting("downloadOverMobile", "false")
        }

        if(getSetting("downloadCoversOverMobile") == null){
            saveSetting("downloadCoversOverMobile", "false")
        }

        if (overwrite) {
            saveSetting("downloadType", "mp3")
            saveSetting("downloadQuality", "320")

            saveSetting("primaryCoverResolution", "512")
            saveSetting("secondaryCoverResolution", "64")

            saveSetting("streamOverMobile", "false")
            saveSetting("downloadOverMobile", "false")
            saveSetting("downloadCoversOverMobile", "false")
        }

    }

}