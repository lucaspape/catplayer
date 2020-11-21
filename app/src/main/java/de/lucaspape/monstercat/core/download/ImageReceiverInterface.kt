package de.lucaspape.monstercat.core.download

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.squareup.picasso.Target

interface ImageReceiverInterface {
    fun setBitmap(id: String, bitmap: Bitmap?)

    fun setDrawable(id: String, drawable: Drawable?)

    fun setTag(target:Target)
}