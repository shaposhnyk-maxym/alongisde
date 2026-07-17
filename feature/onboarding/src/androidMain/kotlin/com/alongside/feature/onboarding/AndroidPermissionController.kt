package com.alongside.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private const val PREFS_NAME = "onboarding_permissions"

/**
 * Android's real [PermissionController]: ActivityCompat/ActivityResultContracts runtime
 * permissions. The native permission dialog this drives is a system dialog and is deliberately
 * not unit-tested (M6 accept criteria) - only OnboardingContainer's handling of the resulting
 * [PermissionStatus] is.
 *
 * Must be constructed in [activity]'s `onCreate`, before `setContent` - [registerForActivityResult]
 * requires that timing, the same constraint `CredentialManagerGoogleAuthProvider` already relies on.
 */
public class AndroidPermissionController(
    private val activity: ComponentActivity,
) : PermissionController {
    private val prefs: SharedPreferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var pendingPermission: OnboardingPermission? = null
    private var pendingCallback: ((PermissionStatus) -> Unit)? = null

    private val launcher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val permission = pendingPermission
            val callback = pendingCallback
            pendingPermission = null
            pendingCallback = null
            if (permission != null) {
                callback?.invoke(if (granted) PermissionStatus.GRANTED else status(permission))
            }
        }

    override fun status(permission: OnboardingPermission): PermissionStatus {
        val manifestPermission = manifestPermission(permission) ?: return PermissionStatus.GRANTED
        val granted =
            ContextCompat.checkSelfPermission(activity, manifestPermission) == PackageManager.PERMISSION_GRANTED
        return when {
            granted -> PermissionStatus.GRANTED
            !wasAsked(permission) -> PermissionStatus.NOT_DETERMINED
            ActivityCompat.shouldShowRequestPermissionRationale(activity, manifestPermission) -> PermissionStatus.DENIED
            else -> PermissionStatus.DENIED_PERMANENTLY
        }
    }

    override fun request(
        permission: OnboardingPermission,
        onResult: (PermissionStatus) -> Unit,
    ) {
        val manifestPermission = manifestPermission(permission)
        if (manifestPermission == null) {
            onResult(PermissionStatus.GRANTED)
            return
        }
        // Written before launching, not after the result returns, so a process death mid-dialog
        // still leaves `status()` able to tell NOT_DETERMINED apart from a real prior denial.
        markAsked(permission)
        pendingPermission = permission
        pendingCallback = onResult
        launcher.launch(manifestPermission)
    }

    override fun openAppSettings() {
        val intent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.packageName, null),
            )
        activity.startActivity(intent)
    }

    private fun wasAsked(permission: OnboardingPermission): Boolean = prefs.getBoolean(askedKey(permission), false)

    private fun markAsked(permission: OnboardingPermission) {
        prefs.edit().putBoolean(askedKey(permission), true).apply()
    }

    private fun askedKey(permission: OnboardingPermission): String =
        when (permission) {
            OnboardingPermission.PHOTOS -> "asked_photos"
            OnboardingPermission.NOTIFICATIONS -> "asked_notifications"
        }

    // null means no runtime permission applies on this OS version - callers treat that as GRANTED.
    private fun manifestPermission(permission: OnboardingPermission): String? =
        when (permission) {
            OnboardingPermission.PHOTOS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            OnboardingPermission.NOTIFICATIONS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    null
                }
        }
}
