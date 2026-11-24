package com.example.data.local

import android.content.Context
import com.example.data.repository.AppRepository
import com.example.data.network.PocketBaseClient

object ServiceLocator {
    var repository: AppRepository? = null
        private set

    fun init(context: Context) {
        if (repository == null) {
            PocketBaseClient.init(context)
            repository = AppRepository(context)
        }
    }
}
