package twosra.it.exmob_revisi

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class FloatingDetect : AccessibilityService() {
    companion object {
        var isOverlayDetected = false
    }

    private val allowedPackages = listOf(
        "twosra.it.exmob_revisi",
        "com.android.systemui",
        "com.google.android.inputmethod.latin"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        val packageName = event?.packageName?.toString() ?: return

        Log.d("ACCESSIBILITY", packageName)

        isOverlayDetected = packageName !in allowedPackages
    }

    override fun onInterrupt() {

    }
}