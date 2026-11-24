package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.ServiceLocator
import com.example.data.network.ApiClient

class MainApplication : Application() {
    companion object {
        lateinit var instance: MainApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d("MainApplication", "Iniciando MainApplication...")
        
        // Inicialización global explícita de ApiClient y ServiceLocator
        ApiClient.init(this)
        ServiceLocator.init(this)
    }
}
