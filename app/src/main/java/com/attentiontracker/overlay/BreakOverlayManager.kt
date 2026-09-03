package com.attentiontracker.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Manages a full-screen system overlay drawn via [WindowManager] using the
 * [android.permission.SYSTEM_ALERT_WINDOW] permission.
 */
class BreakOverlayManager(
    private val context: Context,
    private val onBreakFinished: () -> Unit,
    private val onDismissed: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    private var _isVisible = false

    private var countdownValue = 20
    private var countdownTextView: TextView? = null

    private val countdownRunnable = object : Runnable {
        override fun run() {
            countdownValue--
            if (countdownValue > 0) {
                countdownTextView?.text = countdownValue.toString()
                mainHandler.postDelayed(this, 1000)
            } else {
                hide()
                onBreakFinished()
                onDismissed()
            }
        }
    }

    /** `true` while the overlay is on screen. */
    val isVisible: Boolean get() = _isVisible

    // ── Colours ───────────────────────────────────────────────────────────────
    private val bgColor      = Color.argb(250, 10, 25, 41)   // #0A1929, near-opaque
    private val accentColor  = Color.parseColor("#4FC3F7")    // light-blue
    private val textColor    = Color.WHITE
    private val btnTextColor = Color.parseColor("#0A1929")    // dark text on light btn

    // ── Public API ────────────────────────────────────────────────────────────

    /** Show the break overlay and vibrate. Thread-safe. */
    fun show(): Boolean = mainHandler.post {
        if (_isVisible) return@post
        countdownValue = 20
        val view = buildView()
        try {
            windowManager.addView(view, buildLayoutParams())
            overlayView = view
            _isVisible = true
            
            view.alpha = 0f
            view.animate().alpha(1f).setDuration(600).start()
            
            mainHandler.postDelayed(countdownRunnable, 1000)
            triggerVibration()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Hide the break overlay. Thread-safe. */
    fun hide(): Boolean = mainHandler.post {
        if (!_isVisible) return@post
        try {
            mainHandler.removeCallbacks(countdownRunnable)
            overlayView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            overlayView = null
            countdownTextView = null
            _isVisible = false
        }
    }

    // ── View construction ─────────────────────────────────────────────────────

    private fun buildView(): LinearLayout {
        // Root container — full screen, dark navy
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(bgColor)
            setPadding(72, 72, 72, 72)
        }

        // Headline: light-blue, bold
        root.addView(TextView(context).apply {
            text = "Time to rest your eyes"
            setTextColor(accentColor)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 0)
        })

        // Sub-message: white
        root.addView(TextView(context).apply {
            text = "Look at something 20 feet away"
            setTextColor(textColor)
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 56)
            setLineSpacing(lineSpacingExtra, 1.5f)
        })

        countdownTextView = TextView(context).apply {
            text = countdownValue.toString()
            setTextColor(accentColor)
            textSize = 64f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 56)
        }
        root.addView(countdownTextView)

        // Dismiss button: light-blue background, dark text
        root.addView(Button(context).apply {
            text = "Dismiss"
            setTextColor(btnTextColor)
            setBackgroundColor(accentColor)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(64, 32, 64, 32)
            setOnClickListener {
                hide()
                onDismissed()
            }
        })

        return root
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            // Intentionally NO FLAG_NOT_TOUCHABLE so the overlay intercepts taps
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    // ── Haptic feedback ───────────────────────────────────────────────────────

    /**
     * Three short bursts: 150ms on, 100ms off × 3.
     */
    private fun triggerVibration() {
        val pattern = longArrayOf(0, 150, 100, 150, 100, 150)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, -1)
            }
        }
    }
}
