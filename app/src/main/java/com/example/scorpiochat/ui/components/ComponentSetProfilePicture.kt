package com.example.scorpiochat.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.scorpiochat.databinding.ComponentSetProfilePictureBinding
import com.example.scorpiochat.getImageUri
import com.example.scorpiochat.getResizedBitmap

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
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                val uri = getImageUri(imageBitmap, context)
                setProfilePictureFromUri(uri, fragment.requireContext())
                setProfilePictureUri(uri)
            }
        }

        resultGalleryPhoto = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageBitmap: Bitmap
                val contentResolver = context.contentResolver

                imageBitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(contentResolver, data?.data)
                } else {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, data?.data!!))
                }
                val resizedBitmap = getResizedBitmap(imageBitmap, 1000)

                if (resizedBitmap != null) {
                    val uri = getImageUri(resizedBitmap, context)
                    setProfilePictureFromUri(uri, fragment.requireContext())
                    setProfilePictureUri(uri)
                }
            }
        }
        activateListeners()
    }


    fun setProfilePictureFromUri(uri: Uri?, context: Context) {


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
        var heigh = parameters.height

        if (large) {
            width /= 2
            heigh /= 2
        } else {
            width *= 2
            heigh *= 2
        }
        parameters.width = width
        parameters.height = heigh
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
                resultCameraPhoto.launch(takePictureIntent)
            }


            imgProfilePicture.setOnClickListener {
                changePictureSize(large)
                large = large.not()
            }
        }
    }

}