package com.friday.assistant

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** No event-based screen harvesting. A read requires a fresh unlock + user button tap. */
class ScreenService : AccessibilityService() {
    companion object {
        var instance: ScreenService? = null
            private set
        var preview by mutableStateOf("")
        var readUntil = 0L
        fun arm() { preview = ""; readUntil = SystemClock.elapsedRealtime() + 60000 }
        fun cancelRead() { readUntil = 0; preview = "" }
    }
    private val buttonCallback = object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) {
            if (SystemClock.elapsedRealtime() >= readUntil) return
            readUntil = 0
            val root = rootInActiveWindow
            val excluded = root?.packageName?.toString() in setOf(packageName, "com.android.systemui")
            preview = if (root == null || excluded) "No readable app text. Open the target app and try again." else read(root)
            startActivity(Intent(this@ScreenService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
    }
    override fun onServiceConnected() {
        instance = this
        serviceInfo = serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON }
        accessibilityButtonController.registerAccessibilityButtonCallback(buttonCallback)
    }
    private fun read(root: AccessibilityNodeInfo): String {
        val chunks = linkedSetOf<String>()
        var visited = 0
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (++visited > 500 || depth > 30 || node.isPassword || !node.isVisibleToUser) return
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { chunks.add(it.take(1000)) }
            if (node.text.isNullOrBlank()) node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { chunks.add(it.take(1000)) }
            for (i in 0 until node.childCount.coerceAtMost(100)) node.getChild(i)?.let { walk(it, depth + 1) }
        }
        walk(root, 0)
        return chunks.joinToString("\n").take(6000).ifBlank { "This app does not expose readable text. Try importing a screenshot in Tools." }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { cancelRead() }
    override fun onDestroy() {
        accessibilityButtonController.unregisterAccessibilityButtonCallback(buttonCallback)
        instance = null; cancelRead(); super.onDestroy()
    }
}
