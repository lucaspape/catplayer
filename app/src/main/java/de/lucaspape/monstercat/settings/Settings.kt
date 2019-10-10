package de.lucaspape.monstercat.settings

import android.content.Context

class Settings(private val context: Context) {

    init {
        setDefaultSettings(false)
    }

    /**
     * Get a setting
     */
    fun getSetting(key: String): String? {
        val sharedPreferences = context.getSharedPreferences("settings", 0)
        return sharedPreferences.getString(key, null)
    }

    /**
     * Save a setting
     */
    fun saveSetting(key: String, setting: String) {
        val sharedPreferences = context.getSharedPreferences("settings", 0)
        val editor = sharedPreferences.edit()
        editor.putString(key, setting)
        editor.apply()
    }

    /**
     * Set default
     */
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