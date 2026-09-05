package com.attentiontracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.attentiontracker.MainActivity
import com.attentiontracker.R
import com.attentiontracker.camera.FaceAnalyzer
import com.attentiontracker.overlay.BreakOverlayManager
import com.attentiontracker.util.PreferenceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Foreground service that owns the CameraX pipeline and drives the
 * attention-timer logic.
 *
 * Lifecycle:
 *  1. [onStartCommand] → start foreground, bind camera.
 *  2. [FaceAnalyzer] calls [onFaceResult] on every analysed frame.
 *  3. A coroutine timer counts up while the user is looking at the screen.
 *  4. When [thresholdMs] is exceeded → [BreakOverlayManager.show] + vibration.
 *  5. User dismisses → timer resets; service continues tracking.
 *
 * Broadcasts [ACTION_STATUS_UPDATE] with [EXTRA_IS_LOOKING] and
 * [EXTRA_SECONDS_LOOKING] so [MainActivity] can update its UI.
 */
class AttentionService : LifecycleService() {

    companion object {
        private const val TAG = "AttentionService"
        private const val CHANNEL_ID = "AttentionServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_ID_CAMERA = 2   // separate "camera in use" notification

        // Intent actions
        const val ACTION_UPDATE_THRESHOLD      = "com.attentiontracker.UPDATE_THRESHOLD"
        const val ACTION_STATUS_UPDATE         = "com.attentiontracker.STATUS_UPDATE"
        // Internal action fired by the camera notification's deleteIntent so we can re-post it immediately
        private const val ACTION_REPOST_CAMERA = "com.attentiontracker.REPOST_CAMERA_NOTIF"

        // Intent extras
        const val EXTRA_THRESHOLD      = "threshold_seconds"
        const val EXTRA_IS_LOOKING     = "is_looking"
        const val EXTRA_SECONDS_LOOKING = "seconds_looking"

        // Battery polling interval – hard fallback; live receiver drives normal updates
        private const val BATTERY_REFRESH_INTERVAL_MS = 30_000L
    }

    // ── Dependencies ──────────────────────────────────────────────────────────
    private lateinit var overlayManager: BreakOverlayManager
    private lateinit var prefManager: PreferenceManager
    private lateinit var cameraExecutor: ExecutorService

    // ── Timer state ────────────────────────────────────────────────────────────
    private var lookingStartTime = 0L   // epoch ms; 0 = not currently looking
    private var timerJob: Job? = null
    private var thresholdMs = PreferenceManager.DEFAULT_THRESHOLD_SECONDS * 1_000L
    private var breakActive = false     // true while the overlay is on screen

    // ── Battery / thermal session state ────────────────────────────────────────
    /** Battery % recorded when the service started tracking (used to compute session drain). */
    private var sessionStartBatteryPct: Float = -1f
    /** Epoch ms when the current tracking session began. */
    private var sessionStartTimeMs: Long = 0L
    /** Last time (epoch ms) we refreshed the battery stats cache. */
    private var lastBatteryRefreshMs: Long = 0L
    /** Cached one-line summary string shown in the collapsed notification. */
    private var cachedBatterySummary: String = ""
    /** Last status text passed to [updateNotification]; replayed by the battery receiver. */
    private var currentStatusText: String = "Tracking attention..."

    // ── Live broadcast receivers ───────────────────────────────────────────────

