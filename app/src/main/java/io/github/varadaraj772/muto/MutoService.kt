package io.github.varadaraj772.muto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat

class MutoService : Service() {
    private lateinit var detector: ContextDetector
    private lateinit var ringer: RingerController
    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(Scenario.PREFS, MODE_PRIVATE)
        goForeground()
        ringer = RingerController(this)
        detector = ContextDetector(this) { placement, screenOn ->
            val mode = prefs.getString(
                Scenario.key(placement, screenOn),
                Scenario.default(placement, screenOn)
            )!!
            Log.d("Muto", "$placement screenOn=$screenOn -> $mode")
            prefs.edit().putString(
                "debug_state",
                "$placement / screen ${if (screenOn) "ON" else "OFF"} → $mode"
            ).apply()
            ringer.apply(mode)
        }
        detector.start()
    }

    private fun goForeground() {
        val ch = "muto_fg"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(ch, "Muto running", NotificationManager.IMPORTANCE_MIN)
        )
        val n = Notification.Builder(this, ch)
            .setContentTitle("Muto is active")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null

    override fun onDestroy() {
        detector.stop()
        ringer.release()
        super.onDestroy()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (c.getSharedPreferences(Scenario.PREFS, Context.MODE_PRIVATE).getBoolean("enabled", false)) {
            ContextCompat.startForegroundService(c, Intent(c, MutoService::class.java))
        }
    }
}