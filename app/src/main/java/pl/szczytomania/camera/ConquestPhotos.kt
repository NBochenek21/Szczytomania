package pl.szczytomania.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import pl.szczytomania.data.firestore.ConquestRepository
import pl.szczytomania.data.storage.PhotoStorage
import pl.szczytomania.work.PhotoUploadWorker
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Zdjęcie zdobyczy: zrobienie zdjęcia, zmniejszanie i wysyłanie w tle.
 *
 * Zmniejszone zdjęcie leży w pamięci aplikacji (`files/photos/{uid}/{id szczytu}.jpg`), a nie w cache –
 * system nie usunie go, zanim w górach bez zasięgu doczeka się wysłania.
 */
class ConquestPhotos(
    context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val context = context.applicationContext

    /** Tu aparat zapisuje pełne zdjęcie – jedno naraz, więc stała nazwa. */
    private val captureFile get() = File(context.cacheDir, "camera/capture.jpg").apply { parentFile?.mkdirs() }

    /** Robi zdjęcie aparatem z podglądu ([CameraViewfinder]), zmniejsza je do pliku zdobyczy i zwraca do podglądu. */
    suspend fun capture(imageCapture: ImageCapture, peakId: String): Bitmap {
        val uid = checkNotNull(auth.currentUser?.uid) { "Zdjęcie bez zalogowanego użytkownika" }
        val source = captureFile
        suspendCancellableCoroutine { cont ->
            imageCapture.takePicture(
                ImageCapture.OutputFileOptions.Builder(source).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) = cont.resume(Unit)
                    override fun onError(exception: ImageCaptureException) = cont.resumeWithException(exception)
                },
            )
        }
        return withContext(Dispatchers.IO) {
            shrinkPhoto(source, photoFile(context, uid, peakId)).also { source.delete() }
        }
    }

    /** Zleca wysłanie zdjęcia w tle – poczeka na zasięg, także po zamknięciu aplikacji. */
    fun upload(peakId: String) {
        val uid = auth.currentUser?.uid ?: return
        PhotoUploadWorker.enqueue(context, uid, peakId)
    }

    /**
     * Zdjęcie zdobyczy do pokazania: z telefonu, a gdy go tu nie ma (inny telefon, ponowna instalacja) –
     * pobrane z Cloud Storage i zapamiętane. null, gdy zdjęcia nie ma albo nie da się go teraz pobrać.
     */
    suspend fun load(peakId: String, uploaded: Boolean): Bitmap? = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext null
        val file = photoFile(context, uid, peakId)
        if (!file.exists() && uploaded) {
            try {
                PhotoStorage().download(uid, peakId, file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Nie udało się pobrać zdjęcia $peakId", e)
            }
        }
        if (file.exists()) BitmapFactory.decodeFile(file.path) else null
    }

    companion object {
        private const val TAG = "ConquestPhotos"

        fun photoFile(context: Context, uid: String, peakId: String) =
            File(context.filesDir, "photos/$uid/${ConquestRepository.documentId(peakId)}.jpg")
    }
}
