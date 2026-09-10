package com.attentiontracker

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.attentiontracker.service.AttentionService
import com.attentiontracker.ui.AppTab
import com.attentiontracker.ui.NeoBottomBar
import com.attentiontracker.ui.NeoTopBar
import com.attentiontracker.ui.screens.*
import com.attentiontracker.ui.theme.*
import com.attentiontracker.util.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class AppUsage(
    val packageName: String,
    val timeMs: Long,
    val label: String,
    val icon: android.graphics.drawable.Drawable? = null
)

data class TimeOfDayUsage(
    val label: String,
    val timeMs: Long
)

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun getTodayUsageStats(context: Context): List<AppUsage> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val startTime = calendar.timeInMillis
    val endTime = System.currentTimeMillis()

    val usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
    if (usageStatsList.isNullOrEmpty()) return emptyList()

    val pm = context.packageManager
    return usageStatsList
        .filter { it.totalTimeInForeground > 0 }
        .mapNotNull {
            val appInfo = try { pm.getApplicationInfo(it.packageName, 0) } catch (e: Exception) { null }
            if (appInfo == null) null
            else {
                val label = appInfo.loadLabel(pm).toString()
                val icon = try { pm.getApplicationIcon(it.packageName) } catch (e: Exception) { null }
                AppUsage(it.packageName, it.totalTimeInForeground, label, icon)
            }
        }
        .sortedByDescending { it.timeMs }
        .take(5)
}

fun getTimeOfDayUsage(context: Context): List<TimeOfDayUsage> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val dayStart = cal.timeInMillis
    val now = System.currentTimeMillis()

    val events = usm.queryEvents(dayStart, now) ?: return emptyList()

    val bucketRanges = listOf(
        Triple("🌅 Morning",   6,  12),
        Triple("☀️ Afternoon", 12, 17),
        Triple("🌆 Evening",   17, 21),
        Triple("🌙 Night",     21, 30)
    )
    val bucketMs = LongArray(4) { 0L }

    val event = android.app.usage.UsageEvents.Event()
    var currentForegroundApp: String? = null
    var lastEventTime = -1L

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val ts = event.timeStamp
        val type = event.eventType

        if (type == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
            if (currentForegroundApp != null && lastEventTime > 0) {
                val duration = ts - lastEventTime
                if (duration > 0) addToBucket(lastEventTime, duration, bucketMs)
            }
            currentForegroundApp = event.packageName
            lastEventTime = ts
        } else if (type == android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND) {
            if (currentForegroundApp == event.packageName && lastEventTime > 0) {
                val duration = ts - lastEventTime
                if (duration > 0) addToBucket(lastEventTime, duration, bucketMs)
                currentForegroundApp = null
                lastEventTime = -1L
            }
        }
    }

    if (currentForegroundApp != null && lastEventTime > 0 && lastEventTime < now) {
        addToBucket(lastEventTime, now - lastEventTime, bucketMs)
    }

    return bucketRanges.mapIndexed { i, (label, _, _) ->
        TimeOfDayUsage(label, bucketMs[i])
    }.filter { it.timeMs > 0 }
}

private fun addToBucket(timestamp: Long, duration: Long, bucketMs: LongArray) {
    val eventCal = Calendar.getInstance()
    eventCal.timeInMillis = timestamp
    val hour = eventCal.get(Calendar.HOUR_OF_DAY)

    val bucketIndex = when {
        hour in 6..11  -> 0 // Morning
        hour in 12..16 -> 1 // Afternoon
        hour in 17..20 -> 2 // Evening
        else           -> 3 // Night
    }
    bucketMs[bucketIndex] += duration
}

fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
    if (drawable is android.graphics.drawable.BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 64
    val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 64
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cvs = android.graphics.Canvas(bmp)
    drawable.setBounds(0, 0, cvs.width, cvs.height)
    drawable.draw(cvs)
    return bmp
}

fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    return when {
        hours > 0   -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else        -> "<1m"
    }
}

enum class Screen {
    LOADING, ONBOARDING, MAIN
}

class MainActivity : AppCompatActivity() {

    private lateinit var prefManager: PreferenceManager

