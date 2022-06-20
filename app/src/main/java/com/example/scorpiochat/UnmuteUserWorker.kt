package com.example.scorpiochat

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class UnmuteUserWorker(private val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val userId = inputData.getString("userId")
        if (userId != null) {
            SharedPreferencesManager.setIfUserIsMuted(context, false, userId)
        }
        return Result.success()
    }
}