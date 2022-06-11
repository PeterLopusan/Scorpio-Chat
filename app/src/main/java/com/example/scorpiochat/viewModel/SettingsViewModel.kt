package com.example.scorpiochat.viewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scorpiochat.*
import com.example.scorpiochat.data.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


class SettingsViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val storage = FirebaseStorage.getInstance().reference
    private val database = FirebaseDatabase.getInstance().reference
    var userInfo: MutableLiveData<User> = MutableLiveData<User>()

    fun loadUserInfo() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (userInfo.value == null) {
                    for (dbChild in snapshot.children) {
                        val user = dbChild.child(userInformation).getValue(User::class.java)
                        if (user?.userId == auth.uid) {
                            userInfo.value = user!!
                            return
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }
        })
    }

    fun getCurrentProfilePictureStorageRef(): StorageReference {
        val storageReference = if (userInfo.value?.customProfilePicture == true) {
            storage.child(userInfo.value?.userId!!).child(customProfilePicture)
        } else {
            storage.child(defaultProfilePicture).child(default_icon)
        }
        return storageReference
    }

    fun getDefaultProfilePictureStorageRef(): StorageReference {
        return storage.child(defaultProfilePicture).child(default_icon)
    }

    fun changeUsername(newUsername: String) {
        if (newUsername != "") {
            val update = mapOf("username" to newUsername)
            database.child(auth.uid!!).child(userInformation).updateChildren(update)
        }
    }

    fun changeProfilePicture(profilePictureUri: Uri?) {
        val update: Map<String, Boolean>

        if (profilePictureUri == null) {
            update = mapOf("customProfilePicture" to false)
            storage.child(auth.uid!!).child(customProfilePicture).delete()
        } else {
            update = mapOf("customProfilePicture" to true)
            storage.child(auth.uid!!).child(customProfilePicture).putFile(profilePictureUri)
        }

        database.child(auth.uid!!).child(userInformation).updateChildren(update)
    }

    fun changePassword(context: Context) {
        auth.sendPasswordResetEmail(auth.currentUser?.email!!).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, context.getString(R.string.reset_password_mail_send, getUserEmail()), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.reset_password_mail_send_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun changeEmail(newEmail: String, password: String, context: Context) {
        if (newEmail != "") {
            auth.signInWithEmailAndPassword(getUserEmail(), password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser!!.updateEmail(newEmail).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(context, context.getString(R.string.new_email_set), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.new_email_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.new_email_failed), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, context.getString(R.string.new_email_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun signOut(context: Context) {
        unsubscribeTopic(context)
        makeUserOffline()
        auth.signOut()
    }

    private fun makeUserOffline() {
        val update =
            mapOf("online" to false, "lastSeen" to System.currentTimeMillis())
        database.child(auth.uid!!).child(userInformation).updateChildren(update)

    }

    private fun unsubscribeTopic(context: Context) {
        val previousTopic = SharedPreferencesManager.getTopic(context)
        if (previousTopic != null) {
            Firebase.messaging.unsubscribeFromTopic(previousTopic)
        }
    }

    private fun deleteUserData() {
        if (userInfo.value?.customProfilePicture == true) {
            storage.child(auth.uid!!).child(customProfilePicture).delete()
            storage.child(auth.uid!!).delete()
        }
        database.child(auth.uid!!).removeValue()
    }

    fun deleteAccount(context: Context, password: String) {

        auth.signInWithEmailAndPassword(getUserEmail(), password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                unsubscribeTopic(context)
                makeUserOffline()
                deleteUserData()

                auth.currentUser?.delete()?.addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(context, context.getString(R.string.account_was_deleted), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.new_email_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, context.getString(R.string.new_email_failed), Toast.LENGTH_SHORT).show()
            }
        }

    }

    fun getUserEmail(): String {
        return auth.currentUser?.email!!
    }

}