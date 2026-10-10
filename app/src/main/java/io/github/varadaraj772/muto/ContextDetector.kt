package io.github.varadaraj772.muto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

class ContextDetector(
    private val ctx: Context,
    private val onChange: (Placement, Boolean) -> Unit
) : SensorEventListener {

    private val sm = ctx.getSystemService(SensorManager::class.java)
    private val pm = ctx.getSystemService(PowerManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val gravity = FloatArray(3)
    private var haveGravity = false
    private var motion = 0f
    private var flat = false
    private var covered = false
    private var maxProx = 5f

    private var placement = Placement.HAND
    private var screenOn = false
    private var pending: Placement? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = updateScreen()
    }

    fun start() {
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let {
            maxProx = it.maximumRange
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        val f = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(ctx, screenReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)
        updateScreen()
    }

    fun stop() {
        sm.unregisterListener(this)
        runCatching { ctx.unregisterReceiver(screenReceiver) }
        handler.removeCallbacksAndMessages(null)
    }

    private fun updateScreen() {
        // True hardware display state (independent of lock screen)
        val on = pm.isInteractive
        if (on != screenOn) {
            screenOn = on
            onChange(placement, screenOn)
        }
    }

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> handleAccel(e.values)
            Sensor.TYPE_PROXIMITY -> covered = e.values[0] < minOf(maxProx, 5f)
        }
        evaluate()
    }

    private fun handleAccel(v: FloatArray) {
        val a = 0.8f
        if (!haveGravity) {
            v.copyInto(gravity)
            haveGravity = true
        }
        for (i in 0..2) gravity[i] = a * gravity[i] + (1 - a) * v[i]

        val dx = v[0] - gravity[0]
        val dy = v[1] - gravity[1]
        val dz = v[2] - gravity[2]
        motion = 0.85f * motion + 0.15f * sqrt(dx * dx + dy * dy + dz * dz)

        val g = sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2])
        if (g < 1f) return
        val tilt = Math.toDegrees(acos((abs(gravity[2]) / g).coerceIn(0f, 1f).toDouble()))

        // Calibrated thresholds: noise-tolerant (motion < 1.2f instead of 0.25f)
        flat = if (flat) (tilt < 22 && motion < 1.5f) else (tilt < 14 && motion < 1.0f)
    }

    private fun evaluate() {
        val faceUp = gravity[2] > 0
        val candidate = when {
            covered && flat && !faceUp -> Placement.TABLE   // Face-down flat on table
            covered                    -> Placement.POCKET  // Proximity blocked in pocket/bag
            flat                       -> Placement.TABLE   // Resting flat face-up
            else                       -> Placement.HAND    // Tilted or moving in hand
        }

        if (candidate == placement) {
            pending = null
            handler.removeCallbacks(commit)
            return
        }
        if (candidate == pending) return

        pending = candidate
        handler.removeCallbacks(commit)
        // 400ms debounce ensures rapid responsive transitions without jitter cancel
        handler.postDelayed(commit, 400L)
    }

    private val commit = Runnable {
        pending?.let {
            placement = it
            pending = null
            onChange(placement, screenOn)
        }
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}