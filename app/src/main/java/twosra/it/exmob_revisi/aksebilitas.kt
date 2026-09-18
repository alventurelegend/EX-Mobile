package twosra.it.exmob_revisi

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo

class Aksesibilitas(
    private val context: Context
) {

    private var dialog: AlertDialog? = null

    private val whitelistPackages = setOf(
        "com.google.android.marvin.talkback",
        "com.google.android.accessibility.talkback",
        "com.samsung.android.accessibility.talkback"
    )

    fun cekAccessibility(): Boolean {

        val manager =
            context.getSystemService(
                Context.ACCESSIBILITY_SERVICE
            ) as AccessibilityManager

        val services =
            manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )

        for (service in services) {

            val packageName =
                service.resolveInfo.serviceInfo.packageName

            // Accessibility bawaan yang diizinkan
            if (packageName in whitelistPackages) {
                continue
            }

            // Accessibility aplikasi sistem → diizinkan
            if (isSystemApp(packageName)) {
                continue
            }

            // Accessibility pihak ketiga ditemukan
            return true
        }

        return false
    }

    private fun isSystemApp(packageName: String): Boolean {

        return try {

            val appInfo =
                context.packageManager.getApplicationInfo(
                    packageName,
                    0
                )

            val systemApp =
                (appInfo.flags and
                        ApplicationInfo.FLAG_SYSTEM) != 0

            val updatedSystemApp =
                (appInfo.flags and
                        ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            systemApp || updatedSystemApp

        } catch (e: Exception) {

            false
        }
    }

    fun tampilkanDialog() {

        // Jangan membuat dialog kedua
        if (dialog?.isShowing == true) {
            return
        }

        val activity = context as? Activity
            ?: return

        // Pastikan Activity masih aktif
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        dialog = AlertDialog.Builder(activity)
            .setTitle("Aksesibilitas Terdeteksi")
            .setMessage(
                "Terdeteksi aplikasi pihak ketiga yang " +
                        "menggunakan layanan Aksesibilitas.\n\n" +
                        "Untuk melanjutkan, silakan matikan " +
                        "Aksesibilitas aplikasi tersebut terlebih dahulu."
            )
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                bukaPengaturanAccessibility()
            }
            .create()

        // Tidak bisa ditutup dengan tap di luar
        dialog?.setCanceledOnTouchOutside(false)

        // Tampilkan di atas Activity aplikasi
        dialog?.show()
    }

    private fun bukaPengaturanAccessibility() {

        val intent =
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

        context.startActivity(intent)
    }

    fun destroy() {

        dialog?.dismiss()
        dialog = null
    }
}