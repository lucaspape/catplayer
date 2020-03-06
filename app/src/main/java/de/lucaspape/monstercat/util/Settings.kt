package de.lucaspape.monstercat.util

import android.content.Context
import de.lucaspape.monstercat.R

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
        
        if (getSetting(context.getString(R.string.downloadTypeSetting)) == null || overwrite) {
            saveSetting(context.getString(R.string.downloadTypeSetting), defaultDownloadType)
        }

        if (getSetting(context.getString(R.string.primaryCoverResolutionSetting)) == null || getSetting(
                context.getString(R.string.secondaryCoverResolutionSetting)
            ) == null || overwrite
        ) {
            saveSetting(
                context.getString(R.string.primaryCoverResolutionSetting),
                defaultPrimaryCoverResolution
            )
            saveSetting(
                context.getString(R.string.secondaryCoverResolutionSetting),
                defaultSecondaryCoverResolution
            )
        }

        if (getSetting(context.getString(R.string.streamOverMobileSetting)) == null || overwrite) {
            saveSetting(
                context.getString(R.string.streamOverMobileSetting),
                defaultStreamOverMobile
            )
        }

        if (getSetting(context.getString(R.string.downloadOverMobileSetting)) == null || overwrite) {
            saveSetting(
                context.getString(R.string.downloadOverMobileSetting),
                defaultDownloadOverMobile
            )
        }

        if (getSetting(context.getString(R.string.downloadCoversOverMobileSetting)) == null || overwrite) {
            saveSetting(
                context.getString(R.string.downloadCoversOverMobileSetting),
                defaultDownloadCoversMobile
            )
        }

        if (getSetting(context.getString(R.string.crossfadeTimeSetting)) == null || overwrite) {
            saveSetting(context.getString(R.string.crossfadeTimeSetting), defaultCrossfadeTime)
        }

    }
}