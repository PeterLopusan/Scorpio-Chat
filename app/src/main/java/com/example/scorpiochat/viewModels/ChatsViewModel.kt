package com.example.scorpiochat.viewModels

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.*
import com.example.scorpiochat.*
import com.example.scorpiochat.data.*
import com.example.scorpiochat.workers.UnmuteUserWorker
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class ChatsViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val database = FirebaseDatabase.getInstance().reference
    var listOfUsersAndMessages: MutableLiveData<MutableList<Triple<User, Message, Int>>> = MutableLiveData<MutableList<Triple<User, Message, Int>>>()
    val myUserInfo: MutableLiveData<User> = MutableLiveData<User>()

    init {
        listOfUsersAndMessages.value = mutableListOf()
    }

    fun loadMyUserInfo() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                for (dbChild in snapshot.children) {
                    val user = dbChild.child(userInformation).getValue(User::class.java)
                    if (user?.userId == auth.uid) {
                        myUserInfo.value = user
                        return
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }
        })
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
                    for (dbChild in snapshot.children) {
                        val user = dbChild.child(userInformation).getValue(User::class.java)

                        if (user != null && dbChild.key == data.first) {
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

    fun blockUser(blockedUserId: String, deleteConversation: Boolean) {
        var blockedUsers = myUserInfo.value?.blockedUsers
        if (blockedUsers == null) {
            blockedUsers = mutableListOf()
        }
        blockedUsers.add(blockedUserId)
        val update = mapOf("blockedUsers" to blockedUsers)
        database.child(getMyId()).child(userInformation).updateChildren(update)
        if (deleteConversation) {
            deleteConversation(blockedUserId)
        }
    }

    fun unblockUser(blockedUserId: String) {
        val blockedUsers = myUserInfo.value?.blockedUsers
        blockedUsers?.remove(blockedUserId)
        val update = mapOf("blockedUsers" to blockedUsers)
        database.child(getMyId()).child(userInformation).updateChildren(update)
    }

    fun getMyId(): String {
        return auth.uid!!
    }
}




