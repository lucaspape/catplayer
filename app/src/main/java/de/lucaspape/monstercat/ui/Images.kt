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

fun setFallbackCoverFiles(context: Context) {
    fallbackFile = File("${context.dataDir}/fallback.webp")
    fallbackFileLow = File("${context.dataDir}/fallback_low.webp")

    fallbackBlackFile = File("${context.dataDir}/fallback_black.webp")
    fallbackBlackFileLow = File("${context.dataDir}/fallback_black_low.webp")
    fallbackWhiteFile = File("${context.dataDir}/fallback_white.webp")
    fallbackWhiteFileLow = File("${context.dataDir}/fallback_white_low.webp")
}

var offlineDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_offline_pin_24dp"
var downloadDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_file_download_24dp"
var addToQueueDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_play_24"
var shareDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_share_24dp"
var openInAppDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_open_in_new_24dp"
var addToPlaylistDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_add_24"
var deleteDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_delete_outline_24"
var editDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_edit_24"
var playlistPublicDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_public_24"
var playlistPrivateDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_block_24"
var playButtonDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_play_arrow_24dp"
var pauseButtonDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_pause_24dp"
var createPlaylistDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_add_24dp"
var addPlaylistDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_add_24"
var backButtonDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_arrow_back_24"
const val emptyDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_empty_24dp"

private const val offlineDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_offline_pin_black_24dp"
private const val downloadDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_file_download_black_24dp"
private const val addToQueueDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_play_black_24"
private const val shareDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_share_black_24dp"
private const val openInAppDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_open_in_new_black_24dp"
private const val addToPlaylistDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_add_black_24"
private const val deleteDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_delete_outline_black_24"
private const val editDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_edit_black_24"
private const val playlistPublicDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_public_black_24"
private const val playlistPrivateDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_block_black_24"
private const val playButtonDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_play_arrow_black_24dp"
private const val pauseButtonDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_pause_black_24dp"
private const val createPlaylistDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_add_black_24dp"
private const val addPlaylistDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_add_black_24"
private const val backButtonDrawableBlack =
    "android.resource://de.lucaspape.monstercat/drawable/ic_arrow_back_black_24dp"

private const val offlineDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_offline_pin_white_24dp"
private const val downloadDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_file_download_white_24dp"
private const val addToQueueDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_play_white_24"
private const val shareDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_share_white_24dp"
private const val openInAppDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_open_in_new_white_24dp"
private const val addToPlaylistDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_add_white_24"
private const val deleteDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_delete_outline_white_24"
private const val editDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_edit_white_24"
private const val playlistPublicDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_public_white_24"
private const val playlistPrivateDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_block_white_24"
private const val playButtonDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_play_arrow_white_24dp"
private const val pauseButtonDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_pause_white_24dp"
private const val createPlaylistDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_add_white_24dp"
private const val addPlaylistDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_add_white_24"
private const val backButtonDrawableWhite =
    "android.resource://de.lucaspape.monstercat/drawable/ic_arrow_back_white_24"

fun switchDrawablesToWhite() {
    offlineDrawable =
        offlineDrawableWhite
    downloadDrawable =
        downloadDrawableWhite
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