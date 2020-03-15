package de.lucaspape.monstercat.download

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

interface ImageReceiverInterface {
    fun setBitmap(id: String, bitmap: Bitmap?)

    fun setDrawable(id: String, drawable: Drawable?)
}