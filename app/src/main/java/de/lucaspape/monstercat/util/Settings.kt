package de.lucaspape.monstercat.util

import android.content.Context

class Settings(private val context: Context) {

    init {
        setDefaultSettings(true)
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
            saveSetting("downloadType", "flac")
        }

        if (getSetting("primaryCoverResolution") == null) {
            saveSetting("primaryCoverResolution", "1024")
            saveSetting("secondaryCoverResolution", "128")
        }

        if (getSetting("streamOverMobile") == null) {
            saveSetting("streamOverMobile", "false")
        }

        if (getSetting("downloadOverMobile") == null) {
            saveSetting("downloadOverMobile", "false")
        }

        if (getSetting("downloadCoversOverMobile") == null) {
            saveSetting("downloadCoversOverMobile", "false")
        }

        if (getSetting("maximumLoad") == null) {
            saveSetting("maximumLoad", 50.toString())
        }

        if (overwrite) {
            saveSetting("downloadType", "flac")

            saveSetting("primaryCoverResolution", "1024")
            saveSetting("secondaryCoverResolution", "128")

            saveSetting("streamOverMobile", "false")
            saveSetting("downloadOverMobile", "false")
            saveSetting("downloadCoversOverMobile", "false")
            saveSetting("maximumLoad", 50.toString())
        }else{

        }

    }

}