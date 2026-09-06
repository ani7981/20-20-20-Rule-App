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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.attentiontracker.service.AttentionService
import com.attentiontracker.util.PreferenceManager
import com.attentiontracker.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.Calendar

data class AppUsage(val packageName: String, val timeMs: Long, val label: String, val icon: android.graphics.drawable.Drawable? = null)

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
            if (appInfo == null) null // Filter out deleted/uninstalled apps
            else {
                val label = appInfo.loadLabel(pm).toString()
                val icon = try { pm.getApplicationIcon(it.packageName) } catch (e: Exception) { null }
                AppUsage(it.packageName, it.totalTimeInForeground, label, icon)
            }
        }
        .sortedByDescending { it.timeMs }
        .take(5)
}

data class TimeOfDayUsage(val label: String, val timeMs: Long)

/**
 * Uses UsageStatsManager.queryEvents() to calculate how much screen time occurred
 * in each time-of-day bucket for today:
 *   Morning   06:00–12:00
 *   Afternoon 12:00–17:00
 *   Evening   17:00–21:00
 *   Night     21:00–06:00 (next day)
 */
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

    // Bucket boundaries (hour of day)
    val bucketRanges = listOf(
        Triple("🌅 Morning",   6,  12),
        Triple("☀️ Afternoon", 12, 17),
        Triple("🌆 Evening",   17, 21),
        Triple("🌙 Night",     21, 30)  // 30 = covers 21–00 + 0–6 wrapped
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

    // Add time for the app currently in the foreground (if any)
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
        else           -> 3 // Night (21–5)
    }
    bucketMs[bucketIndex] += duration
}

enum class Screen {
    LOADING, ONBOARDING, DASHBOARD, SETTINGS
}

class MainActivity : AppCompatActivity() {

    private lateinit var prefManager: PreferenceManager
    
    // Compose State
    private val isTrackingState = mutableStateOf<Boolean>(false)
    private val statusTextState = mutableStateOf<String>("Ready to track")
    private val elapsedTextState = mutableStateOf<String>("")
    private val thresholdSecondsState = mutableStateOf<Long>(15L)
    private val userNameState = mutableStateOf<String>("")
    private val completedBreaksState = mutableStateOf<Int>(0)

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

        // Check if our service is already running so UI matches reality
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
                var currentScreen by remember { mutableStateOf<Screen>(Screen.LOADING) }
                var showUsageDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val name = prefManager.userName.first()
                    if (name.isNotBlank()) {
                        currentScreen = Screen.DASHBOARD
                    } else {
                        currentScreen = Screen.ONBOARDING
                    }
                    
