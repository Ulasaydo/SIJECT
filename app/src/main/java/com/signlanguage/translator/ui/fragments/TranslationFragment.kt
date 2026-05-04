package com.signlanguage.translator.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.signlanguage.translator.R
import com.signlanguage.translator.data.repository.AppSettingsRepository
import com.signlanguage.translator.databinding.FragmentTranslationBinding
import com.signlanguage.translator.domain.services.CameraService
import com.signlanguage.translator.ui.activities.MainActivity
import com.signlanguage.translator.ui.viewmodels.TranslationViewModel
import com.signlanguage.translator.utils.asPercentText

class TranslationFragment : Fragment() {
    private var _binding: FragmentTranslationBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: TranslationViewModel by viewModels()
    private lateinit var cameraService: CameraService
    private lateinit var settingsRepository: AppSettingsRepository
    private var activeLensFacing: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranslationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraService = CameraService(requireContext())
        settingsRepository = AppSettingsRepository(requireContext())
        observeState()
        bindActions()
        if (isDemoPipelineMode()) {
            viewModel.runLandmarkPipelineDemo()
        } else {
            viewModel.checkBackendHealth()
            startCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        if (
            !isDemoPipelineMode() &&
            _binding != null &&
            ::settingsRepository.isInitialized &&
            ::cameraService.isInitialized
        ) {
            viewModel.refreshSettings()
            restartCameraIfNeeded()
        }
    }

    override fun onDestroyView() {
        if (::cameraService.isInitialized) {
            cameraService.release()
        }
        _binding = null
        super.onDestroyView()
    }

    private fun observeState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.candidateOneText.text = state.recognizedWords.getOrNull(0).asCandidateText(1)
            binding.candidateTwoText.text = state.recognizedWords.getOrNull(1).asCandidateText(2)
            binding.candidateThreeText.text = state.recognizedWords.getOrNull(2).asCandidateText(3)
            binding.sentenceText.text = state.translatedSentence
            binding.translationProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.apiStatusText.visibility = if (
                state.errorMessage != null ||
                state.apiError != null ||
                state.backendOnline != null
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.apiStatusText.text = state.errorMessage
                ?: state.apiError
                ?: state.backendOnline?.let { online ->
                    getString(if (online) R.string.backend_online else R.string.backend_offline)
                }.orEmpty()
            binding.confidenceBar.setConfidence(state.recognizedWords.firstOrNull()?.confidence ?: 0f)
            val showOverlay = state.debugMode && state.showLandmarkOverlay
            binding.landmarkOverlay.visibility = if (showOverlay) View.VISIBLE else View.GONE
            binding.landmarkOverlay.setMirrorHorizontally(activeLensFacing == CameraSelector.LENS_FACING_FRONT)
            binding.landmarkOverlay.setSourceImageSize(state.sourceImageWidth, state.sourceImageHeight)
            binding.landmarkOverlay.submitLandmarks(if (showOverlay) state.landmarks else emptyList())
            binding.fpsText.text = getString(R.string.fps_counter, state.fps)
            binding.fpsText.visibility = if (state.showFpsCounter) View.VISIBLE else View.GONE
            binding.latencyText.text = getString(
                R.string.latency_counter,
                state.landmarkTimeMillis,
                state.inferenceTimeMillis
            )
        }
    }

    private fun bindActions() {
        binding.settingsButton.setOnClickListener {
            (requireActivity() as MainActivity).openSettings()
        }
        binding.historyButton.setOnClickListener {
            (requireActivity() as MainActivity).openHistory()
        }
        binding.debugButton.setOnClickListener {
            viewModel.toggleDebugMode()
        }
        binding.refreshTranslationButton.setOnClickListener {
            viewModel.refreshTranslation()
        }
        binding.pipelineDemoButton.setOnClickListener {
            runPipelineDemoFromButton()
        }
        binding.playTtsButton.setOnClickListener {
            if (hasTranslatedSentence()) {
                viewModel.speakCurrentSentence()
            } else {
                Toast.makeText(requireContext(), R.string.no_translation_to_play, Toast.LENGTH_SHORT).show()
            }
        }
        binding.copyTranslationButton.setOnClickListener {
            copyTranslatedSentence()
        }
    }

    private fun startCamera() {
        val settings = settingsRepository.getSettings()
        activeLensFacing = settings.cameraLensFacing
        cameraService.startCamera(
            lifecycleOwner = viewLifecycleOwner,
            previewView = binding.previewView,
            lensFacing = settings.cameraLensFacing,
            onFrame = viewModel::processFrame,
            onError = { throwable ->
                Toast.makeText(requireContext(), throwable.message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun restartCameraIfNeeded() {
        val lensFacing = settingsRepository.getSettings().cameraLensFacing
        if (activeLensFacing == null) {
            startCamera()
            return
        }
        if (activeLensFacing != lensFacing) {
            cameraService.stopCamera()
            startCamera()
        }
    }

    private fun runPipelineDemoFromButton() {
        if (::cameraService.isInitialized) {
            cameraService.stopCamera()
            activeLensFacing = null
        }
        viewModel.runLandmarkPipelineDemo()
    }

    private fun com.signlanguage.translator.data.model.PredictionCandidate?.asCandidateText(rank: Int): String {
        return this?.let { "$rank. ${it.label} (${it.confidence.asPercentText()})" }
            ?: getString(R.string.candidate_placeholder)
    }

    private fun copyTranslatedSentence() {
        val sentence = binding.sentenceText.text?.toString().orEmpty()
        if (!hasTranslatedSentence(sentence)) {
            Toast.makeText(requireContext(), R.string.no_translation_to_copy, Toast.LENGTH_SHORT).show()
            return
        }

        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(getString(R.string.translation_clip_label), sentence)
        )
        Toast.makeText(requireContext(), R.string.translation_copied, Toast.LENGTH_SHORT).show()
    }

    private fun hasTranslatedSentence(
        sentence: String = binding.sentenceText.text?.toString().orEmpty()
    ): Boolean {
        return sentence.isNotBlank() && sentence != getString(R.string.translated_sentence_placeholder)
    }

    private fun isDemoPipelineMode(): Boolean {
        return arguments?.getBoolean(ARG_DEMO_PIPELINE, false) == true
    }

    companion object {
        private const val ARG_DEMO_PIPELINE = "demoPipeline"

        fun newInstance(demoPipeline: Boolean = false): TranslationFragment {
            return TranslationFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_DEMO_PIPELINE, demoPipeline)
                }
            }
        }
    }
}
