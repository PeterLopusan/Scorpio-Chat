package com.example.scorpiochat.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.*
import com.example.scorpiochat.*
import com.example.scorpiochat.data.Message
import com.example.scorpiochat.data.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.concurrent.TimeUnit

class ChatsViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference
    var listOfUsersAndMessages: MutableLiveData<MutableList<Triple<User, Message, Int>>> = MutableLiveData<MutableList<Triple<User, Message, Int>>>()


    init {
        listOfUsersAndMessages.value = mutableListOf()
    }


    fun loadKeysAndMessages() {
        listOfUsersAndMessages.value?.clear()
        database.child(auth.uid!!).child(conversations).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (conversationChild in snapshot.children) {
                    val key = conversationChild.key
                    if (key != null) {
                        database.child(auth.uid!!).child(conversations).child(key).addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var message: Message? = null
                                var newMessageCount = 0
                                for (messageChild in snapshot.children) {
                                    message = messageChild.getValue(Message::class.java)
                                    if (message?.seen == false && message.recipientId == getMyId()) {
                                        newMessageCount++
                                    }
                                }
                                loadUsers(Triple(key, message, newMessageCount))
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.d("TAG", error.toString())
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }

        })
    }


    private fun loadUsers(data: Triple<String, Message?, Int>) {
        if (data.second != null) {
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var match = false
                    for (dbChild in snapshot.children) {
                        val user = dbChild.child(userInformation).getValue(User::class.java)

                        if (user?.username != null && dbChild.key == data.first) {
                            match = true
                            var duplicate = false
                            var itemForRemove: Triple<User, Message, Int>? = null
                            for (item in listOfUsersAndMessages.value!!) {

                                if (item.first.userId == user.userId) {
                                    if (item.first.online == user.online && item.second.time == data.second!!.time && item.second.seen == data.second!!.seen) {
                                        duplicate = true
                                    } else {
                                        itemForRemove = item
                                    }
                                }
                            }
                            listOfUsersAndMessages.value?.remove(itemForRemove)
                            if (!duplicate) {
                                listOfUsersAndMessages.value?.add(Triple(user, data.second!!, data.third))
                            }
                        }
                    }
                    if (!match) {
                        var duplicate = false
                        for (item in listOfUsersAndMessages.value!!) {
                            if (item.first.userId == data.first) {
                                duplicate = true
                            }
                        }
                        if (!duplicate) {
                            storage.child(deleteProfilePicture).child(delete_icon).downloadUrl.addOnCompleteListener { task ->
                                listOfUsersAndMessages.value?.add(Triple(User(userId = data.first, customProfilePictureUri = task.result.toString()), data.second!!, data.third))
                                listOfUsersAndMessages.value = listOfUsersAndMessages.value
                            }
                        }
                    }
                    listOfUsersAndMessages.value = listOfUsersAndMessages.value
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("TAG", error.toString())
                }
            })
        }
    }

    fun deleteConversation(userId: String) {
        database.child(auth.uid!!).child(conversations).child(userId).removeValue().addOnCompleteListener {
            loadKeysAndMessages()
        }
    }

    fun muteConversation(context: Context, userId: String, duration: Long? = null) {
        SharedPreferencesManager.setIfUserIsMuted(context, true, userId)
        if (duration != null) {
            val data = Data.Builder()
            data.putString("userId", userId)
            val request: OneTimeWorkRequest =
                OneTimeWorkRequest.Builder(UnmuteUserWorker::class.java)
                    .setInitialDelay(duration, TimeUnit.MINUTES)
                    .setInputData(data.build())
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(userId, ExistingWorkPolicy.REPLACE, request)
        }
        loadKeysAndMessages()
    }

    fun unmuteConversation(context: Context, userId: String) {
        SharedPreferencesManager.setIfUserIsMuted(context, false, userId)
        WorkManager.getInstance(context).cancelUniqueWork(userId)
        loadKeysAndMessages()
    }

    fun getMyId(): String {
        return auth.uid!!
    }
}




