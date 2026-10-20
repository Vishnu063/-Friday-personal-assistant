package com.friday.assistant

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.provider.Settings
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

data class FridayCharacter(val id: String, val name: String, val girl: Boolean, val style: Int, val outfit: Int, val hair: Int, val skin: Int)

object FridayCharacters {
    val all = listOf(
        FridayCharacter("kai", "Kai", false, 0, 0xFF72D8CA.toInt(), 0xFF30334C.toInt(), 0xFFF3BD94.toInt()),
        FridayCharacter("leo", "Leo", false, 1, 0xFFFFBB70.toInt(), 0xFF875230.toInt(), 0xFFFFD6B8.toInt()),
        FridayCharacter("avi", "Avi", false, 2, 0xFF83BFFF.toInt(), 0xFF29273A.toInt(), 0xFFBA7D58.toInt()),
        FridayCharacter("noah", "Noah", false, 3, 0xFFBAA1FF.toInt(), 0xFF543536.toInt(), 0xFFE2A77D.toInt()),
        FridayCharacter("finn", "Finn", false, 4, 0xFFB5DA80.toInt(), 0xFFC58443.toInt(), 0xFFFFD6B8.toInt()),
        FridayCharacter("mia", "Mia", true, 0, 0xFFF6A1C4.toInt(), 0xFF4D3042.toInt(), 0xFFF3BD94.toInt()),
        FridayCharacter("lily", "Lily", true, 1, 0xFFB8A3F5.toInt(), 0xFFAA7541.toInt(), 0xFFFFD6B8.toInt()),
        FridayCharacter("tara", "Tara", true, 2, 0xFFFFD578.toInt(), 0xFF29273A.toInt(), 0xFFBA7D58.toInt()),
        FridayCharacter("nia", "Nia", true, 3, 0xFF80DAD4.toInt(), 0xFF403040.toInt(), 0xFF985D42.toInt()),
        FridayCharacter("zoe", "Zoe", true, 4, 0xFFFFA58E.toInt(), 0xFF8C453D.toInt(), 0xFFE2A77D.toInt())
    )
    fun find(id: String?) = all.firstOrNull { it.id == id } ?: all.first()
}

class CharacterPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("friday_character", 0)
    val selected get() = FridayCharacters.find(prefs.getString("selected", null))
    fun select(id: String) { require(FridayCharacters.all.any { it.id == id }); prefs.edit().putString("selected", id).apply() }
    fun clear() { prefs.edit().clear().apply() }
}

/** Original vector artwork shared by the app and overlay; no images, downloads or AI runtime. */
class CharacterView(context: Context) : View(context) {
    var character = FridayCharacters.all.first()
        set(value) { field = value; contentDescription = "${value.name}, Friday companion"; invalidate() }
    var animated = false
        set(value) { field = value; updateAnimation() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private var animator: ValueAnimator? = null
    private fun updateAnimation() {
        val shouldRun = animated && isAttachedToWindow && isShown && ValueAnimator.areAnimatorsEnabled()
        if (shouldRun && animator == null) {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 4000; repeatCount = ValueAnimator.INFINITE
                addUpdateListener { phase = it.animatedValue as Float; invalidate() }
                start()
            }
        } else if (!shouldRun) { animator?.cancel(); animator = null; phase = 0f; invalidate() }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); updateAnimation() }
    override fun onDetachedFromWindow() { animator?.cancel(); animator = null; super.onDetachedFromWindow() }
    override fun onVisibilityAggregated(visible: Boolean) { super.onVisibilityAggregated(visible); updateAnimation() }
    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = character
        val scale = minOf(width, height) / 100f
        canvas.save()
        canvas.translate((width - 100 * scale) / 2, (height - 100 * scale) / 2)
        canvas.scale(scale, scale)
        fun oval(l: Float, t: Float, r: Float, b: Float, color: Int) { paint.color = color; paint.style = Paint.Style.FILL; canvas.drawOval(l,t,r,b,paint) }
        fun round(l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) { paint.color=color; paint.style=Paint.Style.FILL; canvas.drawRoundRect(l,t,r,b,radius,radius,paint) }
        fun circle(x: Float, y: Float, radius: Float, color: Int) { paint.color=color; paint.style=Paint.Style.FILL; canvas.drawCircle(x,y,radius,paint) }
        val dark=0xFF323148.toInt()
        circle(50f,50f,46f,0x203CB9BE)
        oval(28f,92f,72f,97f,0x20323148)
        canvas.translate(0f, if (animated) (kotlin.math.sin(phase * Math.PI * 2) * 1.5).toFloat() else 0f)
        // Rounded boots, tiny body, hands and hoodie.
        round(35f,83f,47f,95f,5f,dark); round(53f,83f,65f,95f,5f,dark)
        round(25f,62f,35f,82f,5f,c.outfit); round(65f,62f,75f,82f,5f,c.outfit)
        circle(30f,81f,5f,c.skin); circle(70f,81f,5f,c.skin)
        round(32f,57f,68f,89f,11f,c.outfit)
        oval(44f,58f,56f,66f,c.skin)
        round(40f,75f,60f,83f,4f,0x35FFFFFF)
        // Different hair silhouettes and accessories.
        if (c.girl) {
            round(25f,15f,75f,66f,20f,c.hair)
            when(c.style) {
                0 -> { circle(24f,45f,10f,c.hair); circle(76f,45f,10f,c.hair) }
                2 -> { circle(24f,59f,9f,c.hair); circle(76f,59f,9f,c.hair) }
                3 -> { circle(33f,13f,11f,c.hair); circle(67f,13f,11f,c.hair) }
                4 -> { round(65f,43f,77f,73f,6f,c.hair) }
            }
        }
        circle(27f,36f,5f,c.skin); circle(73f,36f,5f,c.skin)
        oval(28f,13f,72f,61f,c.skin)
        val fringe = Path().apply {
            moveTo(27f,32f); cubicTo(22f,0f,78f,-1f,74f,33f)
            lineTo(66f,24f); lineTo(61f,29f + c.style); lineTo(52f,22f)
            lineTo(43f,30f - c.style); lineTo(34f,25f); close()
        }
        paint.color=c.hair; canvas.drawPath(fringe,paint)
        if (!c.girl && c.style == 2) { circle(34f,16f,7f,c.hair); circle(45f,11f,8f,c.hair); circle(59f,12f,8f,c.hair) }
        if (!c.girl && c.style == 4) { round(26f,12f,74f,25f,8f,c.outfit); round(53f,21f,80f,26f,3f,c.outfit) }
        if (c.girl) { circle(69f,25f,5f,c.outfit); circle(75f,25f,4f,c.outfit); circle(72f,25f,2f,Color.WHITE) }
        val blink = animated && phase > .91f && phase < .94f
        if (blink) {
            round(35f,39f,43f,41f,1f,dark); round(57f,39f,65f,41f,1f,dark)
        } else {
            oval(35f,33f,44f,45f,dark); oval(56f,33f,65f,45f,dark)
            circle(38f,36f,2f,Color.WHITE); circle(59f,36f,2f,Color.WHITE)
        }
        oval(30f,44f,40f,49f,0x70F28D9C); oval(60f,44f,70f,49f,0x70F28D9C)
        paint.color=dark; paint.style=Paint.Style.STROKE; paint.strokeWidth=1.7f; paint.strokeCap=Paint.Cap.ROUND
        canvas.drawArc(45f,44f,55f,53f,15f,150f,false,paint)
        paint.style=Paint.Style.FILL
        circle(50f,72f,3f,Color.WHITE)
        canvas.restore()
    }
}

