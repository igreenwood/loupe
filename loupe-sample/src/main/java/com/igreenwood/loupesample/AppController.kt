package com.igreenwood.loupesample

import android.app.Application
import com.chibatching.kotpref.Kotpref
import timber.log.Timber

class AppController: Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Kotpref.init(this)
    }
}