    /**
     * Fires whenever the battery level actually changes (Android posts this automatically).
     * Resets the cache timer so [refreshBatteryCacheIfStale] re-reads all values immediately,
     * then re-anchors the foreground notification via [startForeground] so it reappears
     * even if the user swiped it away on Android 13+.
     */
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lastBatteryRefreshMs = 0L   // force full cache refresh on next buildNotification()
            startForeground(NOTIFICATION_ID, buildNotification(currentStatusText))
        }
    }

    /**
     * Fires when the user swipes away the camera notification.
     * Immediately re-posts [NOTIFICATION_ID_CAMERA] so it stays visible.
     */
    private val cameraNotifDeleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_REPOST_CAMERA) showCameraNotification()
        }
    }


    override fun onCreate() {
        super.onCreate()
        prefManager = PreferenceManager(this)
        overlayManager = BreakOverlayManager(
            this,
            onBreakFinished = { lifecycleScope.launch { prefManager.incrementCompletedBreaks() } },
            onDismissed = ::onBreakDismissed
        )
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Load the persisted threshold once at startup
        lifecycleScope.launch {
            thresholdMs = prefManager.thresholdSeconds.first() * 1_000L
        }
    }

    /** Guards against registering receivers twice on repeated [onStartCommand] calls. */
    private var receiversRegistered = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Hot-update the threshold from MainActivity without restarting the service
        if (intent?.action == ACTION_UPDATE_THRESHOLD) {
            val secs = intent.getLongExtra(EXTRA_THRESHOLD, PreferenceManager.DEFAULT_THRESHOLD_SECONDS)
            thresholdMs = secs * 1_000L
            Log.d(TAG, "Threshold updated to ${secs}s")
            return START_STICKY
        }

        createNotificationChannel()
        // Record where the battery is when this tracking session begins
        sessionStartBatteryPct = getBatteryLevelPct()
        sessionStartTimeMs = System.currentTimeMillis()
        // startForeground FIRST — receivers registered below may call startForeground() themselves
        startForeground(NOTIFICATION_ID, buildNotification("Tracking attention..."))

        // Register live receivers AFTER startForeground() so batteryReceiver can safely call
        // startForeground() in its onReceive() without hitting ForegroundServiceDidNotStartInTimeException
        if (!receiversRegistered) {
            receiversRegistered = true
            // ACTION_BATTERY_CHANGED is a system broadcast; RECEIVER_NOT_EXPORTED is semantically correct
            // (no external app should be allowed to spoof a battery-changed event to us)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_NOT_EXPORTED)
                registerReceiver(cameraNotifDeleteReceiver, IntentFilter(ACTION_REPOST_CAMERA), RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                registerReceiver(cameraNotifDeleteReceiver, IntentFilter(ACTION_REPOST_CAMERA))
            }
        }

        startCamera()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        cameraExecutor.shutdown()
        overlayManager.hide()
        // Clean up receivers and secondary notification
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(cameraNotifDeleteReceiver) } catch (_: Exception) {}
        hideCameraNotification()
    }

    // ── Camera setup ──────────────────────────────────────────────────────────

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(cameraExecutor, FaceAnalyzer(::onFaceResult)) }

                provider.unbindAll()
                provider.bindToLifecycle(
                    this,                                    // LifecycleService = LifecycleOwner
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    analysis
                )
                Log.d(TAG, "Camera bound successfully")
                showCameraNotification()   // let the user know camera is actively in use
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed: ${e.message}", e)
                hideCameraNotification()
            }
        }, ContextCompat.getMainExecutor(this))
    }


    // ── Face result handler ───────────────────────────────────────────────────

    private var isCurrentlyFacing = false
    private var lookAwayStartTime = 0L

    /**
     * Called from the camera executor thread every time a frame is processed.
     * Must not touch Views directly; broadcasts are thread-safe.
     */
    private fun onFaceResult(isFacing: Boolean) {
        // While the break overlay is visible, ignore face changes so the timer
        // doesn't restart behind the overlay before the user dismisses it.
        if (breakActive) return

        isCurrentlyFacing = isFacing

        if (isFacing) {
            lookAwayStartTime = 0L
            if (lookingStartTime == 0L) {
                lookingStartTime = System.currentTimeMillis()
                startCountUpTimer()
            }
        } else {
            if (lookingStartTime != 0L) {
                if (lookAwayStartTime == 0L) {
                    lookAwayStartTime = System.currentTimeMillis()
                } else if (System.currentTimeMillis() - lookAwayStartTime > 5000L) {
                    // Looked away for more than 5 seconds -> reset session
                    resetTimer()
                    broadcastStatus(isLooking = false, elapsed = 0L)
                }
            }
        }
    }

    // ── Timer logic ───────────────────────────────────────────────────────────

    /**
     * Counts up every second while the user is looking at the screen (or within the grace period).
     * Triggers [triggerBreak] when [thresholdMs] is reached.
     */
    private fun startCountUpTimer() {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            while (true) {
                delay(1_000L)
                val elapsed = System.currentTimeMillis() - lookingStartTime
                broadcastStatus(isLooking = isCurrentlyFacing, elapsed = elapsed / 1_000L)
                if (elapsed >= thresholdMs) {
                    triggerBreak()
                    break
                }
            }
        }
    }

    private fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        lookingStartTime = 0L
        lookAwayStartTime = 0L
    }

    private fun triggerBreak() {
        breakActive = true
        overlayManager.show()
        updateNotification("Time for a break — look away from the screen.")
        Log.i(TAG, "Break triggered after ${thresholdMs / 1_000}s")
    }

    private fun onBreakDismissed() {
        breakActive = false
        resetTimer()
        updateNotification("Tracking attention...")
        Log.i(TAG, "Break dismissed — timer reset")
    }

    // ── Broadcast helpers ─────────────────────────────────────────────────────

    private fun broadcastStatus(isLooking: Boolean, elapsed: Long) {
        sendBroadcast(Intent(ACTION_STATUS_UPDATE).apply {
            putExtra(EXTRA_IS_LOOKING, isLooking)
            putExtra(EXTRA_SECONDS_LOOKING, elapsed)
            `package` = packageName   // restrict to this app only
        })
    }

    // ── Notification helpers ──────────────────────────────────────────────────

    /**
     * Queries current battery level as a percentage using the sticky battery broadcast.
     * This is a direct cached intent lookup – no event listener, no thread overhead.
     */
    private fun getBatteryLevelPct(): Float {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return -1f
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        return (level / scale.toFloat()) * 100f
    }

    /**
     * Returns battery/device temperature in °C.
     * BatteryManager reports in tenths of a degree (e.g. 345 = 34.5°C).
     */
    private fun getTemperatureCelsius(): Float {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return tenths / 10.0f
    }

    /**
     * Returns instantaneous current draw in mA from the hardware fuel-gauge.
     * Returns 0 if the device doesn't expose this property.
     * Value is signed: negative = discharging. We return the absolute value.
     */
    private fun getInstantCurrentMa(): Int {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val microAmps = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        return if (microAmps == Int.MIN_VALUE) 0 else abs(microAmps) / 1000
    }

    /**
     * Refreshes [cachedBatterySummary] at most once every [BATTERY_REFRESH_INTERVAL_MS].
     * Called from [buildNotification] so the heavy sticky-intent query only runs
     * every 30 s, not on every camera frame or timer tick.
     */
    private fun refreshBatteryCacheIfStale() {
        val now = System.currentTimeMillis()
        if (now - lastBatteryRefreshMs < BATTERY_REFRESH_INTERVAL_MS && cachedBatterySummary.isNotEmpty()) return

        lastBatteryRefreshMs = now

        val currentPct   = getBatteryLevelPct()
        val tempC        = getTemperatureCelsius()
        val mA           = getInstantCurrentMa()

        // Session drain = how many % have been used since tracking started
        val drainPct     = if (sessionStartBatteryPct >= 0f) (sessionStartBatteryPct - currentPct).coerceAtLeast(0f) else 0f

        // Drain rate = % per hour based on elapsed session time
        val elapsedHours = (now - sessionStartTimeMs) / 3_600_000.0
        val ratePctPerHr = if (elapsedHours > 0.01) drainPct / elapsedHours else 0.0

        // One-line summary for collapsed view
        cachedBatterySummary = buildString {
            if (currentPct >= 0f) append("%.0f%%".format(currentPct))
            if (mA > 0)        append(if (isEmpty()) "${mA}mA" else " · ${mA}mA")
            if (drainPct > 0f) append(" · -%.1f%%".format(drainPct))
            if (tempC > 0f)    append(" · %.1f°C".format(tempC))
        }

        // Store individual values for expanded view
        batteryCurrentPct  = currentPct
        batteryTempC       = tempC
        batteryMa          = mA
        batteryDrainPct    = drainPct
        batteryRatePctPerHr = ratePctPerHr
    }

    // Cached per-field values written by refreshBatteryCacheIfStale()
    private var batteryCurrentPct: Float   = -1f
    private var batteryTempC: Float        = 0f
    private var batteryMa: Int             = 0
    private var batteryDrainPct: Float     = 0f
    private var batteryRatePctPerHr: Double = 0.0

    private fun buildNotification(statusText: String): Notification {
        refreshBatteryCacheIfStale()

        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // Collapsed line: status + battery summary
        val collapsedText = if (cachedBatterySummary.isNotEmpty())
            "$statusText  ·  $cachedBatterySummary"
        else
            statusText

        // Expanded multi-line view
        val expandedText = buildString {
            appendLine(statusText)
            if (batteryCurrentPct >= 0f) appendLine("🔋 Battery: %.0f%%".format(batteryCurrentPct))
            if (batteryMa > 0)           appendLine("⚡ Current draw: ${batteryMa} mA")
            if (batteryDrainPct > 0f)    appendLine("📉 Session drain: -%.1f%%  (%.1f%%/hr)".format(batteryDrainPct, batteryRatePctPerHr))
            if (batteryTempC > 0f)       appendLine("🌡 Temperature: %.1f°C".format(batteryTempC))
        }.trimEnd()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("20-20-20 Rule")
            .setContentText(collapsedText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    /**
     * Updates the foreground notification with new status text.
     * Uses [startForeground] (not just [NotificationManager.notify]) so the notification
     * is re-anchored to the foreground service — this makes it reappear immediately even
     * if the user swiped it away on Android 13+.
     */
    private fun updateNotification(text: String) {
        currentStatusText = text
        startForeground(NOTIFICATION_ID, buildNotification(text))
    }

    // ── Camera in-use notification ────────────────────────────────────────────

    /**
     * Posts a secondary persistent notification (ID [NOTIFICATION_ID_CAMERA]) indicating
     * the front camera is actively in use. Uses a [deleteIntent] so if the user swipes it
     * the notification immediately re-posts itself via [cameraNotifDeleteReceiver].
     */
    private fun showCameraNotification() {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        // When swiped, fire ACTION_REPOST_CAMERA so the receiver re-posts it
        val deletePi = PendingIntent.getBroadcast(
            this, 0,
            Intent(ACTION_REPOST_CAMERA).apply { `package` = packageName },
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📷 Camera in use")
            .setContentText("20-20-20 Rule is using the front camera to detect screen attention")
            .setSmallIcon(R.drawable.ic_camera)
            .setContentIntent(tapIntent)
            .setDeleteIntent(deletePi)   // re-post on swipe
            .setOngoing(false)           // OS won't block the swipe; deleteIntent handles persistence
            .setSilent(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID_CAMERA, notification)
    }

    private fun hideCameraNotification() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIFICATION_ID_CAMERA)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Attention Tracker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent screen-time tracker notification"
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}
