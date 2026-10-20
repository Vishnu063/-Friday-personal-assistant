package com.friday.assistant

import android.Manifest
import android.app.*
import android.app.AppOpsManager
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

object CharacterPosition {
    fun clamp(value: Int, extent: Int, item: Int): Int = value.coerceIn(0, (extent - item).coerceAtLeast(0))
}

/** User-started, stoppable shortcut. It has no microphone, screen-reader or model reference. */
class CharacterOverlayService : Service() {
    companion object {
        const val ACTION_STATE = "com.friday.assistant.CHARACTER_STATE"
        const val ACTION_STOP = "com.friday.assistant.CHARACTER_STOP"
        private const val CHANNEL = "friday_character"
        private const val NOTIFICATION = 704
        @Volatile var running = false; private set
        fun start(context: Context): String {
            if (!Settings.canDrawOverlays(context)) return "Allow Display over other apps first."
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled() ||
                (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED))
                return "Enable Friday notifications first so the floating shortcut has a visible Stop control."
            return try {
                ContextCompat.startForegroundService(context, Intent(context, CharacterOverlayService::class.java))
                "Starting floating character…"
            } catch (_: Exception) { "Android could not start the floating character. Reopen Friday and try again." }
        }
        fun refresh(context: Context) {
            if (running) runCatching { context.startService(Intent(context, CharacterOverlayService::class.java)) }
        }
    }
    private lateinit var windows: WindowManager
    private var bubble: LinearLayout? = null
    private var avatar: CharacterView? = null
    private var registered = false
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.LEFT }
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { updateVisibility(intent?.action == Intent.ACTION_SCREEN_OFF) }
    }
    private val permissionListener = AppOpsManager.OnOpChangedListener { op, _ ->
        if (op == AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW && !Settings.canDrawOverlays(this))
            android.os.Handler(mainLooper).post { stopSelf() }
    }
    private fun state(message: String) { sendBroadcast(Intent(ACTION_STATE).setPackage(packageName).putExtra("status", message)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onCreate() {
        super.onCreate()
        windows = getSystemService(WindowManager::class.java)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        if (!Settings.canDrawOverlays(this) || !NotificationManagerCompat.from(this).areNotificationsEnabled()) { stopSelf(); return START_NOT_STICKY }
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Floating Friday companion", NotificationManager.IMPORTANCE_LOW))
            val open = PendingIntent.getActivity(this, 704, talkIntent(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val stop = PendingIntent.getService(this, 705, Intent(this, CharacterOverlayService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_friday).setContentTitle("Friday companion is on screen")
                .setContentText("Tap the character to open Friday. Stop hides it.")
                .setContentIntent(open).setOngoing(true).setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(Notification.Action.Builder(null, "Stop", stop).build()).build()
            if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(NOTIFICATION, notification)
            if (bubble == null) createBubble()
            avatar?.character = CharacterPreferences(this).selected
            running = true
            updateVisibility()
            state("Floating character active. Minimize Friday to use it over your other apps.")
        } catch (_: Exception) { state("Android could not show the floating character. Check overlay access and reopen Friday."); stopSelf() }
        return START_NOT_STICKY
    }
    private fun talkIntent() = Intent(this, MainActivity::class.java).setAction("com.friday.assistant.TALK")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    private fun createBubble() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(2),0,dp(2),0)
        }
        val character = CharacterView(this).apply {
            this.character = CharacterPreferences(this@CharacterOverlayService).selected
            animated = true
            isClickable = true
            setOnClickListener { runCatching { startActivity(talkIntent()) }.onFailure { state("Open Friday from its notification or app icon.") } }
        }
        root.addView(character, LinearLayout.LayoutParams(dp(88),dp(88)))
        root.addView(TextView(this).apply {
            text = "Friday"; setTextColor(android.graphics.Color.WHITE); textSize=12f
            gravity=Gravity.CENTER; setShadowLayer(3f,0f,1f,android.graphics.Color.BLACK)
            importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(88),dp(20)))
        root.addView(Button(this).apply {
            text="Hide"; contentDescription="Stop floating Friday character"; textSize=12f
            setOnClickListener { stopSelf() }
        }, LinearLayout.LayoutParams(dp(88),dp(48)))
        var downX=0f; var downY=0f; var initialX=0; var initialY=0; var dragged=false
        val slop=ViewConfiguration.get(this).scaledTouchSlop
        character.setOnTouchListener { view,event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX=event.rawX; downY=event.rawY; initialX=params.x; initialY=params.y; dragged=false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx=event.rawX-downX; val dy=event.rawY-downY
                    if (abs(dx)>slop || abs(dy)>slop) dragged=true
                    if (dragged) { params.x=initialX+dx.toInt(); params.y=initialY+dy.toInt(); place() }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!dragged) view.performClick(); true }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        params.width=dp(92); params.height=dp(156)
        params.x=dp(12); params.y=dp(120)
        windows.addView(root,params)
        bubble=root; avatar=character
        ContextCompat.registerReceiver(this, screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        registered=true
        getSystemService(AppOpsManager::class.java).startWatchingMode(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, packageName, permissionListener)
        place()
    }
    private fun place() {
        val root=bubble ?: return
        val metrics=resources.displayMetrics
        params.x=CharacterPosition.clamp(params.x,metrics.widthPixels,params.width)
        params.y=CharacterPosition.clamp(params.y,metrics.heightPixels-dp(72),params.height)
        runCatching { windows.updateViewLayout(root,params) }.onFailure { stopSelf() }
    }
    private fun updateVisibility(screenOff: Boolean = false) {
        val hidden=screenOff || !getSystemService(PowerManager::class.java).isInteractive ||
            getSystemService(KeyguardManager::class.java).isKeyguardLocked
        bubble?.visibility=if(hidden) View.GONE else View.VISIBLE
        avatar?.animated=!hidden
    }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); place() }
    override fun onTaskRemoved(rootIntent: Intent?) { stopSelf(); super.onTaskRemoved(rootIntent) }
    override fun onDestroy() {
        running=false
        if (registered) { runCatching { unregisterReceiver(screenReceiver) }; runCatching { getSystemService(AppOpsManager::class.java).stopWatchingMode(permissionListener) } }
        avatar?.animated=false
        bubble?.let { runCatching { windows.removeViewImmediate(it) } }
        bubble=null; avatar=null
        stopForeground(STOP_FOREGROUND_REMOVE)
        state("Floating character stopped.")
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
