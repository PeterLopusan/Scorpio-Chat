package com.example.scorpiochat.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import com.bumptech.glide.load.resource.bitmap.TransformationUtils

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

fun rotateBitmap(bitmap: Bitmap, filePath: String): Bitmap {
    val exitInterface = ExifInterface(filePath)
    val rotatedBitmap = when (exitInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> TransformationUtils.rotateImage(bitmap, 90)
        ExifInterface.ORIENTATION_ROTATE_180 -> TransformationUtils.rotateImage(bitmap, 180)
        ExifInterface.ORIENTATION_ROTATE_270 -> TransformationUtils.rotateImage(bitmap, 270)
        ExifInterface.ORIENTATION_NORMAL -> bitmap
        else -> bitmap
    }
    return rotatedBitmap
}