@Composable fun FridayCharacterAvatar(id: String, modifier: Modifier = Modifier, animate: Boolean = false) {
    AndroidView(factory = { CharacterView(it) }, modifier = modifier, update = { it.character = FridayCharacters.find(id); it.animated = animate })
}

@Composable fun CharacterPanel(vm: FridayModel, requestNotifications: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var refresh by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    DisposableEffect(lifecycle) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) { refresh++; status = intent?.getStringExtra("status").orEmpty() }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(CharacterOverlayService.ACTION_STATE), ContextCompat.RECEIVER_NOT_EXPORTED)
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); context.unregisterReceiver(receiver) }
    }
    val allowed = remember(refresh) { Settings.canDrawOverlays(context) }
    val running = remember(refresh) { CharacterOverlayService.running }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Choose your Friday companion", style = MaterialTheme.typography.titleMedium)
        Text("Five boys and five girls. Your choice appears in Home and Chat. Start the floating shortcut to keep it on screen after minimizing Friday.")
        OutlinedButton(onClick = { vm.character(FridayCharacters.all.filter { it.id != vm.characterId }.random().id); refresh++ }) { Text("Surprise me — random character") }
        FridayCharacters.all.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { character ->
                    OutlinedButton(onClick = { vm.character(character.id); refresh++ }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            FridayCharacterAvatar(character.id, Modifier.size(76.dp))
                            Text("${if (vm.characterId == character.id) "✓ " else ""}${character.name}")
                            Text(if (character.girl) "Girl" else "Boy", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Text("Drag the character to move it. Tap it to open Friday, then tap the mic or type. It does not listen, read other apps or show private messages. App lock still applies.")
        if (!allowed) OutlinedButton(onClick = {
            runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName))) }
                .onFailure { status = "Open Android Settings → Apps → Special app access → Display over other apps → Friday." }
        }) { Text("Allow display over other apps") }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) TextButton(onClick = {
            if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestNotifications()
            else runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }
                .onFailure { status = "Enable Friday notifications in Android app settings." }
        }) { Text("Enable notifications for the Stop control") }
        Button(onClick = {
            if (running) { context.stopService(Intent(context, CharacterOverlayService::class.java)); status = "Floating character stopped." }
            else status = CharacterOverlayService.start(context)
            refresh++
        }, enabled = allowed || running) { Text(if (running) "Stop floating character" else "Start floating character") }
        Text(if (running) "Floating shortcut is active. Android can hide it on protected screens." else "Floating shortcut is off. Start it here each session; it does not restart on boot.", style = MaterialTheme.typography.bodySmall)
        if (status.isNotBlank()) Text(status)
    }
}
