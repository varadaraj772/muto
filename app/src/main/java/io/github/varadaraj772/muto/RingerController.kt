package io.github.varadaraj772.muto

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.util.Log

class RingerController(ctx: Context) {
    private val am = ctx.getSystemService(AudioManager::class.java)
    private val nm = ctx.getSystemService(NotificationManager::class.java)

    fun apply(mode: String) {
        if (!nm.isNotificationPolicyAccessGranted) {
            Log.w("Muto", "Ringer policy access not granted!")
            return
        }

        val target = when (mode) {
            Scenario.SILENT -> AudioManager.RINGER_MODE_SILENT
            Scenario.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
            else -> AudioManager.RINGER_MODE_NORMAL
        }

        if (am.ringerMode != target) {
            try {
                am.ringerMode = target
                Log.d("Muto", "Successfully changed ringerMode to $mode ($target)")
            } catch (e: SecurityException) {
                Log.e("Muto", "Failed to change ringerMode", e)
            }
        }
    }

    fun release() {
        // No-op or restore to normal on service stop
    }
}