package com.example.scorpiochat.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scorpiochat.*
import com.example.scorpiochat.data.AuthenticationState
import com.example.scorpiochat.data.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import androidx.lifecycle.map

class MainActivityViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val database = FirebaseDatabase.getInstance().reference
    var userInfo: MutableLiveData<User> = MutableLiveData<User>()

    val authenticationState = FirebaseUserLiveData().map {
        if (auth.currentUser != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }


    fun loadUserInfo() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dbChild in snapshot.children) {
                    val user = dbChild.child(userInformation).getValue(User::class.java)
                    if (user?.userId == auth.uid && user!=null) {
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

    fun getUserEmail(): String? {
        return auth.currentUser?.email
    }

    fun changeUserStatus(isOnline: Boolean) {
        val update = if (isOnline) {
            mapOf("online" to isOnline)
        } else {
            mapOf("online" to isOnline, "lastSeen" to System.currentTimeMillis())
        }
        if(auth.uid != null) {
            database.child(auth.uid!!).child(userInformation).updateChildren(update)
        }
    }

}