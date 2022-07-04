package com.example.scorpiochat.data

class User(
    val username: String? = null,
    val userId: String? = null,
    var online: Boolean? = null,
    var lastSeen: Long? = null,
    var customProfilePictureUri: String? = null,
    var blockedUsers: MutableList<String>? = null
)
