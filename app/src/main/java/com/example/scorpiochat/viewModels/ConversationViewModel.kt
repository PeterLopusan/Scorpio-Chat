package com.example.scorpiochat.viewModels

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.scorpiochat.*
import com.example.scorpiochat.data.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONException
import org.json.JSONObject

private const val url = "https://fcm.googleapis.com/fcm/send"
private const val contentType = "application/json"
private const val topicTemplate = "/topics/"

class ConversationViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = Firebase.auth
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference
    var messagesList: MutableLiveData<MutableList<Pair<Message, Message?>>> = MutableLiveData<MutableList<Pair<Message, Message?>>>()
    val conversationUserInfo: MutableLiveData<User> = MutableLiveData<User>()

    val myUserInfo: MutableLiveData<User> = MutableLiveData<User>()

    init {
        messagesList.value = mutableListOf()
        loadMyUserInfo()
    }

    private fun loadMyUserInfo() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                for (dbChild in snapshot.children) {
                    val user = dbChild.child(userInformation).getValue(User::class.java)
                    if (user?.userId == getMyId()) {
                        myUserInfo.value = user
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }
        })
    }

    fun sendMessage(message: Message) {
        val recipientId = message.recipientId!!
        val key = message.time.toString()

        prepareNotification(message)
        database.apply {
            if (getMyId() != null) {
                child(getMyId()!!).child(conversations).child(recipientId).child(key).setValue(message)
                child(recipientId).child(conversations).child(getMyId()!!).child(key).setValue(message)
            }
        }
    }

    fun loadMessage(chatId: String) {
        messagesList.value?.clear()
        getMyId()?.let {
            database.child(it).child(conversations).child(chatId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    for (dbChild in snapshot.children) {
                        val message = dbChild.getValue(Message::class.java)
                        var duplicate = false
                        var itemForRemove: Pair<Message, Message?>? = null

                        if (message != null) {
                            for (item in messagesList.value!!) {
                                if (item.first.time == message.time) {
                                    if (item.first.seen == message.seen) {
                                        duplicate = true
                                    } else {
                                        itemForRemove = item
                                    }
                                }
                            }
                            messagesList.value?.remove(itemForRemove)
                            if (!duplicate) {
                                var repliedToMessage: Message? = null
                                if (message.repliedTo != null) {
                                    for (item in messagesList.value!!) {
                                        if (item.first.time == message.repliedTo) {
                                            repliedToMessage = item.first
                                        }
                                    }
                                }

                                messagesList.value?.add(Pair(message, repliedToMessage))
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
    }


    fun loadUserInfo(userId: String) {

        database.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NullSafeMutableLiveData")
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dbChild in snapshot.children) {
                    val user = dbChild.child(userInformation).getValue(User::class.java)
                    if (user?.userId == userId) {
                        conversationUserInfo.value = user
                        conversationUserInfo.value = conversationUserInfo.value
                        return
                    }
                }
                storage.child(deleteProfilePicture).child(delete_icon).downloadUrl.addOnCompleteListener { task ->
                    conversationUserInfo.value = User(customProfilePictureUri = task.result.toString())
                }
                conversationUserInfo.value = conversationUserInfo.value
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }
        })
    }

    fun clearUserInfo() {
        conversationUserInfo.value = null
    }

    fun getMyId(): String? {
        return auth.uid
    }

    private fun prepareNotification(message: Message, actionWithMessage: String? = null) {
        val topic = topicTemplate + message.recipientId
        val notification = JSONObject()
        val notificationBody = JSONObject()
        try {
            notificationBody.put("title", myUserInfo.value?.username)
            notificationBody.put("message", message.text)
            notificationBody.put("senderId", auth.uid)
            notificationBody.put("time", message.time)
            notificationBody.put("actionWithMessage", actionWithMessage)
            notification.put("to", topic)
            notification.put("data", notificationBody)
        } catch (e: JSONException) {
            Log.e("TAG", "prepareNotification: " + e.message)
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


    fun editMessage(recipientId: String, editedText: String, message: Message) {
        val update = mapOf("text" to editedText, "edited" to true)
        val messageKey = message.time.toString()

        getMyId()?.let { myId ->
            database.child(recipientId).child(conversations).child(myId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.hasChild(messageKey)) {
                        getMyId()?.let { database.child(recipientId).child(conversations).child(it).child(messageKey).updateChildren(update) }
                        val newMessage = Message(text = editedText, message.time, message.recipientId, message.seen, message.edited, message.repliedTo)
                        prepareNotification(newMessage, editThisMessage)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("TAG", error.toString())
                }

            })
        }
        getMyId()?.let { database.child(it).child(conversations).child(recipientId).child(messageKey).updateChildren(update) }
    }

    fun deleteMessage(deleteAlsoForAnotherUser: Boolean?, message: Message) {
        val messageKey = message.time.toString()

        if (getMyId() != null) {
            if (deleteAlsoForAnotherUser == true) {
                database.child(conversationUserInfo.value?.userId!!).child(conversations).child(getMyId()!!).child(messageKey).removeValue()
                prepareNotification(message, deleteThisMessage)
            }
            database.child(getMyId()!!).child(conversations).child(conversationUserInfo.value?.userId!!).child(messageKey).removeValue()
        }
    }
}

