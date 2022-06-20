package com.example.scorpiochat

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore

private const val profilePictureScorpioChat = "ProfilePictureScorpioChat"

fun getImageUri(image: Bitmap, context: Context): Uri? {
    val path = MediaStore.Images.Media.insertImage(context.contentResolver, image, profilePictureScorpioChat, null)
    return Uri.parse(path)
}

fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap? {
    var width = image.width
    var height = image.height
    val bitmapRatio = width.toFloat() / height.toFloat()

    if (bitmapRatio > 1) {
        width = maxSize
        height = (width / bitmapRatio).toInt()
    } else {
        height = maxSize
        width = (height * bitmapRatio).toInt()
    }

    return Bitmap.createScaledBitmap(image, width, height, true)
}