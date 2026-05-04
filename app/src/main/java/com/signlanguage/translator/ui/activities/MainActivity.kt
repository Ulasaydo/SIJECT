package com.signlanguage.translator.ui.activities

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.signlanguage.translator.R
import com.signlanguage.translator.databinding.ActivityMainBinding
import com.signlanguage.translator.ui.fragments.HistoryFragment
import com.signlanguage.translator.ui.fragments.SettingsFragment
import com.signlanguage.translator.ui.fragments.TranslationFragment

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionHelper: PermissionHelper

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showTranslationFragment()
        } else {
            showPermissionDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionHelper = PermissionHelper(this, requestCameraPermission)
        if (isDemoPipelineMode()) {
            showTranslationFragment()
        } else if (permissionHelper.hasCameraPermission()) {
            showTranslationFragment()
        } else {
            showPermissionDialog()
        }
    }

    private fun showTranslationFragment() {
        if (supportFragmentManager.findFragmentById(binding.fragmentContainer.id) == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, TranslationFragment.newInstance(isDemoPipelineMode()))
                .commit()
        }
    }

    private fun isDemoPipelineMode(): Boolean {
        return intent.getBooleanExtra(EXTRA_DEMO_PIPELINE, false)
    }

    fun openHistory() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, HistoryFragment())
            .addToBackStack("history")
            .commit()
    }

    fun openSettings() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, SettingsFragment())
            .addToBackStack("settings")
            .commit()
    }

    private fun showPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.camera_permission_title)
            .setMessage(R.string.camera_permission_message)
            .setPositiveButton(R.string.camera_permission_button) { _, _ ->
                permissionHelper.requestCameraPermission()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val EXTRA_DEMO_PIPELINE = "demoPipeline"
    }
}
