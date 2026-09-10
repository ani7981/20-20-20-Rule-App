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
 * Styled in the Optical Kinetic Neo-Brutalist design language.
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
                countdownTextView?.text = String.format("%02d", countdownValue)
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

    // ── Neo-Brutalist Colors ──────────────────────────────────────────────────
    private val bgColor       = Color.parseColor("#F9F9F9")   // Canvas
    private val blackColor    = Color.parseColor("#000000")   // Ink / Borders / Shadows
    private val inkColor      = Color.parseColor("#1B1B1B")   // Typography
    private val yellowColor   = Color.parseColor("#FFE600")   // Electric Yellow
    private val pinkColor     = Color.parseColor("#B31F56")   // Critical Override Cyber Pink
    private val mintContainer = Color.parseColor("#53FCA4")   // Optical Target Mint
    private val darkGreen     = Color.parseColor("#006D3F")   // Target Subtitle

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
            view.animate().alpha(1f).setDuration(400).start()

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
        // Root container — full screen, warm canvas
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(bgColor)
            setPadding(32, 48, 32, 48)
        }

        // 1. Warning Bar Strip at top
        val warningShadow = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val shadowBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(blackColor)
                cornerRadius = 24f
            }
            background = shadowBg
            setPadding(0, 0, 8, 8)
        }

        val warningBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val barBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(yellowColor)
                setStroke(8, blackColor)
                cornerRadius = 24f
            }
            background = barBg
            setPadding(28, 18, 28, 18)
        }

        val warningText = TextView(context).apply {
            text = "⚠ 20-MIN LIMIT REACHED"
            setTextColor(inkColor)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val emergencyBadge = TextView(context).apply {
            text = "RETINA EMERGENCY"
            setTextColor(inkColor)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            val badgeBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke(4, blackColor)
                cornerRadius = 999f
            }
            background = badgeBg
            setPadding(16, 6, 16, 6)
        }

        warningBar.addView(warningText)
        warningBar.addView(emergencyBadge)
        warningShadow.addView(warningBar)

        val warningParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 32
        }
        root.addView(warningShadow, warningParams)

        // 2. Main Instruction & Countdown Card with 6px hard shadow
        val cardWrapper = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val shadow = android.graphics.drawable.GradientDrawable().apply {
                setColor(blackColor)
                cornerRadius = 32f
            }
            background = shadow
            setPadding(0, 0, 14, 14)
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val cardBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke(10, blackColor)
                cornerRadius = 32f
            }
            background = cardBg
        }

        // Cyber Pink Top Bar
        val headerBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val hBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(pinkColor)
                cornerRadii = floatArrayOf(24f, 24f, 24f, 24f, 0f, 0f, 0f, 0f)
            }
            background = hBg
            setPadding(28, 16, 28, 16)
        }
        val headerTitle = TextView(context).apply {
            text = "CRITICAL OVERRIDE"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val ruleBadge = TextView(context).apply {
            text = "RULE 20·20·20"
            setTextColor(blackColor)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            val rBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(yellowColor)
                setStroke(4, blackColor)
                cornerRadius = 999f
            }
            background = rBg
            setPadding(16, 6, 16, 6)
        }
        headerBar.addView(headerTitle)
        headerBar.addView(ruleBadge)
        card.addView(headerBar)

        // Card Content
        val cardContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 32)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Title: REST YOUR EYES NOW! 👀
        cardContent.addView(TextView(context).apply {
            text = "REST YOUR EYES NOW! 👀"
            setTextColor(inkColor)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        })

        // Optical Deflection Target Box (Mint)
        val deflectionBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val dBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(mintContainer)
                setStroke(6, blackColor)
                cornerRadius = 20f
            }
            background = dBg
            setPadding(24, 18, 24, 18)
        }
        deflectionBox.addView(TextView(context).apply {
            text = "OPTICAL DEFLECTION TARGET ≥ 20 FT (6M)"
            setTextColor(darkGreen)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        })
        deflectionBox.addView(TextView(context).apply {
            text = "Stop staring at this screen. Fixate on an object across the room, out a window, or down the corridor."
            setTextColor(inkColor)
            textSize = 12f
            setPadding(0, 8, 0, 0)
        })
        cardContent.addView(deflectionBox)

        // Giant Countdown Numeral (Space Mono / Monospace)
        countdownTextView = TextView(context).apply {
            text = countdownValue.toString()
            setTextColor(inkColor)
            textSize = 72f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 4)
        }
        cardContent.addView(countdownTextView)

        cardContent.addView(TextView(context).apply {
            text = "Blink softly • Breathe deeply • Release jaw tension"
            setTextColor(Color.parseColor("#4B4731"))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })

        // Big Yellow CTA Button
        val btnShadow = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val shadow = android.graphics.drawable.GradientDrawable().apply {
                setColor(blackColor)
                cornerRadius = 20f
            }
            background = shadow
            setPadding(0, 0, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val dismissBtn = Button(context).apply {
            text = "✓ COMPLETE BREAK"
            setTextColor(blackColor)
            val btnBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(yellowColor)
                setStroke(8, blackColor)
                cornerRadius = 20f
            }
            background = btnBg
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(32, 28, 32, 28)
            setOnClickListener {
                hide()
                onBreakFinished()
                onDismissed()
            }
        }
        btnShadow.addView(dismissBtn)
        cardContent.addView(btnShadow)

        // Snooze Button underneath
        val snoozeBtn = Button(context).apply {
            text = "SNOOZE (+120S)"
            setTextColor(inkColor)
            val sBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke(6, blackColor)
                cornerRadius = 16f
            }
            background = sBg
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
            }
            setOnClickListener {
                countdownValue = 20
                countdownTextView?.text = "20"
            }
        }
        cardContent.addView(snoozeBtn)

        card.addView(cardContent)
        cardWrapper.addView(card)
        root.addView(cardWrapper)

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
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    // ── Haptic feedback ───────────────────────────────────────────────────────

    private fun triggerVibration() {
        val pattern = longArrayOf(0, 150, 100, 150, 100, 150)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(pattern, -1)
            }
        }
    }
}
