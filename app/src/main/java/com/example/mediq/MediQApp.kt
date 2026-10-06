package com.example.mediq

import android.app.Application
import com.example.mediq.di.AppContainer

/**
 * Custom Application class required to initialise [AppContainer] with a
 * Context before any ViewModel or repository is created.
 *
 * Registered in AndroidManifest.xml as android:name=".MediQApp".
 */
class MediQApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
