package com.altair.app

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SignalService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null
}