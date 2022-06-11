package com.example.scorpiochat.data

class Message(
    val text: String? = null,
    val time: Long? = null,
    val recipientId: String? = null,
    val seen: Boolean? = null,
    val edited: Boolean? = null,
    val repliedTo: Long? = null,
    val forwarded: String? = null
)