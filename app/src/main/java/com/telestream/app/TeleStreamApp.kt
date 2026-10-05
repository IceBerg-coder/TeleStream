package com.telestream.app

import android.app.Application
import com.telestream.app.data.local.AppPreferences
import com.telestream.app.data.stream.LocalStreamProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TeleStreamApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var streamProxy: LocalStreamProxy
        private set
    lateinit var appPreferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        appPreferences = AppPreferences(this)
        
        // Start local zero-cost streaming proxy on device
        streamProxy = LocalStreamProxy(this, appPreferences)
        applicationScope.launch {
            streamProxy.start()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        streamProxy.stop()
    }

    companion object {
        lateinit var instance: TeleStreamApp
            private set
    }
}
