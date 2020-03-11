package de.lucaspape.monstercat.handlers.abstract_items

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

interface ViewHolderInterface {
    fun setCoverBitmap(albumId:String, bitmap: Bitmap?)

    fun setCoverDrawable(albumId: String, drawable:Drawable?)
}