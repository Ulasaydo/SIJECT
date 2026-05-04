package com.signlanguage.translator.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.fragment.app.Fragment
import com.signlanguage.translator.R
import com.signlanguage.translator.data.repository.AppSettingsRepository
import com.signlanguage.translator.data.repository.LocalDataRepository
import com.signlanguage.translator.databinding.FragmentSettingsBinding
import java.util.Locale

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)
    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var localDataRepository: LocalDataRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsRepository = AppSettingsRepository(requireContext())
        localDataRepository = LocalDataRepository(requireContext())
        bindCurrentSettings()
        bindActions()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun bindCurrentSettings() {
        val settings = settingsRepository.getSettings()
        binding.frontCameraButton.isChecked = settings.cameraLensFacing == CameraSelector.LENS_FACING_FRONT
        binding.backCameraButton.isChecked = settings.cameraLensFacing == CameraSelector.LENS_FACING_BACK
        binding.confidenceSeekBar.max = AppSettingsRepository.CONFIDENCE_SEEK_STEPS
        binding.confidenceSeekBar.progress =
            AppSettingsRepository.thresholdToProgress(settings.confidenceThreshold)
        binding.overlaySwitch.isChecked = settings.showLandmarkOverlay
        binding.fpsSwitch.isChecked = settings.showFpsCounter
        renderConfidenceLabel(settings.confidenceThreshold)
    }

    private fun bindActions() {
        binding.frontCameraButton.setOnClickListener {
            settingsRepository.setCameraLensFacing(CameraSelector.LENS_FACING_FRONT)
        }
        binding.backCameraButton.setOnClickListener {
            settingsRepository.setCameraLensFacing(CameraSelector.LENS_FACING_BACK)
        }
        binding.confidenceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val threshold = AppSettingsRepository.progressToThreshold(progress)
                renderConfidenceLabel(threshold)
                if (fromUser) {
                    settingsRepository.setConfidenceThreshold(threshold)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val threshold = AppSettingsRepository.progressToThreshold(binding.confidenceSeekBar.progress)
                settingsRepository.setConfidenceThreshold(threshold)
                renderConfidenceLabel(threshold)
            }
        })
        binding.overlaySwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsRepository.setShowLandmarkOverlay(isChecked)
        }
        binding.fpsSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsRepository.setShowFpsCounter(isChecked)
        }
        binding.clearHistoryButton.setOnClickListener {
            localDataRepository.clearHistory()
            Toast.makeText(requireContext(), R.string.history_cleared, Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderConfidenceLabel(threshold: Float) {
        binding.confidenceThresholdText.text = String.format(
            Locale.US,
            "%s: %.2f",
            getString(R.string.confidence_threshold),
            threshold
        )
    }
}
