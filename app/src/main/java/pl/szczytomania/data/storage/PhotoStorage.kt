package pl.szczytomania.data.storage

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import pl.szczytomania.data.firestore.ConquestRepository
import java.io.File
import java.io.IOException

/** Zdjęcia zdobyczy w Cloud Storage: `users/{uid}/conquests/{id szczytu}.jpg` (reguły: firebase/storage.rules). */
class PhotoStorage(private val storage: FirebaseStorage = FirebaseStorage.getInstance()) {

    /** Wysyła zdjęcie i zwraca link do niego – trafia do zdobyczy jako `photoUrl`. */
    suspend fun upload(uid: String, peakId: String, file: File): String {
        val ref = photoRef(uid, peakId)
        // Typ pliku sprawdzają reguły Storage – bez niego zdjęcie zostałoby odrzucone.
        ref.putFile(Uri.fromFile(file), StorageMetadata.Builder().setContentType("image/jpeg").build()).await()
        return ref.downloadUrl.await().toString()
    }

    /** Pobiera wysłane zdjęcie do pliku, np. na nowym telefonie albo po ponownej instalacji aplikacji. */
    suspend fun download(uid: String, peakId: String, target: File) {
        target.parentFile?.mkdirs()
        // Najpierw plik tymczasowy – przerwane pobieranie nie zostawi uszkodzonego zdjęcia.
        val temp = File(target.path + ".tmp")
        photoRef(uid, peakId).getFile(temp).await()
        if (!temp.renameTo(target)) throw IOException("Nie da się zapisać zdjęcia ${target.name}")
    }

    private fun photoRef(uid: String, peakId: String) =
        storage.reference.child("users/$uid/conquests/${ConquestRepository.documentId(peakId)}.jpg")
}
