package de.lucaspape.monstercat.util

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

interface ImageReceiverInterface {
    fun setBitmap(albumId:String, bitmap: Bitmap?)

    fun setDrawable(albumId: String, drawable: Drawable?)
}