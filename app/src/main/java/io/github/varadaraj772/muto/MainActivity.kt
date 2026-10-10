package io.github.varadaraj772.muto

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Date

class MainActivity : Activity() {

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_PAUSED_UNTIL = "paused_until"
        const val KEY_DEBUG = "debug_state"
        const val PAUSE_MS = 60 * 60 * 1000L
        const val REQ_POST_NOTIF = 1
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var notificationManager: NotificationManager
    private lateinit var powerManager: PowerManager

    private lateinit var enableSwitch: Switch
    private lateinit var stateView: TextView
    private lateinit var policyButton: Button
    private lateinit var batteryButton: Button
    private lateinit var pauseButton: Button
    private lateinit var oemHint: TextView

    private var updatingUi = false

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
        if (key == KEY_DEBUG) stateView.text = p.getString(KEY_DEBUG, null) ?: IDLE_TEXT
    }

    private val IDLE_TEXT = "Waiting for first detection…"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(Scenario.PREFS, Context.MODE_PRIVATE)
        notificationManager = getSystemService(NotificationManager::class.java)
        powerManager = getSystemService(PowerManager::class.java)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(
                bars.left + dp(20),
                bars.top + dp(24),
                bars.right + dp(20),
                bars.bottom + dp(24)
            )
            insets
        }

        root.addView(TextView(this).apply {
            text = "Muto"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        })

        enableSwitch = Switch(this).apply {
            text = "Enable Muto"
            textSize = 18f
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, checked ->
                if (updatingUi) return@setOnCheckedChangeListener
                if (checked) enableMuto() else disableMuto()
            }
        }
        root.addView(enableSwitch)

        root.addView(sectionLabel("Detected right now"))
        stateView = TextView(this).apply {
            text = IDLE_TEXT
            textSize = 15f
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(stateView)

        pauseButton = Button(this).apply {
            setOnClickListener { togglePause() }
        }
        root.addView(pauseButton)

        root.addView(sectionLabel("Scenarios"))
        root.addView(card("1. On Table — Screen OFF", "Resting flat or face-down, locked", Placement.TABLE, false))
        root.addView(card("2. On Table — Screen ON", "Resting flat, screen awake", Placement.TABLE, true))
        root.addView(card("3. In Hand — Screen OFF", "Held or carried, locked", Placement.HAND, false))
        root.addView(card("4. In Hand — Screen ON", "Actively using the phone", Placement.HAND, true))
        root.addView(card("5. In Pocket / Bag — Screen OFF", "Covered and dark, locked", Placement.POCKET, false))
        root.addView(card("6. In Pocket / Bag — Screen ON", "Covered and dark, accidental wake", Placement.POCKET, true))

        root.addView(sectionLabel("Setup"))

        policyButton = Button(this).apply {
            text = "Grant Ringer Policy Access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        }
        root.addView(policyButton)

        batteryButton = Button(this).apply {
            text = "Allow Background Running"
            setOnClickListener { requestBatteryExemption() }
        }
        root.addView(batteryButton)

        oemHint = TextView(this).apply {
            text = "On Xiaomi, Realme, Oppo, Vivo and similar phones, also set Autostart ON " +
                    "and Battery to \"No restrictions\" for Muto in system settings, " +
                    "otherwise the system may stop it."
            textSize = 12f
            alpha = 0.65f
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(oemHint)

        setContentView(scrollView)
    }

    override fun onStart() {
        super.onStart()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        stateView.text = prefs.getString(KEY_DEBUG, null) ?: IDLE_TEXT
    }

    override fun onStop() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        syncUi()
    }

    private fun enableMuto() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Grant Ringer Policy Access first", Toast.LENGTH_LONG).show()
            setSwitch(false)
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_POST_NOTIF)
        }

        prefs.edit().putBoolean(KEY_ENABLED, true).apply()
        ContextCompat.startForegroundService(this, Intent(this, MutoService::class.java))
        stateView.text = IDLE_TEXT
    }

    private fun disableMuto() {
        prefs.edit()
            .putBoolean(KEY_ENABLED, false)
            .remove(KEY_DEBUG)
            .apply()
        stopService(Intent(this, MutoService::class.java))
        stateView.text = "Muto is off"
    }

    private fun isPaused() = System.currentTimeMillis() < prefs.getLong(KEY_PAUSED_UNTIL, 0L)

    private fun togglePause() {
        if (isPaused()) {
            prefs.edit().remove(KEY_PAUSED_UNTIL).apply()
        } else {
            prefs.edit().putLong(KEY_PAUSED_UNTIL, System.currentTimeMillis() + PAUSE_MS).apply()
        }
        updatePauseButton()
    }

    private fun updatePauseButton() {
        pauseButton.text = if (isPaused()) {
            val until = prefs.getLong(KEY_PAUSED_UNTIL, 0L)
            "Paused until ${DateFormat.getTimeFormat(this).format(Date(until))} — tap to resume"
        } else {
            "Pause for 1 hour"
        }
        pauseButton.visibility = if (enableSwitch.isChecked) View.VISIBLE else View.GONE
    }

    private fun requestBatteryExemption() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun syncUi() {
        val policyGranted = notificationManager.isNotificationPolicyAccessGranted
        val batteryOk = powerManager.isIgnoringBatteryOptimizations(packageName)

        if (prefs.getBoolean(KEY_ENABLED, false) && !policyGranted) disableMuto()

        setSwitch(prefs.getBoolean(KEY_ENABLED, false) && policyGranted)

        policyButton.visibility = if (policyGranted) View.GONE else View.VISIBLE
        batteryButton.visibility = if (batteryOk) View.GONE else View.VISIBLE
        updatePauseButton()
    }

    private fun setSwitch(checked: Boolean) {
        updatingUi = true
        enableSwitch.isChecked = checked
        updatingUi = false
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        alpha = 0.65f
        setPadding(0, dp(20), 0, dp(8))
    }

    private fun card(
        titleText: String,
        descText: String,
        placement: Placement,
        screenOn: Boolean
    ): View {
        val prefKey = Scenario.key(placement, screenOn)
        val selected = prefs.getString(prefKey, Scenario.default(placement, screenOn))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(16))
        }

        card.addView(TextView(this).apply {
            text = titleText
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        })

        card.addView(TextView(this).apply {
            text = descText
            textSize = 12f
            alpha = 0.65f
            setPadding(0, dp(2), 0, dp(6))
        })

        val group = RadioGroup(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val modes = listOf(
            Scenario.SILENT to "Silent",
            Scenario.VIBRATE to "Vibrate",
            Scenario.RING to "Ring"
        )
        val buttons = modes.map { (value, label) ->
            RadioButton(this).apply {
                text = label
                textSize = 14f
                setPadding(dp(4), 0, dp(16), 0)
                tag = value
                group.addView(this)
            }
        }
        buttons.firstOrNull { it.tag == selected }?.isChecked = true

        group.setOnCheckedChangeListener { g, checkedId ->
            val value = g.findViewById<RadioButton>(checkedId)?.tag as? String ?: return@setOnCheckedChangeListener
            prefs.edit().putString(prefKey, value).apply()
        }

        card.addView(group)
        return card
    }
}