    // Reactive states
    private val isTrackingState = mutableStateOf(false)
    private val statusTextState = mutableStateOf("Ready to track")
    private val elapsedTextState = mutableStateOf("")
    private val thresholdSecondsState = mutableStateOf(15L)
    private val userNameState = mutableStateOf("")
    private val completedBreaksState = mutableStateOf(0)

    private val runtimePermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            startTracking()
        } else {
            Toast.makeText(this, "Camera permission required.", Toast.LENGTH_LONG).show()
        }
    }

    private val overlayPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            checkRuntimePermissionsAndStart()
        } else {
            Toast.makeText(this, "Overlay permission required.", Toast.LENGTH_LONG).show()
        }
    }

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != AttentionService.ACTION_STATUS_UPDATE) return
            val isLooking = intent.getBooleanExtra(AttentionService.EXTRA_IS_LOOKING, false)
            val elapsed = intent.getLongExtra(AttentionService.EXTRA_SECONDS_LOOKING, 0L)

            statusTextState.value = if (isLooking) "Looking at screen" else "Not looking"
            elapsedTextState.value = if (isLooking && elapsed > 0L) "Looking for: ${elapsed}s" else ""
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PreferenceManager(this)

        // Check if service is already running
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (AttentionService::class.java.name == service.service.className) {
                isTrackingState.value = true
                statusTextState.value = "Tracking active"
                break
            }
        }

        lifecycleScope.launch {
            thresholdSecondsState.value = prefManager.thresholdSeconds.first()
            userNameState.value = prefManager.userName.first()
        }

        lifecycleScope.launch {
            prefManager.completedBreaks.collect { breaks ->
                completedBreaksState.value = breaks
            }
        }

        registerStatusReceiver()

        setContent {
            AttentionTrackerTheme {
                var currentScreen by remember { mutableStateOf(Screen.LOADING) }
                var activeTab by remember { mutableStateOf(AppTab.TRACKER) }
                var showUsageDialog by remember { mutableStateOf(false) }

                val context = LocalContext.current
                var hasUsagePerm by remember { mutableStateOf(hasUsageStatsPermission(context)) }
                var usageStats by remember { mutableStateOf<List<AppUsage>>(emptyList()) }
                var timeOfDayStats by remember { mutableStateOf<List<TimeOfDayUsage>>(emptyList()) }

                val coroutineScope = rememberCoroutineScope()
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val fetchData = {
                        hasUsagePerm = hasUsageStatsPermission(context)
                        if (hasUsagePerm) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val newUsage = getTodayUsageStats(context)
                                val newTimeOfDay = getTimeOfDayUsage(context)
                                launch(Dispatchers.Main) {
                                    usageStats = newUsage
                                    timeOfDayStats = newTimeOfDay
                                }
                            }
                        }
                    }

                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            fetchData()
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(observer)
                    fetchData()

                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(Unit) {
                    val name = prefManager.userName.first()
                    if (name.isNotBlank()) {
                        currentScreen = Screen.MAIN
                    } else {
                        currentScreen = Screen.ONBOARDING
                    }

                    if (!hasUsageStatsPermission(this@MainActivity)) {
                        showUsageDialog = true
                    }
                }

                // Compute total screen time
                val totalScreenTimeStr = remember(usageStats) {
                    val totalMs = usageStats.sumOf { it.timeMs }
                    if (totalMs == 0L) "" else formatMs(totalMs)
                }

                if (showUsageDialog) {
                    AlertDialog(
                        onDismissRequest = { showUsageDialog = false },
                        title = { Text("Usage Access Required", fontWeight = FontWeight.ExtraBold) },
                        text = {
                            Text(
                                "To display your real screen time telemetry and top attention-draining apps, " +
                                "please grant Usage Access."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showUsageDialog = false
                                try {
                                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                        data = Uri.parse("package:$packageName")
                                    })
                                } catch (_: Exception) {
                                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            }) {
                                Text("Grant Access", fontWeight = FontWeight.Bold, color = NeoBlack)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUsageDialog = false }) {
                                Text("Later", color = Color(0xFF7C775F))
                            }
                        }
                    )
                }

                when (currentScreen) {
                    Screen.LOADING -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NeoSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NeoBlack)
                        }
                    }

                    Screen.ONBOARDING -> {
                        OnboardingScreen(
                            onContinue = { name ->
                                lifecycleScope.launch {
                                    prefManager.setUserName(name)
                                    userNameState.value = name
                                    currentScreen = Screen.MAIN
                                }
                            }
                        )
                    }

                    Screen.MAIN -> {
                        Scaffold(
                            topBar = {
                                NeoTopBar(
                                    currentTab = activeTab,
                                    onProfileClick = { activeTab = AppTab.SETTINGS }
                                )
                            },
                            bottomBar = {
                                NeoBottomBar(
                                    currentTab = activeTab,
                                    onTabSelected = { activeTab = it }
                                )
                            },
                            containerColor = NeoSurface
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                when (activeTab) {
                                    AppTab.TRACKER -> {
                                        TrackerScreen(
                                            userName = userNameState.value,
                                            completedBreaks = completedBreaksState.value,
                                            isTracking = isTrackingState.value,
                                            statusText = statusTextState.value,
                                            elapsedText = elapsedTextState.value,
                                            totalScreenTimeStr = totalScreenTimeStr,
                                            onToggleTracking = {
                                                if (isTrackingState.value) stopTracking()
                                                else checkPermissionsAndStart()
                                            },
                                            onOpenSettings = { activeTab = AppTab.SETTINGS }
                                        )
                                    }

                                    AppTab.ANALYTICS -> {
                                        AnalyticsScreen(
                                            usageStats = usageStats,
                                            timeOfDayStats = timeOfDayStats,
                                            hasUsagePerm = hasUsagePerm,
                                            onRequestUsagePerm = {
                                                try {
                                                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                                        data = Uri.parse("package:$packageName")
                                                    })
                                                } catch (_: Exception) {
                                                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                                }
                                            }
                                        )
                                    }

                                    AppTab.REST_MODE -> {
                                        RestModeScreen(
                                            onCompleteBreak = {
                                                lifecycleScope.launch {
                                                    prefManager.incrementCompletedBreaks()
                                                }
                                            }
                                        )
                                    }

                                    AppTab.SETTINGS -> {
                                        SettingsScreen(
                                            threshold = thresholdSecondsState.value,
                                            userName = userNameState.value,
                                            onThresholdChange = { newSec ->
                                                thresholdSecondsState.value = newSec
                                                lifecycleScope.launch { prefManager.setThreshold(newSec) }
                                                if (isTrackingState.value) sendThresholdToService(newSec)
                                            },
                                            onSaveProfile = { newName ->
                                                userNameState.value = newName
                                                lifecycleScope.launch { prefManager.setUserName(newName) }
                                            },
                                            onBack = { activeTab = AppTab.TRACKER }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(statusReceiver)
    }

    private fun checkPermissionsAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            overlayPermLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        checkRuntimePermissionsAndStart()
    }

    private fun checkRuntimePermissionsAndStart() {
        val needed = buildList<String> {
            if (!hasPerm(Manifest.permission.CAMERA)) add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasPerm(Manifest.permission.POST_NOTIFICATIONS)) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (needed.isEmpty()) startTracking() else runtimePermLauncher.launch(needed.toTypedArray())
    }

    private fun hasPerm(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun startTracking() {
        val intent = Intent(this, AttentionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        isTrackingState.value = true
        statusTextState.value = "Tracking started..."
        elapsedTextState.value = ""
    }

    private fun stopTracking() {
        stopService(Intent(this, AttentionService::class.java))
        isTrackingState.value = false
        statusTextState.value = "Tracking stopped"
        elapsedTextState.value = ""
    }

    private fun sendThresholdToService(seconds: Long) {
        startService(Intent(this, AttentionService::class.java).apply {
            action = AttentionService.ACTION_UPDATE_THRESHOLD
            putExtra(AttentionService.EXTRA_THRESHOLD, seconds)
        })
    }

    private fun registerStatusReceiver() {
        val filter = IntentFilter(AttentionService.ACTION_STATUS_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusReceiver, filter)
        }
    }
}

