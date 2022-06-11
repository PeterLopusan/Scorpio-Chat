package com.example.scorpiochat.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
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
import com.google.firebase.storage.StorageReference
import org.json.JSONException
import org.json.JSONObject

private const val url = "https://fcm.googleapis.com/fcm/send"
private const val contentType = "application/json"
private const val topicTemplate = "/topics/"

class ConversationViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = Firebase.auth
    private val database = FirebaseDatabase.getInstance().reference
    var messagesList: MutableLiveData<MutableList<Triple<String, Message, Message?>>> = MutableLiveData<MutableList<Triple<String, Message, Message?>>>()
    var userInfo: MutableLiveData<User?> = MutableLiveData<User?>()

    private lateinit var myUsername: String

    init {
        messagesList.value = mutableListOf()
        getMyUsername()
    }

    private fun getMyUsername() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                for (dbChild in snapshot.children) {
                    val user = dbChild.child(userInformation).getValue(User::class.java)
                    if (user?.userId == getMyId()) {
                        myUsername = user.username!!
                    }
                }
                userInfo.value = userInfo.value
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }
        })
    }

    fun sendMessage(message: Message) {
        val recipientId = message.recipientId!!
        val key = database.push().key!!

        prepareNotification(message)
        database.apply {
            child(getMyId()).child(conversation).child(recipientId).child(allMessages).child(key).setValue(message)
            child(recipientId).child(conversation).child(getMyId()).child(allMessages).child(key).setValue(message)
        }
    }

    fun loadMessage(chatId: String) {
        messagesList.value?.clear()
        database.child(getMyId()).child(conversation).child(chatId).child(allMessages).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                for (dbChild in snapshot.children) {
                    val message = dbChild.getValue(Message::class.java)
                    var duplicate = false
                    var itemForRemove: Triple<String, Message, Message?>? = null

                    if (message != null) {
                        for (item in messagesList.value!!) {
                            if (item.second.time == message.time) {
                                if (item.second.seen == message.seen) {
                                    duplicate = true
                                } else {
                                    itemForRemove = item
                                }
                            }
                        }
                        messagesList.value?.remove(itemForRemove)
                        if (!duplicate) {
                            var repliedToMessage: Message? = null
                            if(message.repliedTo != null) {
                                for (i in messagesList.value!!) {
                                    if(i.second.time == message.repliedTo) {
                                        repliedToMessage = i.second
                                    }
                                }
                            }
                            messagesList.value?.add(Triple(dbChild.key!!, message, repliedToMessage))
                        }
                    }
                }
                messagesList.value = messagesList.value
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }
        })
    }


    fun loadUserInfo(userId: String) {
        userInfo.value = null
        database.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NullSafeMutableLiveData")
            override fun onDataChange(snapshot: DataSnapshot) {

                for (dbChild in snapshot.children) {
                    val user = dbChild.child(userInformation).getValue(User::class.java)
                    if (user?.userId == userId) {
                        userInfo.value = user
                    }
                }
                userInfo.value = userInfo.value
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }
        })
    }

    fun getMyId(): String {
        return auth.uid!!
    }

    private fun prepareNotification(message: Message, actionWithMessage: String? = null) {
        val topic = topicTemplate + message.recipientId
        val notification = JSONObject()
        val notificationBody = JSONObject()
        try {
            notificationBody.put("title", myUsername)
            notificationBody.put("message", message.text)
            notificationBody.put("senderId", auth.uid)
            notificationBody.put("time", message.time)
            notificationBody.put("actionWithMessage", actionWithMessage)
            notification.put("to", topic)
            notification.put("data", notificationBody)
        } catch (e: JSONException) {
            Log.e("TAG", "onCreate: " + e.message)
        }

        sendNotification(notification)
    }


    private fun sendNotification(notification: JSONObject) {
        val jsonObjectRequest = object : JsonObjectRequest(Method.POST, url, notification,
            Response.Listener { response ->
                Log.i("TAG", "onResponse: $response")
            },
            Response.ErrorListener {
                Log.i("TAG", "$it")
            }) {

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = serverKey
                params["Content-Type"] = contentType
                return params
            }

            override fun getBodyContentType(): String {
                return contentType
            }
        }
        val requestQueue = Volley.newRequestQueue(getApplication<Application?>().applicationContext)
        requestQueue.add(jsonObjectRequest)
    }


    fun editMessage(recipientId: String, editedText: String, messageKey: String, message: Message) {
        val update = mapOf("text" to editedText, "edited" to true)

        database.child(recipientId).child(conversation).child(getMyId()).child(allMessages).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(messageKey)) {
                    database.child(recipientId).child(conversation).child(getMyId()).child(allMessages).child(messageKey).updateChildren(update)
                    val newMessage = Message(text = editedText, message.time, message.recipientId, message.seen, message.edited, message.repliedTo)
                    prepareNotification(newMessage, editThisMessage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }

        })
        database.child(getMyId()).child(conversation).child(recipientId).child(allMessages).child(messageKey).updateChildren(update)
    }

    fun deleteMessage(messageKey: String, deleteAlsoForAnotherUser: Boolean?, message: Message) {
        if (deleteAlsoForAnotherUser == true) {
            database.child(userInfo.value?.userId!!).child(conversation).child(getMyId()).child(allMessages).child(messageKey).removeValue()
            prepareNotification(message, deleteThisMessage)
        }
        database.child(getMyId()).child(conversation).child(userInfo.value?.userId!!).child(allMessages).child(messageKey).removeValue()
    }


    fun getCurrentProfilePictureStorageRef(): StorageReference {
        val storageReference = if (userInfo.value?.customProfilePicture == true) {
            FirebaseStorage.getInstance().reference.child(userInfo.value?.userId!!).child(customProfilePicture)
        } else {
            FirebaseStorage.getInstance().reference.child(defaultProfilePicture).child(default_icon)
        }
        return storageReference
    }
}

