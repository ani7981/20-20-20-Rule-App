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
    ONBOARDING, DASHBOARD, SETTINGS
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
                var currentScreen by remember { mutableStateOf<Screen>(Screen.ONBOARDING) }
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsagePerm = hasUsageStatsPermission(context)
                if (hasUsagePerm) {
                    usageStats = getTodayUsageStats(context)
                    timeOfDayStats = getTimeOfDayUsage(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkNavy, MidNavy)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Welcome back, $userName",
                style = MaterialTheme.typography.titleLarge,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Status card ── fixed height so centering works ──
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTracking) AccentCyan else SubText
                        )
                        if (elapsedText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = elapsedText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = SubText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Breaks completed card ──
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Breaks Completed Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$completedBreaks",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Tracking button (above charts) ──
            val buttonColor by animateColorAsState(
                targetValue = if (isTracking) Color(0xFFE53935) else AccentCyan,
                label = "buttonColor"
            )
            val textColor by animateColorAsState(
                targetValue = if (isTracking) Color.White else DarkNavy,
                label = "textColor"
            )

            Button(
                onClick = onToggleTracking,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = if (isTracking) "Stop" else "Start Monitoring",
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Screen Time Section ──
            Text(
                text = "App Wise Screen Time:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Assuming permission requested on launch, always render charts section.
            // If permission is denied, charts will gracefully display 'No data available'.
                val appColors = listOf(
                    AccentCyan,
                    Color(0xFF81C784),
                    Color(0xFFFFB74D),
                    Color(0xFFE57373),
                    Color(0xFFBA68C8)
                )
                val timeOfDayColors = listOf(
                    Color(0xFFFFD54F), // Morning – warm yellow
                    Color(0xFFFF8A65), // Afternoon – warm orange
                    Color(0xFF7986CB), // Evening – indigo
                    Color(0xFF4FC3F7)  // Night – cool cyan
                )

                // Bar chart — Top Apps
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.05f))
                ) {
                    if (usageStats.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No data available", color = SubText, fontWeight = FontWeight.Bold)
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

                Spacer(modifier = Modifier.height(24.dp))

                // Pie chart — Time of day (distinct info from bar chart)
                Text(
                    text = "Screen Time Distribution:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
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
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.05f))
                ) {
                    if (timeOfDayStats.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No data yet today", color = SubText, fontWeight = FontWeight.Bold)
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkNavy, MidNavy)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
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
                        tint = OnSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Break Threshold",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val formattedThreshold = if (threshold >= 60) {
                        val m = threshold / 60
                        val s = threshold % 60
                        if (s > 0L) "$m m $s s" else "$m m"
                    } else {
                        "${threshold}s"
                    }
                    Text(
                        text = "Break after: $formattedThreshold",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SubText,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = threshold.toFloat(),
                        onValueChange = { onThresholdChange(it.toLong()) },
                        valueRange = 10f..1200f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentCyan,
                            activeTrackColor = AccentCyan
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onChangeName,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Change Name", color = AccentCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
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

// ── Bar Chart: Y-axis hour scale, gridlines, app icon + name + time ───────────

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
    val capturedColors = barColors.map { it }

    Column(modifier = modifier) {
        // Canvas draws Y-axis labels, horizontal gridlines, and the bars
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val yAxisWidthPx = 36.dp.toPx()
            val chartWidth = size.width - yAxisWidthPx
            val count = data.size
            val totalGap = chartWidth * 0.15f
            val barWidth = (chartWidth - totalGap) / count
            val gap = totalGap / (count + 1)

            val axisPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(160, 150, 165, 185)
                textSize = 28f
                textAlign = android.graphics.Paint.Align.RIGHT
                isAntiAlias = true
            }

            val timePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 26f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            // Draw gridlines + Y-axis hour labels (bottom=0h, top=maxHours)
            for (hour in 0..maxHours) {
                val y = size.height - (hour.toFloat() / maxHours.toFloat()) * size.height
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(yAxisWidthPx, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                // Clamp text baseline inside canvas: labels near the top/bottom get nudged inward
                val textY = (y + axisPaint.textSize / 3f)
                    .coerceIn(axisPaint.textSize, size.height - 2f)
                drawContext.canvas.nativeCanvas.drawText(
                    "${hour}h",
                    yAxisWidthPx - 4f,
                    textY,
                    axisPaint
                )
            }

            // Draw bars scaled against maxHours
            for ((index, value) in data.withIndex()) {
                val barHeight = (value / (maxHours.toFloat() * 3_600_000f)) * size.height
                val x = yAxisWidthPx + gap + index * (barWidth + gap) + barWidth / 2f
                if (barHeight > 0f) {
                    val topY = (size.height - barHeight).coerceAtLeast(0f)
                    drawLine(
                        color = capturedColors[index % capturedColors.size],
                        start = Offset(x, size.height),
                        end = Offset(x, topY),
                        strokeWidth = barWidth * 0.7f,
                        cap = StrokeCap.Butt
                    )
                    
                    // Draw time string above the bar
                    val timeStr = formatMs(value.toLong())
                    val textY = (topY - 8f).coerceAtLeast(timePaint.textSize)
                    drawContext.canvas.nativeCanvas.drawText(timeStr, x, textY, timePaint)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App icon + name row — starts after Y-axis width to align with bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEachIndexed { index, value ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val drawable = icons.getOrNull(index)
                    if (drawable != null) {
                        val labelKey = labels.getOrElse(index) { index.toString() }
                        val bmp = remember(labelKey) { drawableToBitmap(drawable) }
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = labels.getOrElse(index) { "" },
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    barColors[index % barColors.size],
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (labels.isNotEmpty()) {
                        Text(
                            text = labels.getOrElse(index) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            color = SubText,
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
