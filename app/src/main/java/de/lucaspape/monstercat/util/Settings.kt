package de.lucaspape.monstercat.util

import android.content.Context

private const val version = "1.3"

class Settings(private val context: Context) {

    init {
        if (getSetting("settings-version") != version) {
            onUpgrade()
        } else {
            setDefaultSettings(false)
        }
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

    private fun onUpgrade() {
        setDefaultSettings(true)
        saveSetting("settings-version", version)
    }

    /**
     * Set default
     */
    private fun setDefaultSettings(overwrite: Boolean) {
        val defaultDownloadType = "mp3_320"
        val defaultPrimaryCoverResolution = "512"
        val defaultSecondaryCoverResolution = "128"
        val defaultStreamOverMobile = false.toString()
        val defaultDownloadOverMobile = false.toString()
        val defaultDownloadCoversMobile = false.toString()
        val defaultCrossfadeTime = 0.toString()

        if (overwrite) {
            saveSetting("downloadType", defaultDownloadType)

            saveSetting("primaryCoverResolution", defaultPrimaryCoverResolution)
            saveSetting("secondaryCoverResolution", defaultSecondaryCoverResolution)

            saveSetting("streamOverMobile", defaultStreamOverMobile)
            saveSetting("downloadOverMobile", defaultDownloadOverMobile)
            saveSetting("downloadCoversOverMobile", defaultDownloadCoversMobile)
            saveSetting("crossfadeTime", defaultCrossfadeTime)
        } else {
            if (getSetting("downloadType") == null) {
                saveSetting("downloadType", defaultDownloadType)
            }

            if (getSetting("primaryCoverResolution") == null || getSetting("secondaryCoverResolution") == null) {
                saveSetting("primaryCoverResolution", defaultPrimaryCoverResolution)
                saveSetting("secondaryCoverResolution", defaultSecondaryCoverResolution)
            }

            if (getSetting("streamOverMobile") == null) {
                saveSetting("streamOverMobile", defaultStreamOverMobile)
            }

            if (getSetting("downloadOverMobile") == null) {
                saveSetting("downloadOverMobile", defaultDownloadOverMobile)
            }

            if (getSetting("downloadCoversOverMobile") == null) {
                saveSetting("downloadCoversOverMobile", defaultDownloadCoversMobile)
            }

            if(getSetting("crossfadeTime") == null){
                saveSetting("crossfadeTime", defaultCrossfadeTime)
            }
        }
    }
}