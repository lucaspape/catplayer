package de.lucaspape.monstercat.ui

import android.content.Context
import java.io.File

/**
 * Yes this is kinda ugly but it works so i dont give a shit
 */

var fallbackFile = File("")
var fallbackFileLow = File("")

var fallbackBlackFile = File("")
var fallbackBlackFileLow = File("")
var fallbackWhiteFile = File("")
var fallbackWhiteFileLow = File("")

private const val fallbackImagesFileFormat = "jpg"

fun setFallbackCoverFiles(context: Context) {
    fallbackFile = File("${context.dataDir}/fallback.$fallbackImagesFileFormat")
    fallbackFileLow = File("${context.dataDir}/fallback_low.$fallbackImagesFileFormat")

    fallbackBlackFile = File("${context.dataDir}/fallback_black.$fallbackImagesFileFormat")
    fallbackBlackFileLow = File("${context.dataDir}/fallback_black_low.$fallbackImagesFileFormat")
    fallbackWhiteFile = File("${context.dataDir}/fallback_white.$fallbackImagesFileFormat")
    fallbackWhiteFileLow = File("${context.dataDir}/fallback_white_low.$fallbackImagesFileFormat")
}

const val DRAWABLE_URI = "android.resource://de.lucaspape.monstercat/drawable"

var offlineDrawable = "$DRAWABLE_URI/ic_offline_pin_24dp"
var downloadDrawable = "$DRAWABLE_URI/ic_file_download_24dp"
var downloadingDrawable = "$DRAWABLE_URI/ic_baseline_sync_24"
var addToQueueDrawable = "$DRAWABLE_URI/ic_playlist_play_24"
var shareDrawable = "$DRAWABLE_URI/drawable/ic_share_24dp"
var openInAppDrawable = "$DRAWABLE_URI/ic_open_in_new_24dp"
var addToPlaylistDrawable = "$DRAWABLE_URI/ic_playlist_add_24"
var deleteDrawable = "$DRAWABLE_URI/ic_delete_outline_24"
var editDrawable = "$DRAWABLE_URI/ic_edit_24"
var playlistPublicDrawable = "$DRAWABLE_URI/ic_public_24"
var playlistPrivateDrawable = "$DRAWABLE_URI/ic_block_24"
var playButtonDrawable = "$DRAWABLE_URI/ic_play_arrow_24dp"
var pauseButtonDrawable = "$DRAWABLE_URI/ic_pause_24dp"
var createPlaylistDrawable = "$DRAWABLE_URI/ic_add_24dp"
var addPlaylistDrawable = "$DRAWABLE_URI/ic_playlist_add_24"
var backButtonDrawable = "$DRAWABLE_URI/ic_arrow_back_24"
const val emptyDrawable = "$DRAWABLE_URI/ic_empty_24dp"

private const val offlineDrawableBlack =
    "$DRAWABLE_URI/ic_offline_pin_black_24dp"
private const val downloadDrawableBlack =
    "$DRAWABLE_URI/ic_file_download_black_24dp"
private const val downloadingDrawableBlack = "$DRAWABLE_URI/ic_baseline_sync_black_24"
private const val addToQueueDrawableBlack =
    "$DRAWABLE_URI/ic_playlist_play_black_24"
private const val shareDrawableBlack =
    "$DRAWABLE_URI/ic_share_black_24dp"
private const val openInAppDrawableBlack =
    "$DRAWABLE_URI/ic_open_in_new_black_24dp"
private const val addToPlaylistDrawableBlack =
    "$DRAWABLE_URI/ic_playlist_add_black_24"
private const val deleteDrawableBlack =
    "$DRAWABLE_URI/ic_delete_outline_black_24"
private const val editDrawableBlack =
    "$DRAWABLE_URI/ic_edit_black_24"
private const val playlistPublicDrawableBlack =
    "$DRAWABLE_URI/ic_public_black_24"
private const val playlistPrivateDrawableBlack =
    "$DRAWABLE_URI/ic_block_black_24"
private const val playButtonDrawableBlack =
    "$DRAWABLE_URI/ic_play_arrow_black_24dp"
private const val pauseButtonDrawableBlack =
    "$DRAWABLE_URI/ic_pause_black_24dp"
