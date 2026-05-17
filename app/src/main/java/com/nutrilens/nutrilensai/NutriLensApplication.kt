package com.nutrilens.nutrilensai

import android.app.Application
import timber.log.Timber

class NutriLensApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
