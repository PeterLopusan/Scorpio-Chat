package com.example.scorpiochat.viewModels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scorpiochat.data.User
import com.example.scorpiochat.data.userInformation
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class AddContactViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val database = FirebaseDatabase.getInstance().reference
    val userList: MutableLiveData<MutableList<User>> = MutableLiveData<MutableList<User>>()

    init {
        userList.value = mutableListOf()
    }

    fun findUsers(searchedText: String) {
        userList.value?.clear()
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dbChild in snapshot.children) {
                    val user = dbChild.child(userInformation).getValue(User::class.java)
                    if (user?.username != null) {
                        if (user.username.contains(searchedText, ignoreCase = true) && user.userId != auth.currentUser?.uid) {
                            if (user.blockedUsers != null) {
                                if (!user.blockedUsers!!.contains(auth.uid!!)) {
                                    userList.value?.add(user)
                                }
                            } else {
                                userList.value?.add(user)
                            }
                        }
                    }
                }
                userList.value = userList.value
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }
        })
    }
}