private const val createPlaylistDrawableBlack =
    "$DRAWABLE_URI/ic_add_black_24dp"
private const val addPlaylistDrawableBlack =
    "$DRAWABLE_URI/ic_playlist_add_black_24"
private const val backButtonDrawableBlack =
    "$DRAWABLE_URI/ic_arrow_back_black_24dp"

private const val offlineDrawableWhite =
    "$DRAWABLE_URI/ic_offline_pin_white_24dp"
private const val downloadDrawableWhite =
    "$DRAWABLE_URI/ic_file_download_white_24dp"
private const val downloadingDrawableWhite = "$DRAWABLE_URI/ic_baseline_sync_white_24"
private const val addToQueueDrawableWhite =
    "$DRAWABLE_URI/ic_playlist_play_white_24"
private const val shareDrawableWhite =
    "$DRAWABLE_URI/ic_share_white_24dp"
private const val openInAppDrawableWhite =
    "$DRAWABLE_URI/ic_open_in_new_white_24dp"
private const val addToPlaylistDrawableWhite =
    "$DRAWABLE_URI/ic_playlist_add_white_24"
private const val deleteDrawableWhite =
    "$DRAWABLE_URI/ic_delete_outline_white_24"
private const val editDrawableWhite =
    "$DRAWABLE_URI/ic_edit_white_24"
private const val playlistPublicDrawableWhite =
    "$DRAWABLE_URI/ic_public_white_24"
private const val playlistPrivateDrawableWhite =
    "$DRAWABLE_URI/ic_block_white_24"
private const val playButtonDrawableWhite =
    "$DRAWABLE_URI/ic_play_arrow_white_24dp"
private const val pauseButtonDrawableWhite =
    "$DRAWABLE_URI/ic_pause_white_24dp"
private const val createPlaylistDrawableWhite =
    "$DRAWABLE_URI/ic_add_white_24dp"
private const val addPlaylistDrawableWhite =
    "$DRAWABLE_URI/ic_playlist_add_white_24"
private const val backButtonDrawableWhite =
    "$DRAWABLE_URI/ic_arrow_back_white_24"

fun switchDrawablesToWhite() {
    offlineDrawable =
        offlineDrawableWhite
    downloadDrawable =
        downloadDrawableWhite
    downloadingDrawable = downloadingDrawableWhite
    addToQueueDrawable =
        addToQueueDrawableWhite
    shareDrawable =
        shareDrawableWhite
    openInAppDrawable =
        openInAppDrawableWhite
    addToPlaylistDrawable =
        addToPlaylistDrawableWhite
    deleteDrawable =
        deleteDrawableWhite
    editDrawable =
        editDrawableWhite
    playlistPublicDrawable =
        playlistPublicDrawableWhite
    playlistPrivateDrawable =
        playlistPrivateDrawableWhite
    playButtonDrawable =
        playButtonDrawableWhite
    pauseButtonDrawable =
        pauseButtonDrawableWhite
    createPlaylistDrawable =
        createPlaylistDrawableWhite
    addPlaylistDrawable =
        addPlaylistDrawableWhite
    backButtonDrawable = backButtonDrawableWhite
}

fun switchDrawablesToBlack() {
    offlineDrawable =
        offlineDrawableBlack
    downloadDrawable =
        downloadDrawableBlack
    downloadingDrawable = downloadingDrawableBlack
    addToQueueDrawable =
        addToQueueDrawableBlack
    shareDrawable =
        shareDrawableBlack
    openInAppDrawable =
        openInAppDrawableBlack
    addToPlaylistDrawable =
        addToPlaylistDrawableBlack
    deleteDrawable =
        deleteDrawableBlack
    editDrawable =
        editDrawableBlack
    playlistPublicDrawable =
        playlistPublicDrawableBlack
    playlistPrivateDrawable =
        playlistPrivateDrawableBlack
    playButtonDrawable =
        playButtonDrawableBlack
    pauseButtonDrawable =
        pauseButtonDrawableBlack
    createPlaylistDrawable =
        createPlaylistDrawableBlack
    addPlaylistDrawable =
        addPlaylistDrawableBlack
    backButtonDrawable = backButtonDrawableBlack
}