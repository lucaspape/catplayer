package de.lucaspape.monstercat.core.util

import android.content.Context
import android.content.SharedPreferences
import de.lucaspape.monstercat.R

class Settings {

    companion object {
        private const val version = "1.3"
        
        @JvmStatic
        private var sharedPreferences:SharedPreferences? = null

        @JvmStatic
        fun getSettings(context: Context): Settings {
            if(sharedPreferences == null){
                sharedPreferences = context.getSharedPreferences("settings", 0)
            }
            
            val settings = Settings()

            if (settings.getString("settings-version") != version) {
                settings.onUpgrade(context)
            } else {
                settings.setDefaultSettings(context, false)
            }

            return settings
        }
    }

    /**
     * Get a setting
     */
    fun getString(key: String): String? {
        return sharedPreferences?.getString(key, null)
    }

    /**
     * Save a setting
     */
    fun setString(key: String, setting: String) {
        val editor = sharedPreferences?.edit()
        editor?.putString(key, setting)
        editor?.apply()
    }

    fun setBoolean(key: String, boolean: Boolean) {
        setString(key, boolean.toString())
    }

    fun getBoolean(key: String): Boolean? {
        return getString(key)?.toBoolean()
    }

    fun setInt(key: String, int: Int) {
        setString(key, int.toString())
    }

    fun getInt(key: String): Int? {
        getString(key)?.let {
            return Integer.parseInt(it)
        }

        return null
    }

    fun setFloat(key: String, float: Float) {
        setString(key, float.toString())
    }

    fun getFloat(key: String): Float? {
        getString(key)?.let {
            return it.toFloatOrNull()
        }

        return null
    }

    private fun onUpgrade(context: Context) {
        setDefaultSettings(context, true)
        setString("settings-version", version)
    }

    /**
     * Set default
     */
    private fun setDefaultSettings(context: Context, overwrite: Boolean) {
        val defaultDownloadType = "mp3_320"
        val defaultPrimaryCoverResolution = 512
        val defaultSecondaryCoverResolution = 128
        val defaultStreamOverMobile = true
        val defaultDownloadOverMobile = false
        val defaultDownloadCoversMobile = true
        val defaultCrossfadeTime = 0
        val defaultSaveCoverImagesToCache = true
        val defaultVolume = 1.0f
        val defaultCustomApiBaseUrl = "https://api.lucaspape.de/monstercat/"
        val defaultSkipSongsFromMonstercat = true
        val defaultUseCustomApiForRelatedSongs = true
        val defaultUseCustomApiForCoverImages = true
        val defaultUseCustomApiForSearch = true

        if (getString(context.getString(R.string.downloadTypeSetting)) == null || overwrite) {
            setString(context.getString(R.string.downloadTypeSetting), defaultDownloadType)
        }

        if (getString(context.getString(R.string.primaryCoverResolutionSetting)) == null || getString(
                context.getString(R.string.secondaryCoverResolutionSetting)
            ) == null || overwrite
        ) {
            setInt(
                context.getString(R.string.primaryCoverResolutionSetting),
                defaultPrimaryCoverResolution
            )
            setInt(
                context.getString(R.string.secondaryCoverResolutionSetting),
                defaultSecondaryCoverResolution
            )
        }

        if (getString(context.getString(R.string.streamOverMobileSetting)) == null || overwrite) {
            setBoolean(
                context.getString(R.string.streamOverMobileSetting),
                defaultStreamOverMobile
            )
        }

        if (getString(context.getString(R.string.downloadOverMobileSetting)) == null || overwrite) {
            setBoolean(
                context.getString(R.string.downloadOverMobileSetting),
                defaultDownloadOverMobile
            )
        }

        if (getString(context.getString(R.string.downloadCoversOverMobileSetting)) == null || overwrite) {
            setBoolean(
                context.getString(R.string.downloadCoversOverMobileSetting),
                defaultDownloadCoversMobile
            )
        }

        if (getString(context.getString(R.string.crossfadeTimeSetting)) == null || overwrite) {
            setInt(context.getString(R.string.crossfadeTimeSetting), defaultCrossfadeTime)
        }

        if (getString(context.getString(R.string.saveCoverImagesToCacheSetting)) == null || overwrite) {
            setBoolean(
                context.getString(R.string.saveCoverImagesToCacheSetting),
                defaultSaveCoverImagesToCache
            )
        }

        if (getString(context.getString(R.string.volumeSetting)) == null || overwrite) {
            setFloat(context.getString(R.string.volumeSetting), defaultVolume)
        }

        if (getString(context.getString(R.string.customApiBaseUrlSetting)) == null || overwrite) {
            setString(context.getString(R.string.customApiBaseUrlSetting), defaultCustomApiBaseUrl)
        }

        if (getString(context.getString(R.string.skipMonstercatSongsSetting)) == null || overwrite) {
            setBoolean(
                context.getString(R.string.skipMonstercatSongsSetting),
                defaultSkipSongsFromMonstercat
            )
        }

        if (getString(context.getString(R.string.useCustomApiForCoverImagesSetting)) == null || overwrite) {
            setBoolean(
                context.getString(R.string.useCustomApiForCoverImagesSetting),
                defaultUseCustomApiForCoverImages
            )
        }

        if (getString(context.getString(R.string.playRelatedSetting)) == null || overwrite) {
            setBoolean(
                context.getString(R.string.playRelatedSetting),
                defaultUseCustomApiForRelatedSongs
            )
        }

        if (getString(context.getString(R.string.useCustomApiForSearchSetting)) == null || overwrite) {
            setBoolean(
                context.getString(R.string.useCustomApiForSearchSetting),
                defaultUseCustomApiForSearch
            )
        }
    }
}