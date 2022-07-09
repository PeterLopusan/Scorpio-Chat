package com.example.scorpiochat.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.scorpiochat.databinding.ComponentSetProfilePictureBinding
import com.example.scorpiochat.utils.getImageUri
import com.example.scorpiochat.utils.getResizedBitmap
import com.example.scorpiochat.utils.rotateBitmap
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class ComponentSetProfilePicture @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttrs: Int = 0) : LinearLayout(context, attrs, defStyleAttrs) {

    private val binding: ComponentSetProfilePictureBinding by lazy {
        ComponentSetProfilePictureBinding.inflate(LayoutInflater.from(context), this, true)
    }

    var profilePicture: Uri? = null
    private lateinit var resultCameraPhoto: ActivityResultLauncher<Intent>
    private lateinit var resultGalleryPhoto: ActivityResultLauncher<Intent>


    fun setRegisterForActivityResult(fragment: Fragment) {
        resultCameraPhoto = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
                    val file = File(profilePicture.toString())
                    mediaScanIntent.data = Uri.fromFile(file)
                    context.sendBroadcast(mediaScanIntent)
                }

                val imageBitmap = BitmapFactory.decodeFile(profilePicture.toString())
                val rotatedBitmap = rotateBitmap(imageBitmap, profilePicture.toString())
                val resizedBitmap = getResizedBitmap(rotatedBitmap, 1000)

                if (resizedBitmap != null) {
                    val uri = getImageUri(resizedBitmap, context)
                    setProfilePictureFromUri(uri)
                    setProfilePictureUri(uri)
                }
            }
        }

        resultGalleryPhoto = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val inputStream = context.contentResolver.openInputStream(data?.data!!)
                val imageBitmap = BitmapFactory.decodeStream(inputStream)
                val resizedBitmap = getResizedBitmap(imageBitmap, 1000)

                if (resizedBitmap != null) {
                    val uri = getImageUri(resizedBitmap, context)
                    setProfilePictureFromUri(uri)
                    setProfilePictureUri(uri)
                }
            }
        }
        activateListeners()
    }


    fun setProfilePictureFromUri(uri: Uri?) {
        Glide.with(context)
            .load(uri)
            .into(binding.imgProfilePicture)
    }

    fun setProfilePictureUri(uri: Uri?) {
        profilePicture = uri
    }

    private fun changePictureSize(large: Boolean) {
        val parameters = binding.imgProfilePicture.layoutParams
        var width = parameters.width
        var height = parameters.height

        if (large) {
            width /= 2
            height /= 2
        } else {
            width *= 2
            height *= 2
        }
        parameters.width = width
        parameters.height = height
        binding.imgProfilePicture.layoutParams = parameters
    }

    private fun activateListeners() {
        var large = false

        binding.apply {

            btnSelectFromGallery.setOnClickListener {
                val intent = Intent().setType("image/*").setAction(Intent.ACTION_GET_CONTENT)
                resultGalleryPhoto.launch(intent)
            }

            btnTakePhoto.setOnClickListener {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                takePictureIntent.resolveActivity(context.packageManager)?.also {
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        null
                    }

                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(context, "com.example.android.fileprovider", it)
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    }
                }
                resultCameraPhoto.launch(takePictureIntent)
            }

            imgProfilePicture.setOnClickListener {
                changePictureSize(large)
                large = large.not()
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            profilePicture = absolutePath.toUri()
        }
    }
}