package pl.szczytomania.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import pl.szczytomania.camera.ConquestPhotos
import pl.szczytomania.data.firestore.ConquestRepository
import pl.szczytomania.data.storage.PhotoStorage
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Wysyła zdjęcie zdobyczy w tle. WorkManager uruchamia je dopiero przy dostępie do internetu – także
 * po zamknięciu aplikacji czy restarcie telefonu – i ponawia po błędzie. Po wysłaniu zapisuje link
 * w zdobyczy (photoStatus "uploaded").
 */
class PhotoUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = inputData.getString(KEY_UID) ?: return Result.failure()
        val peakId = inputData.getString(KEY_PEAK_ID) ?: return Result.failure()
        val file = ConquestPhotos.photoFile(applicationContext, uid, peakId)
        if (!file.exists()) {
            Log.w(TAG, "Brak zdjęcia $peakId w telefonie")
            return Result.failure()
        }
        // Reguły pozwalają wysłać zdjęcie tylko właścicielowi – po wylogowaniu poczeka, aż znowu się zaloguje.
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return Result.retry()

        return try {
            val url = PhotoStorage().upload(uid, peakId, file)
            ConquestRepository().markPhotoUploaded(peakId, url)
            // Aplikacja mogła być zamknięta: czekamy, aż Firestore wyśle link (i samą zdobycz, jeśli czekała
            // w telefonie), zanim system znowu ją uśpi.
            withTimeoutOrNull(TimeUnit.MINUTES.toMillis(1)) { FirebaseFirestore.getInstance().waitForPendingWrites().await() }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Nie udało się wysłać zdjęcia $peakId – spróbujemy ponownie", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "PhotoUploadWorker"
        private const val KEY_UID = "uid"
        private const val KEY_PEAK_ID = "peakId"

        fun enqueue(context: Context, uid: String, peakId: String) {
            val request = OneTimeWorkRequestBuilder<PhotoUploadWorker>()
                .setInputData(Data.Builder().putString(KEY_UID, uid).putString(KEY_PEAK_ID, peakId).build())
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            // Jedno zadanie na zdobycz – zostawiamy to, które już czeka.
            WorkManager.getInstance(context).enqueueUniqueWork(
                "photo-$uid-${ConquestRepository.documentId(peakId)}",
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
