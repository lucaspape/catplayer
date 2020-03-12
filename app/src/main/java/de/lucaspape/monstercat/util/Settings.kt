package de.lucaspape.monstercat.util

import android.content.Context
import de.lucaspape.monstercat.R

private const val version = "1.3"

class Settings(private val context: Context) {

    init {
        if (getString("settings-version") != version) {
            onUpgrade()
        } else {
            setDefaultSettings(false)
        }
    }

    /**
     * Get a setting
     */
    fun getString(key: String): String? {
        val sharedPreferences = context.getSharedPreferences("settings", 0)
        return sharedPreferences.getString(key, null)
    }

    /**
     * Save a setting
     */
    fun setString(key: String, setting: String) {
        val sharedPreferences = context.getSharedPreferences("settings", 0)
        val editor = sharedPreferences.edit()
        editor.putString(key, setting)
        editor.apply()
    }

    fun setBoolean(key: String, boolean: Boolean){
        setString(key, boolean.toString())
    }

    fun getBoolean(key: String):Boolean?{
        return getString(key)?.toBoolean()
    }

    fun setInt(key: String, int:Int){
        setString(key, int.toString())
    }

    fun getInt(key: String):Int?{
        getString(key)?.let {
            return Integer.parseInt(it)
        }

        return null
    }

    private fun onUpgrade() {
        setDefaultSettings(true)
        setString("settings-version", version)
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
        
        if (getString(context.getString(R.string.downloadTypeSetting)) == null || overwrite) {
            setString(context.getString(R.string.downloadTypeSetting), defaultDownloadType)
        }

        if (getString(context.getString(R.string.primaryCoverResolutionSetting)) == null || getString(
                context.getString(R.string.secondaryCoverResolutionSetting)
            ) == null || overwrite
        ) {
            setString(
                context.getString(R.string.primaryCoverResolutionSetting),
                defaultPrimaryCoverResolution
            )
            setString(
                context.getString(R.string.secondaryCoverResolutionSetting),
                defaultSecondaryCoverResolution
            )
        }

        if (getString(context.getString(R.string.streamOverMobileSetting)) == null || overwrite) {
            setString(
                context.getString(R.string.streamOverMobileSetting),
                defaultStreamOverMobile
            )
        }

        if (getString(context.getString(R.string.downloadOverMobileSetting)) == null || overwrite) {
            setString(
                context.getString(R.string.downloadOverMobileSetting),
                defaultDownloadOverMobile
            )
        }

        if (getString(context.getString(R.string.downloadCoversOverMobileSetting)) == null || overwrite) {
            setString(
                context.getString(R.string.downloadCoversOverMobileSetting),
                defaultDownloadCoversMobile
            )
        }

        if (getString(context.getString(R.string.crossfadeTimeSetting)) == null || overwrite) {
            setString(context.getString(R.string.crossfadeTimeSetting), defaultCrossfadeTime)
        }

    }
}