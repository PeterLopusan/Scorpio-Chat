package com.example.scorpiochat.viewModels

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scorpiochat.*
import com.example.scorpiochat.data.*
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
    val userInfo: MutableLiveData<User> = MutableLiveData<User>()

    fun loadUserInfo() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                for (dbChild in snapshot.children) {
                    val user = dbChild.child(userInformation).getValue(User::class.java)
                    if (user?.userId == auth.uid) {
                        userInfo.value = user
                        return
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }
        })
    }

    fun getDefaultProfilePictureStorageRef(): StorageReference {
        return storage.child(defaultProfilePicture).child(default_icon)
    }

    fun changeUsername(newUsername: String) {
        if (newUsername != "") {
            val update = mapOf("username" to newUsername)
            database.child(auth.uid!!).child(userInformation).updateChildren(update).addOnCompleteListener {
                loadUserInfo()
            }
        }
    }

    fun changeProfilePicture(profilePictureUri: Uri?) {
        var update: Map<String, String>

        if (profilePictureUri == null) {
            storage.child(auth.uid!!).child(customProfilePicture).delete()
            storage.child(defaultProfilePicture).child(default_icon).downloadUrl.addOnCompleteListener { task ->
                update = mapOf("customProfilePictureUri" to task.result.toString())
                database.child(auth.uid!!).child(userInformation).updateChildren(update)
            }
        } else {
            storage.child(auth.uid!!).child(customProfilePicture).putFile(profilePictureUri).addOnCompleteListener {
                storage.child(auth.uid!!).child(customProfilePicture).downloadUrl.addOnCompleteListener { task ->
                    update = mapOf("customProfilePictureUri" to task.result.toString())
                    database.child(auth.uid!!).child(userInformation).updateChildren(update)
                }
            }
        }
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

    fun changeEmail(newEmail: String, password: String, context: Context): MutableLiveData<Boolean?> {
        val result: MutableLiveData<Boolean?> = MutableLiveData()

        if (newEmail.isNotBlank() && password.isNotBlank()) {
            auth.signInWithEmailAndPassword(getUserEmail(), password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser!!.updateEmail(newEmail).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(context, context.getString(R.string.new_email_set), Toast.LENGTH_SHORT).show()
                            result.value = true
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
        return result
    }

    fun signOut(context: Context) {
        unsubscribeTopic(context)
        makeUserOffline()
        auth.signOut()
    }

    private fun makeUserOffline() {
        val update = mapOf("online" to false, "lastSeen" to System.currentTimeMillis())
        database.child(auth.uid!!).child(userInformation).updateChildren(update)

    }

    private fun unsubscribeTopic(context: Context) {
        val previousTopic = SharedPreferencesManager.getTopic(context)
        if (previousTopic != null) {
            Firebase.messaging.unsubscribeFromTopic(previousTopic)
        }
    }


    fun deleteAccount(password: String, context: Context) {
        if (password.isNotBlank()) {
            auth.signInWithEmailAndPassword(getUserEmail(), password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    unsubscribeTopic(context)
                    makeUserOffline()
                    database.child(auth.uid!!).child(conversations).removeValue()

                    storage.child(auth.uid!!).apply {
                        child(customProfilePicture).delete()
                        delete()
                    }

                    storage.child(deleteProfilePicture).child(delete_icon).downloadUrl.addOnCompleteListener { downloadTask ->
                        database.child(auth.uid!!).child(userInformation).setValue(User(userId = auth.uid, customProfilePictureUri = downloadTask.result.toString())).addOnCompleteListener {
                            auth.currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    Toast.makeText(context, context.getString(R.string.account_was_deleted), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.delete_account_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.new_email_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    fun getUserEmail(): String {
        return auth.currentUser?.email!!
    }
}