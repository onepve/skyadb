package com.sky22333.skyadb

import android.app.Application
import timber.log.Timber

class AdbManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AppServices.initialize(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
