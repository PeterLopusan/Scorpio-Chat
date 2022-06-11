package com.example.scorpiochat

import android.content.Context

fun getTime(time: Long, context: Context): String {
    return android.text.format.DateFormat.getTimeFormat(context).format(time)
}

fun getDate(time: Long, context: Context): String {
    return android.text.format.DateFormat.getDateFormat(context).format(time)
}

