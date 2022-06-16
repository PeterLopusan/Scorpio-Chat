package com.example.scorpiochat.viewModel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.scorpiochat.*
import com.example.scorpiochat.data.AuthenticationState
import com.example.scorpiochat.data.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


private const val emailIsUsed = "com.google.firebase.auth.FirebaseAuthUserCollisionException: The email address is already in use by another account."
private const val invalidPassword = "com.google.firebase.auth.FirebaseAuthWeakPasswordException: The given password is invalid. [ Password should be at least 6 characters ]"
private const val emailBadlyFormatted = "com.google.firebase.auth.FirebaseAuthInvalidCredentialsException: The email address is badly formatted."


class LoginViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val storage = FirebaseStorage.getInstance().reference
    private val database = FirebaseDatabase.getInstance().reference

    val authenticationState = FirebaseUserLiveData().map {
        if (auth.currentUser != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }

    fun createAccount(email: String, password: String, username: String, uri: Uri?, context: Context): MutableLiveData<String> {
        val text: MutableLiveData<String> = MutableLiveData()
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (uri == null) {
                    storage.child(defaultProfilePicture).child(default_icon).downloadUrl.addOnCompleteListener {
                        saveUserInfo(username, it.result.toString())
                    }
                } else {
                    storage.child(auth.uid!!).child(customProfilePicture).putFile(uri).addOnCompleteListener {
                        storage.child(auth.uid!!).child(customProfilePicture).downloadUrl.addOnCompleteListener {
                            saveUserInfo(username, it.result.toString())
                        }
                    }
                }
                text.value = context.getString(R.string.welcome_to_scorpio_chat)
            }
        }.addOnFailureListener {
            when(it.toString()){
                emailIsUsed -> text.value = context.getString(R.string.email_is_already_used)
                emailBadlyFormatted -> text.value = context.getString(R.string.email_badly_formatted)
                invalidPassword -> text.value = context.getString(R.string.invalid_password)
            }
        }
        return text
    }

    private fun saveUserInfo(username: String, profilePictureUri: String) {
        val userId = auth.currentUser!!.uid
        val userInfo = User(username = username, userId = userId, online = true, lastSeen = System.currentTimeMillis(), customProfilePictureUri = profilePictureUri)
        database.child(userId).child(userInformation).setValue(userInfo)
    }

    fun login(email: String, password: String): MutableLiveData<Boolean> {
        val isSuccessful: MutableLiveData<Boolean> = MutableLiveData()
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            isSuccessful.value = task.isSuccessful
        }
        return isSuccessful
    }

    fun getStorageWithDefaultPictureImage(): StorageReference {
        return storage.child(defaultProfilePicture).child(default_icon)
    }

    fun subscribeTopic(context: Context) {
        val userId = auth.uid!!
        Firebase.messaging.subscribeToTopic(userId)
        SharedPreferencesManager.setTopic(context, userId)
    }
}