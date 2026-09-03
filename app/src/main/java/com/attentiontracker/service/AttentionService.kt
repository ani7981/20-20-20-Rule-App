package com.attentiontracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
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
import com.attentiontracker.camera.FaceAnalyzer
import com.attentiontracker.overlay.BreakOverlayManager
import com.attentiontracker.util.PreferenceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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

        // Intent actions
        const val ACTION_UPDATE_THRESHOLD = "com.attentiontracker.UPDATE_THRESHOLD"
        const val ACTION_STATUS_UPDATE    = "com.attentiontracker.STATUS_UPDATE"

        // Intent extras
        const val EXTRA_THRESHOLD      = "threshold_seconds"
        const val EXTRA_IS_LOOKING     = "is_looking"
        const val EXTRA_SECONDS_LOOKING = "seconds_looking"
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

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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
        startForeground(NOTIFICATION_ID, buildNotification("Tracking attention..."))
        startCamera()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        cameraExecutor.shutdown()
        overlayManager.hide()
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
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ── Face result handler ───────────────────────────────────────────────────

    /**
     * Called from the camera executor thread every time a frame is processed.
     * Must not touch Views directly; broadcasts are thread-safe.
     */
    private fun onFaceResult(isFacing: Boolean) {
        // While the break overlay is visible, ignore face changes so the timer
        // doesn't restart behind the overlay before the user dismisses it.
        if (breakActive) return

        if (isFacing) {
            if (lookingStartTime == 0L) {
                lookingStartTime = System.currentTimeMillis()
                startCountUpTimer()
            }
            // Elapsed broadcast is sent by the timer loop, not here
        } else {
            resetTimer()
            broadcastStatus(isLooking = false, elapsed = 0L)
        }
    }

    // ── Timer logic ───────────────────────────────────────────────────────────

    /**
     * Counts up every second while the user is looking at the screen.
     * Triggers [triggerBreak] when [thresholdMs] is reached.
     */
    private fun startCountUpTimer() {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            while (true) {
                delay(1_000L)
                val elapsed = System.currentTimeMillis() - lookingStartTime
                broadcastStatus(isLooking = true, elapsed = elapsed / 1_000L)
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

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AttentionTracker")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
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