                    if (!hasUsageStatsPermission(this@MainActivity)) {
                        showUsageDialog = true
                    }
                }

                if (showUsageDialog) {
                    AlertDialog(
                        onDismissRequest = { showUsageDialog = false },
                        title = { Text("Usage Access Required") },
                        text = {
                            Text(
                                "To display your screen time graphs, the app needs Usage Access.\n\n" +
                                "If the setting is greyed out on your phone:\n" +
                                "1. Click 'App Info' below.\n" +
                                "2. Tap the 3 vertical dots at the top right.\n" +
                                "3. Tap 'Allow restricted settings'.\n" +
                                "4. Come back and click 'Grant Usage Access'."
                            )
                        },
                        confirmButton = {
                            Column(horizontalAlignment = Alignment.End) {
                                TextButton(onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:$packageName")
                                    }
                                    startActivity(intent)
                                }) {
                                    Text("App Info (Unlock)", fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = {
                                    showUsageDialog = false
                                    try {
                                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                            data = Uri.parse("package:$packageName")
                                        }
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                    }
                                }) {
                                    Text("Grant Usage Access", fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUsageDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                when (currentScreen) {
                    Screen.LOADING -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    Screen.ONBOARDING -> {
                        OnboardingScreen(onContinue = { name ->
                            lifecycleScope.launch {
                                prefManager.setUserName(name)
                                userNameState.value = name
                                currentScreen = Screen.DASHBOARD
                            }
                        })
                    }
                    Screen.DASHBOARD -> {
                        DashboardScreen(
                            userName = userNameState.value,
                            completedBreaks = completedBreaksState.value,
                            isTracking = isTrackingState.value,
                            statusText = statusTextState.value,
                            elapsedText = elapsedTextState.value,
                            onToggleTracking = {
                                if (isTrackingState.value) stopTracking() else checkPermissionsAndStart()
                            },
                            onOpenSettings = { currentScreen = Screen.SETTINGS }
                        )
                    }
                    Screen.SETTINGS -> {
                        SettingsScreen(
                            threshold = thresholdSecondsState.value,
                            userName = userNameState.value,
                            onThresholdChange = { newThreshold ->
                                thresholdSecondsState.value = newThreshold
                                lifecycleScope.launch { prefManager.setThreshold(newThreshold) }
                                if (isTrackingState.value) sendThresholdToService(newThreshold)
                            },
                            onBack = { currentScreen = Screen.DASHBOARD },
                            onChangeName = { currentScreen = Screen.ONBOARDING }
                        )
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

@Composable
fun OnboardingScreen(onContinue: (String) -> Unit) {
    var name by remember { mutableStateOf<String>("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkNavy, MidNavy)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "What should we call you?",
                style = MaterialTheme.typography.titleMedium,
                color = SubText
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Your name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    cursorColor = AccentCyan,
                    focusedLabelColor = AccentCyan
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { onContinue(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Continue",
                    color = DarkNavy,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    userName: String,
    completedBreaks: Int,
    isTracking: Boolean,
    statusText: String,
    elapsedText: String,
    onToggleTracking: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var hasUsagePerm by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    var usageStats by remember { mutableStateOf<List<AppUsage>>(emptyList()) }
    var timeOfDayStats by remember { mutableStateOf<List<TimeOfDayUsage>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Fetch data whenever app resumes, AND immediately on first composition
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
        
        // Add observer for lifecycle changes
        lifecycleOwner.lifecycle.addObserver(observer)
        
        // Force an immediate initial fetch because ON_RESUME might have already fired
        // before this composable was added to the screen
        fetchData()
        
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ─── Neo-Brutalist Color Tokens ───────────────────────────────────────────
    val NeoYellow        = Color(0xFFFFE600)
    val NeoMint          = Color(0xFF2DE28D)
    val NeoMintContainer = Color(0xFF53FCA4)
    val NeoPink          = Color(0xFFFF5C8D)
    val NeoCyan          = Color(0xFF38DBFF)
    val NeoOrange        = Color(0xFFFF6B4A)
    val NeoBlack         = Color(0xFF000000)
    val NeoWhite         = Color(0xFFFFFFFF)
    val NeoPaper         = Color(0xFFFFFDF9)
    val NeoInk           = Color(0xFF1B1B1B)
    val NeoSurface       = Color(0xFFF9F9F9)
    val NeoCard          = Color(0xFFFFFFFF)
    val NeoSurfaceMid    = Color(0xFFEEEEEE)
    val NeoLavender      = Color(0xFFE8D5FF)

    // Derive elapsedSeconds from elapsedText ("Looking for: Xs")
    val elapsedSeconds: Long = remember(elapsedText) {
        val match = Regex("(\\d+)s").find(elapsedText)
        match?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
    }
    // 20-minute threshold for the hero progress bar (20-20-20 protocol)
    val thresholdSeconds: Long = 1200L

    // Derive face-detected from statusText
    val isFaceDetected = statusText.contains("Looking at screen", ignoreCase = true) ||
        statusText.contains("Tracking active", ignoreCase = true)

    // Derive total screen time string from usageStats
    val totalScreenTimeStr: String = remember(usageStats) {
        val totalMs = usageStats.sumOf { it.timeMs }
        if (totalMs == 0L) ""
        else {
            val h = totalMs / 3_600_000L
            val m = (totalMs % 3_600_000L) / 60_000L
            if (h > 0) "${h}h ${m}m" else "${m}m"
        }
    }

    // Color tokens for analytics charts (kept from original)
    val appColors = listOf(NeoYellow, NeoMint, NeoCyan, NeoPink, NeoOrange)
    val timeOfDayColors = listOf(
        Color(0xFFFFD54F), Color(0xFFFF8A65), Color(0xFF7986CB), Color(0xFF4FC3F7)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoPaper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════════════
            // SECTION 1 — TOP HEADER
            // ═══════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "HEY, ${userName.uppercase()}! 👋",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeoInk,
                    letterSpacing = 0.5.sp
                )

                // Settings button with neo offset shadow
                Box(modifier = Modifier.padding(bottom = 3.dp, end = 3.dp)) {
                    // Shadow layer
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .offset(x = 3.dp, y = 3.dp)
                            .background(NeoBlack, RoundedCornerShape(4.dp))
                    )
                    // Button face
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(NeoSurfaceMid, RoundedCornerShape(4.dp))
                            .border(3.dp, NeoBlack, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = NeoBlack,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ═══════════════════════════════════════════════════════════
            // SECTION 2 — HERO TRACKER CARD (Yellow)
            // ═══════════════════════════════════════════════════════════
            Box(modifier = Modifier.padding(bottom = 6.dp, end = 6.dp)) {
                // Hard drop shadow – 6dp offset
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize()
                        .offset(x = 6.dp, y = 6.dp)
                        .background(NeoBlack, RoundedCornerShape(12.dp))
                )
                // Yellow card face
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoYellow, RoundedCornerShape(12.dp))
                        .border(4.dp, NeoBlack, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    // (a) Status badge pill
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val pillBg = if (isFaceDetected) NeoMintContainer else NeoOrange
                        val pillEmoji = if (isFaceDetected) "🟢" else "🔴"
                        val pillText = if (isFaceDetected) "LOOKING AT SCREEN" else "LOOKING AWAY"
                        Box(
                            modifier = Modifier
                                .background(pillBg, RoundedCornerShape(999.dp))
                                .border(2.5.dp, NeoBlack, RoundedCornerShape(999.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$pillEmoji $pillText",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeoBlack,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // (b) Big countdown timer
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val displayTimer = if (elapsedText.isNotEmpty()) elapsedText else statusText
                        Text(
                            text = displayTimer,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // (c) Threshold progress bar – 20 chunky rectangular cells
                    val totalCells = 20
                    val filledCells = if (thresholdSeconds > 0L)
                        ((elapsedSeconds.toFloat() / thresholdSeconds.toFloat()) * totalCells)
                            .toInt().coerceIn(0, totalCells)
                    else 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(totalCells) { idx ->
                            val isFilled = idx < filledCells
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .background(
                                        if (isFilled) NeoMint else NeoCard,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .border(1.5.dp, NeoBlack, RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // (d) Primary action button
                    Box(modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)) {
                        // Shadow
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .offset(x = 4.dp, y = 4.dp)
                                .background(NeoBlack, RoundedCornerShape(12.dp))
                        )
                        // Button face
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(
                                    if (!isTracking) NeoPink else NeoWhite,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(3.5.dp, NeoBlack, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.TextButton(
                                onClick = onToggleTracking,
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (!isTracking) "▶ START MONITORING" else "⏸ PAUSE MONITORING",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeoBlack,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ═══════════════════════════════════════════════════════════
            // SECTION 3 — QUICK STATS 2-COLUMN GRID
            // ═══════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card A – Breaks Hit
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 4.dp, end = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize()
                            .offset(x = 4.dp, y = 4.dp)
                            .background(NeoBlack, RoundedCornerShape(12.dp))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeoMintContainer, RoundedCornerShape(12.dp))
                            .border(3.dp, NeoBlack, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BREAKS HIT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeoBlack,
                                letterSpacing = 1.sp
                            )
                            Text(text = "✅", fontSize = 14.sp)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(NeoBlack)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "$completedBreaks",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoBlack,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "today",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoInk
                        )
                    }
                }

                // Card B – Screen Time
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 4.dp, end = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize()
                            .offset(x = 4.dp, y = 4.dp)
                            .background(NeoBlack, RoundedCornerShape(12.dp))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF57FFD4), RoundedCornerShape(12.dp))
                            .border(3.dp, NeoBlack, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SCREEN TIME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeoBlack,
                                letterSpacing = 1.sp
                            )
                            Text(text = "👁", fontSize = 14.sp)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(NeoBlack)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (!hasUsagePerm || totalScreenTimeStr.isEmpty()) {
                            Text(
                                text = if (!hasUsagePerm) "GRANT\nPERM" else "—",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeoBlack,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                lineHeight = 28.sp
                            )
                        } else {
                            Text(
                                text = totalScreenTimeStr,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeoBlack,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "today",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoInk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ═══════════════════════════════════════════════════════════
            // SECTION 4 — HARDWARE & TELEMETRY CARD
            // ═══════════════════════════════════════════════════════════
            Box(modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)) {
                // Shadow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize()
                        .offset(x = 4.dp, y = 4.dp)
                        .background(NeoBlack, RoundedCornerShape(12.dp))
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoCard, RoundedCornerShape(12.dp))
                        .border(3.dp, NeoBlack, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🖥", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HARDWARE & TELEMETRY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeoBlack,
                                letterSpacing = 1.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(NeoSurfaceMid, RoundedCornerShape(4.dp))
                                .border(1.dp, NeoBlack, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OFFLINE NPU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoBlack,
                                letterSpacing = 0.5.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(NeoBlack))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 3-column telemetry tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Battery tile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 3.dp, end = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .matchParentSize()
                                    .offset(x = 3.dp, y = 3.dp)
                                    .background(NeoBlack, RoundedCornerShape(8.dp))
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NeoMintContainer, RoundedCornerShape(8.dp))
                                    .border(2.dp, NeoBlack, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🔋 —%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeoBlack,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "BATTERY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeoBlack,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Power Draw tile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 3.dp, end = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .matchParentSize()
                                    .offset(x = 3.dp, y = 3.dp)
                                    .background(NeoBlack, RoundedCornerShape(8.dp))
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NeoYellow, RoundedCornerShape(8.dp))
                                    .border(2.dp, NeoBlack, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "⚡ —mA",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeoBlack,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "DRAW",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeoBlack,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // AI Mode tile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 3.dp, end = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .matchParentSize()
                                    .offset(x = 3.dp, y = 3.dp)
                                    .background(NeoBlack, RoundedCornerShape(8.dp))
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NeoLavender, RoundedCornerShape(8.dp))
                                    .border(2.dp, NeoBlack, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📷 2 FPS",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeoBlack,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "AI MODE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeoBlack,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer text
                    Text(
                        text = "Zero cloud latency. On-device detection.",
                        fontSize = 11.sp,
                        color = NeoInk.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════════════════════════════════════════════════════════
            // ANALYTICS SECTION — kept from original
            // ═══════════════════════════════════════════════════════════

            // NeoYellow underlined "YOUR EYES TODAY" header
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 4.dp, bottom = 20.dp)
            ) {
                Column {
                    Text(
                        text = "YOUR EYES TODAY",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeoInk,
                        letterSpacing = androidx.compose.ui.unit.TextUnit(-0.5f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(NeoYellow)
                    )
                }
            }

            // ── Neobrutalist BarChart container ──
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(314.dp)
                        .offset(x = 4.dp, y = 4.dp)
                        .background(NeoBlack)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(314.dp)
                        .background(NeoCard)
                        .then(
                            Modifier.border(
                                width = 3.dp,
                                color = NeoBlack,
                                shape = androidx.compose.foundation.shape.RectangleShape
                            )
                        )
                ) {
                    if (usageStats.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No data available",
                                color = NeoInk,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        BarChart(
                            data = usageStats.map { it.timeMs.toFloat() },
                            barColors = appColors,
                            labels = usageStats.map { it.label },
                            icons = usageStats.map { it.icon },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Top Attention-Draining Apps List ──
            if (usageStats.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(NeoYellow)
                            .border(width = 3.dp, color = NeoBlack, shape = androidx.compose.foundation.shape.RectangleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "TOP ATTENTION DRAINS",
                            fontWeight = FontWeight.ExtraBold,
                            color = NeoBlack,
                            style = MaterialTheme.typography.labelMedium,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                    }
                    Text(
                        text = "${usageStats.size} APPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B4731)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val maxUsageMs = usageStats.maxOfOrNull { it.timeMs } ?: 1L
                val rowAccentColors = listOf(NeoYellow, NeoMint, NeoCyan, NeoOrange, NeoPink)

                usageStats.forEachIndexed { index, app ->
                    val barFillColor = rowAccentColors[index % rowAccentColors.size]
                    val fillFraction = (app.timeMs.toFloat() / maxUsageMs.toFloat()).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .matchParentSize()
                                .offset(x = 3.dp, y = 3.dp)
                                .background(NeoBlack)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeoCard)
                                .border(width = 3.dp, color = NeoBlack, shape = androidx.compose.foundation.shape.RectangleShape)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(NeoSurfaceMid, shape = RoundedCornerShape(8.dp))
                                            .border(width = 2.dp, color = NeoBlack, shape = RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val drawable = app.icon
                                        if (drawable != null) {
                                            val bmp = remember(app.packageName) { drawableToBitmap(drawable) }
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = app.label,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .background(barFillColor, RoundedCornerShape(4.dp))
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = app.label,
                                        fontWeight = FontWeight.Bold,
                                        color = NeoInk,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(NeoYellow)
                                        .border(width = 2.dp, color = NeoBlack, shape = androidx.compose.foundation.shape.RectangleShape)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = formatMs(app.timeMs),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = androidx.compose.ui.unit.sp(12f),
                                        color = NeoBlack
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .background(NeoCard)
                                    .border(width = 2.dp, color = NeoBlack, shape = androidx.compose.foundation.shape.RectangleShape)
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fillFraction)
                                        .background(NeoCyan)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // ── Screen Time Distribution (Pie chart) ──
            Text(
                text = "Screen Time Distribution:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeoInk,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = NeoCard)
            ) {
                if (timeOfDayStats.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data yet today", color = NeoInk.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    }
                } else {
                    PieChart(
                        data = timeOfDayStats.map { it.timeMs.toFloat() },
                        labels = timeOfDayStats.map { it.label },
                        colors = timeOfDayColors,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsScreen(
    threshold: Long,
    userName: String,
    onThresholdChange: (Long) -> Unit,
    onBack: () -> Unit,
    onChangeName: () -> Unit
) {
    BackHandler { onBack() }

    // ── Neo colour tokens (raw hex, no theme dependency) ──────────────────────
    val NeoYellowC      = Color(0xFFFFE600)
    val NeoMintC        = Color(0xFF2DE28D)
    val NeoMintContainerC = Color(0xFF53FCA4)
    val NeoBlackC       = Color(0xFF000000)
    val NeoWhiteC       = Color(0xFFFFFFFF)
    val NeoPaperC       = Color(0xFFFFFDF9)
    val NeoInkC         = Color(0xFF1B1B1B)
    val NeoSurfaceMidC  = Color(0xFFEEEEEE)
    val NeoLavenderC    = Color(0xFFE8D5FF)

    // Threshold in whole minutes for the stepper (clamped 1..60)
    val thresholdMinutes = (threshold / 60L).coerceIn(1L, 60L)
    val presets = listOf(15L, 20L, 25L, 30L)

    var editedName by remember(userName) { mutableStateOf(userName) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoPaperC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {

            // ── Back row ──────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = NeoInkC
                    )
                }
            }

            // ── Header section ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PREFERENCES & AI SENSORS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = androidx.compose.ui.unit.sp(24f),
                        color = NeoInkC,
                        letterSpacing = androidx.compose.ui.unit.TextUnit(-0.5f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tune detection intervals and battery modes.",
                        fontSize = androidx.compose.ui.unit.sp(14f),
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4B4731)
                    )
                }
                // v2.0 sticker badge
                Box(
                    modifier = Modifier
                        .graphicsLayer { rotationZ = 3f }
                        .background(NeoLavenderC, RoundedCornerShape(50))
                        .border(2.dp, NeoBlackC, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "v2.0",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = androidx.compose.ui.unit.sp(11f),
                        color = NeoBlackC
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Threshold Stepper Card ────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                // Hard shadow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 5.dp, y = 5.dp)
                        .background(NeoBlackC, RoundedCornerShape(12.dp))
                )
                // Card body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoWhiteC, RoundedCornerShape(12.dp))
                        .border(4.dp, NeoBlackC, RoundedCornerShape(12.dp))
                ) {
                    // Card header band
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeoSurfaceMidC, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "EYE BREAK INTERVAL",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = androidx.compose.ui.unit.sp(13f),
                            color = NeoInkC,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Stepper row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Minus button
                            Box(modifier = Modifier.size(64.dp)) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .offset(x = 4.dp, y = 4.dp)
                                        .background(NeoBlackC, RoundedCornerShape(8.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(NeoYellowC, RoundedCornerShape(8.dp))
                                        .border(4.dp, NeoBlackC, RoundedCornerShape(8.dp))
                                        .clickable(
                                            onClick = {
                                                val newMin = (thresholdMinutes - 1L).coerceAtLeast(1L)
                                                onThresholdChange(newMin * 60L)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("−", fontWeight = FontWeight.ExtraBold,
                                        fontSize = androidx.compose.ui.unit.sp(28f), color = NeoBlackC)
                                }
                            }

                            // Centre display
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$thresholdMinutes",
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = androidx.compose.ui.unit.sp(48f),
                                    color = NeoInkC
                                )
                                Text(
                                    text = "MIN",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = androidx.compose.ui.unit.sp(14f),
                                    color = NeoInkC,
                                    letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)
                                )
                            }

                            // Plus button
                            Box(modifier = Modifier.size(64.dp)) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .offset(x = 4.dp, y = 4.dp)
                                        .background(NeoBlackC, RoundedCornerShape(8.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(NeoYellowC, RoundedCornerShape(8.dp))
                                        .border(4.dp, NeoBlackC, RoundedCornerShape(8.dp))
                                        .clickable(
                                            onClick = {
                                                val newMin = (thresholdMinutes + 1L).coerceAtMost(60L)
                                                onThresholdChange(newMin * 60L)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", fontWeight = FontWeight.ExtraBold,
                                        fontSize = androidx.compose.ui.unit.sp(28f), color = NeoBlackC)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick preset pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { preset ->
                                val isSelected = thresholdMinutes == preset
                                Box(modifier = Modifier.weight(1f)) {
                                    // Shadow
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .offset(x = 3.dp, y = 3.dp)
                                            .background(NeoBlackC, RoundedCornerShape(6.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelected) NeoMintContainerC else NeoWhiteC,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(3.dp, NeoBlackC, RoundedCornerShape(6.dp))
                                            .clickable { onThresholdChange(preset * 60L) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${preset}M",
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = androidx.compose.ui.unit.sp(12f),
                                            color = NeoBlackC,
                                            letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── AI & Hardware Toggles Card ────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 4.dp, y = 4.dp)
                        .background(NeoBlackC, RoundedCornerShape(12.dp))
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoWhiteC, RoundedCornerShape(12.dp))
                        .border(3.dp, NeoBlackC, RoundedCornerShape(12.dp))
                ) {
                    // Header band
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeoSurfaceMidC, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "DETECTION & HARDWARE SWITCHES",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = androidx.compose.ui.unit.sp(12f),
                            color = NeoInkC,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(0.5f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Toggle rows — visual wrappers only; logic untouched by callers
                        NeoToggleRow(
                            icon = "👁",
                            title = "Front Camera Attention",
                            subtitle = "Detects gaze direction at ultra-low power",
                            checked = true,  // read-only visual; no logic change needed
                            onCheckedChange = {},
                            neoBlack = NeoBlackC,
                            neoYellow = NeoYellowC,
                            neoSurface = NeoSurfaceMidC
                        )
                        NeoToggleRow(
                            icon = "🔔",
                            title = "Live Sensor Shade",
                            subtitle = "Show countdown in Android notification shade",
                            checked = true,
                            onCheckedChange = {},
                            neoBlack = NeoBlackC,
                            neoYellow = NeoYellowC,
                            neoSurface = NeoSurfaceMidC
                        )
                        NeoToggleRow(
                            icon = "📳",
                            title = "Vibrate & Audio Cue",
                            subtitle = "Haptic pulse & tone when 20 minutes elapsed",
                            checked = true,
                            onCheckedChange = {},
                            neoBlack = NeoBlackC,
                            neoYellow = NeoYellowC,
                            neoSurface = NeoSurfaceMidC
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Profile Card ──────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 4.dp, y = 4.dp)
                        .background(NeoBlackC, RoundedCornerShape(12.dp))
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoLavenderC, RoundedCornerShape(12.dp))
                        .border(3.dp, NeoBlackC, RoundedCornerShape(12.dp))
                ) {
                    // Header band
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                NeoLavenderC.copy(alpha = 0.7f),
                                RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                            )
                            .border(
                                width = 0.dp, color = Color.Transparent,
                                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "YOUR PROFILE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = androidx.compose.ui.unit.sp(13f),
                            color = NeoBlackC,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Name input field
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            placeholder = {
                                Text("YOUR NAME", fontWeight = FontWeight.Bold, color = Color(0xFF9E9E9E))
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = NeoBlackC,
                                focusedBorderColor = NeoBlackC,
                                focusedContainerColor = NeoWhiteC,
                                unfocusedContainerColor = NeoWhiteC,
                                cursorColor = NeoBlackC
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Save button
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = 4.dp, y = 4.dp)
                                    .background(NeoBlackC, RoundedCornerShape(8.dp))
                            )
                            Button(
                                onClick = {
                                    if (editedName.isNotBlank()) onChangeName()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeoMintC),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(3.dp, NeoBlackC),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "SAVE PROFILE",
                                    color = NeoBlackC,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = androidx.compose.ui.unit.sp(14f),
                                    letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Reusable Neo Toggle Row ───────────────────────────────────────────────────
@Composable
private fun NeoToggleRow(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    neoBlack: Color,
    neoYellow: Color,
    neoSurface: Color
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(neoBlack, RoundedCornerShape(8.dp))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFFFFF), RoundedCornerShape(8.dp))
                .border(3.dp, neoBlack, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = androidx.compose.ui.unit.sp(22f))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = androidx.compose.ui.unit.sp(14f),
                    color = Color(0xFF1B1B1B)
                )
                Text(
                    text = subtitle,
                    fontSize = androidx.compose.ui.unit.sp(12f),
                    color = Color(0xFF4B4731),
                    fontWeight = FontWeight.Medium
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = neoBlack,
                    checkedTrackColor = neoYellow,
                    uncheckedTrackColor = neoSurface
                )
            )
        }
    }
}

// ── Helper: convert any Drawable to a Bitmap ──────────────────────────────────

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

// ── Bar Chart (Neo-Brutalist): Y-axis hour scale, thick black gridlines, solid fills ───────────

@Composable
fun BarChart(
    data: List<Float>,
    barColors: List<Color>,
    labels: List<String> = emptyList(),
    icons: List<android.graphics.drawable.Drawable?> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || (data.maxOrNull() ?: 0f) == 0f) return
    val maxData = data.maxOrNull() ?: 1f
    // Compute the next whole-hour ceiling so Y-axis ticks are clean
    val maxMs = maxData.toLong()
    val maxHours = ((maxMs / 3_600_000L) + 1L).coerceAtLeast(1L).toInt()

    // Neo-Brutalist bar fill colours: NeoYellow → NeoMint → NeoCyan, cycling
    val neoBrutalColors = listOf(
        Color(0xFFFFE600), // NeoYellow
        Color(0xFF2DE28D), // NeoMint
        Color(0xFF38DBFF), // NeoCyan
        Color(0xFFFF5C8D), // NeoPink
        Color(0xFFFF6B4A)  // NeoOrange
    )
    val capturedColors = neoBrutalColors

    // Y-axis label paint – monospace black ink
    val axisPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 28f
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }

    // Time label above each bar – black monospace
    val timePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.MONOSPACE,
                android.graphics.Typeface.BOLD
            )
        }
    }

    // Thick gridline paint – black at 25% opacity
    val gridPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(64, 0, 0, 0) // 25% black
            strokeWidth = 3f
            style = android.graphics.Paint.Style.STROKE
        }
    }

    // Black bar-outline paint
    val barBorderPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 2f
            style = android.graphics.Paint.Style.STROKE
        }
    }

    Column(modifier = modifier) {
        // Canvas draws white background, Y-axis labels, thick black gridlines, and solid bars
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val yAxisWidthPx = 40.dp.toPx()
            val chartWidth = size.width - yAxisWidthPx
            val count = data.size
            val totalGap = chartWidth * 0.15f
            val barWidth = (chartWidth - totalGap) / count
            val gap = totalGap / (count + 1)

            // ── White chart area background ──
            drawRect(
                color = Color(0xFFFFFFFF),
                topLeft = Offset(yAxisWidthPx, 0f),
                size = androidx.compose.ui.geometry.Size(chartWidth, size.height)
            )

            // ── Thick black gridlines at each hour + Y-axis labels (monospace, black) ──
            for (hour in 0..maxHours) {
                val y = size.height - (hour.toFloat() / maxHours.toFloat()) * size.height

                // Draw thick black gridline (25% opacity)
                drawContext.canvas.nativeCanvas.drawLine(
                    yAxisWidthPx, y, size.width, y,
                    gridPaint
                )

                // Clamp text baseline inside canvas
                val textY = (y + axisPaint.textSize / 3f)
                    .coerceIn(axisPaint.textSize, size.height - 2f)
                drawContext.canvas.nativeCanvas.drawText(
                    "${hour}h",
                    yAxisWidthPx - 6f,
                    textY,
                    axisPaint
                )
            }

            // ── Draw bars: solid fill + 2px black Rect outline, no gradients ──
            for ((index, value) in data.withIndex()) {
                val barHeight = (value / (maxHours.toFloat() * 3_600_000f)) * size.height
                val left = yAxisWidthPx + gap + index * (barWidth + gap)
                val right = left + barWidth * 0.75f // actual bar footprint

                if (barHeight > 0f) {
                    val topY = (size.height - barHeight).coerceAtLeast(0f)

                    // Solid fill – no gradients
                    drawRect(
                        color = capturedColors[index % capturedColors.size],
                        topLeft = Offset(left, topY),
                        size = androidx.compose.ui.geometry.Size(right - left, barHeight)
                    )

                    // 2px solid black border drawn as outline Rect
                    drawContext.canvas.nativeCanvas.drawRect(
                        left, topY, right, size.height,
                        barBorderPaint
                    )

                    // Time label above the bar – black monospace
                    val centerX = (left + right) / 2f
                    val textY = (topY - 8f).coerceAtLeast(timePaint.textSize)
                    drawContext.canvas.nativeCanvas.drawText(
                        formatMs(value.toLong()),
                        centerX,
                        textY,
                        timePaint
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App icon + name row — starts after Y-axis width to align with bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEachIndexed { index, _ ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val drawable = icons.getOrNull(index)
                    if (drawable != null) {
                        val labelKey = labels.getOrElse(index) { index.toString() }
                        val bmp = remember(labelKey) { drawableToBitmap(drawable) }
                        // App icon in a bordered square box (2dp border, 8dp corner radius)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
                                .border(2.dp, Color(0xFF000000), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = labels.getOrElse(index) { "" },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    capturedColors[index % capturedColors.size],
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(2.dp, Color(0xFF000000), RoundedCornerShape(4.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (labels.isNotEmpty()) {
                        Text(
                            text = labels.getOrElse(index) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1B),
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ── Pie Chart with donut arc + colour-coded legend ─────────────────────────────

@Composable
fun PieChart(data: List<Float>, labels: List<String>, colors: List<Color>, modifier: Modifier = Modifier) {
    if (data.isEmpty() || data.sum() == 0f) return
    val total = data.sum()
    val capturedColors = colors.map { it }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut arc
        Canvas(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
        ) {
            val diameter = minOf(size.width, size.height)
            val strokeWidth = diameter * 0.20f
            val radius = (diameter - strokeWidth) / 2f
            val topLeft = Offset(
                (size.width - radius * 2f) / 2f,
                (size.height - radius * 2f) / 2f
            )
            val arcSize = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)

            var startAngle = -90f
            for ((index, value) in data.withIndex()) {
                val sweepAngle = (value / total) * 360f
                drawArc(
                    color = capturedColors[index % capturedColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle - 2f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend column: colour dot + app name + time
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEachIndexed { index, value ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                colors[index % colors.size],
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = labels.getOrElse(index) { "App ${index + 1}" },
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatMs(value.toLong()),
                            style = MaterialTheme.typography.labelSmall,
                            color = SubText,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ── Helper: format milliseconds to human-readable time ────────────────────────

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
