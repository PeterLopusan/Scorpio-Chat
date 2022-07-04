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

class BlockedUsersViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val database = FirebaseDatabase.getInstance().reference
    val myUserInfo: MutableLiveData<User> = MutableLiveData<User>()
    val listOfBlockedUsers: MutableLiveData<MutableList<User>> = MutableLiveData<MutableList<User>>()

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

    fun loadBlockedUsers(list: MutableList<String>): MutableLiveData<MutableList<User>> {
        listOfBlockedUsers.value = mutableListOf()
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dbChild in snapshot.children) {
                    val user = dbChild.child(userInformation).getValue(User::class.java)
                    for (blockedUser in list) {
                        if (user?.userId == blockedUser) {
                            var duplicate = false
                            for (item in listOfBlockedUsers.value!!) {
                                if (item.userId == user.userId) {
                                    duplicate = true
                                }
                            }
                            if (!duplicate) {
                                listOfBlockedUsers.value?.add(user)
                            }
                        }
                    }
                }
                listOfBlockedUsers.value = listOfBlockedUsers.value
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.toString())
            }
        })
        return listOfBlockedUsers
    }

    fun unBlockUser(blockedUserId: String) {
        val blockedUsers = myUserInfo.value?.blockedUsers
        blockedUsers?.remove(blockedUserId)
        val update = mapOf("blockedUsers" to blockedUsers)
        database.child(auth.uid!!).child(userInformation).updateChildren(update)
        listOfBlockedUsers.value?.removeIf { it.userId == blockedUserId }
        listOfBlockedUsers.value = listOfBlockedUsers.value
    }

    fun getMyId(): String {
        return auth.uid!!
    }
}