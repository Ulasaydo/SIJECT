package com.signlanguage.translator.ui.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import com.signlanguage.translator.utils.PermissionUtils

class PermissionHelper(
    private val activity: Activity,
    private val requestPermissionLauncher: ActivityResultLauncher<String>
) {
    fun hasCameraPermission(): Boolean = PermissionUtils.hasCameraPermission(activity)

    fun requestCameraPermission() {
        requestPermissionLauncher.launch(PermissionUtils.CAMERA_PERMISSION)
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
}
