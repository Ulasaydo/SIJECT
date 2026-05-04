package com.signlanguage.translator.domain.services

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.signlanguage.translator.utils.LogUtils
import java.util.concurrent.Executors

/**
 * Owns CameraX preview and analysis binding for the translation screen.
 */
class CameraService(
    private val context: Context
) {
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private val previewResolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .setResolutionStrategy(
            ResolutionStrategy(
                Size(640, 480),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
            )
        )
        .build()
    private val analysisResolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .setResolutionStrategy(
            ResolutionStrategy(
                Size(160, 120),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
            )
        )
        .build()

    fun startFrontCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onFrame: (ImageProxy) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        startCamera(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            lensFacing = CameraSelector.LENS_FACING_FRONT,
            onFrame = onFrame,
            onError = onError
        )
    }

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int,
        onFrame: (ImageProxy) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                runCatching {
                    cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .setResolutionSelector(previewResolutionSelector)
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                    val analysis = ImageAnalysis.Builder()
                        .setResolutionSelector(analysisResolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                        .also { analyzer ->
                            analyzer.setAnalyzer(analysisExecutor) { imageProxy ->
                                onFrame(imageProxy)
                            }
                        }

                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build(),
                        preview,
                        analysis
                    )
                    LogUtils.d("CameraService", "Camera started with lens facing $lensFacing")
                }.onFailure(onError)
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        LogUtils.d("CameraService", "Camera stopped")
    }

    fun release() {
        stopCamera()
        analysisExecutor.shutdown()
        LogUtils.d("CameraService", "Camera executor shutdown")
    }
}
