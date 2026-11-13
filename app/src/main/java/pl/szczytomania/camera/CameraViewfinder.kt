package pl.szczytomania.camera

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

/**
 * Podgląd na żywo z tylnego aparatu (mockup 04). Zdjęcie robi [imageCapture] – dzięki ViewPort
 * ma ten sam kadr, co widać w ramce. Aparat działa tylko, gdy podgląd jest na ekranie.
 */
@Composable
fun CameraViewfinder(imageCapture: ImageCapture, onError: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            // TextureView zamiast SurfaceView – podgląd da się przyciąć do zaokrąglonej ramki i przewijać.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)

    LaunchedEffect(previewView, lifecycleOwner, imageCapture) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
        val useCases = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageCapture)
            .apply { previewView.awaitViewPort()?.let(::setViewPort) }
            .build()
        try {
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCases)
            awaitCancellation()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Np. telefon albo emulator bez tylnego aparatu.
            Log.e(TAG, "Nie udało się uruchomić aparatu", e)
            onError()
        } finally {
            provider.unbind(preview, imageCapture)
        }
    }
}

/** Kadr podglądu (proporcje i obrót) – znany dopiero, gdy widok ma wymiary na ekranie. */
private suspend fun PreviewView.awaitViewPort(): ViewPort? = suspendCancellableCoroutine { cont ->
    doOnLayout { cont.resume((it as PreviewView).viewPort) }
}

private const val TAG = "CameraViewfinder"
