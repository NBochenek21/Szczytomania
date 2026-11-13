package pl.szczytomania.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

/** Dłuższy bok zdjęcia po zmniejszeniu – wystarczy na ekran telefonu, a plik ma ok. 200–400 KB. */
const val MAX_PHOTO_SIDE = 1600
private const val JPEG_QUALITY = 80

/**
 * Zmniejsza zdjęcie z aparatu do [MAX_PHOTO_SIDE], obraca je tak, jak było trzymane w ręku,
 * i zapisuje jako JPEG. Nowy plik nie ma metadanych EXIF – nie zdradza np. pozycji GPS ani modelu telefonu.
 * Zwraca zmniejszone zdjęcie do podglądu.
 */
fun shrinkPhoto(source: File, target: File): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(source.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Nie da się odczytać zdjęcia ${source.name}")

    // Pełne zdjęcie z aparatu (np. 4000×3000) zajęłoby w pamięci kilkadziesiąt MB – wczytujemy je od razu
    // pomniejszone (co drugi / co czwarty piksel), a resztę skalujemy dokładnie.
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_PHOTO_SIDE) }
    val decoded = BitmapFactory.decodeFile(source.path, options) ?: throw IOException("Nie da się odczytać zdjęcia ${source.name}")

    val (width, height) = scaledSize(decoded.width, decoded.height, MAX_PHOTO_SIDE)
    val matrix = Matrix().apply {
        postScale(width / decoded.width.toFloat(), height / decoded.height.toFloat())
        postRotate(rotationDegrees(source))
    }
    val photo = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

    // Najpierw plik tymczasowy – przerwany zapis nie zostawi uszkodzonego zdjęcia do wysłania.
    target.parentFile?.mkdirs()
    val temp = File(target.path + ".tmp")
    temp.outputStream().use { photo.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
    if (!temp.renameTo(target)) throw IOException("Nie da się zapisać zdjęcia ${target.name}")
    return photo
}

/** Największa potęga dwójki, przy której wczytane zdjęcie ma dłuższy bok nadal co najmniej [maxSide]. */
internal fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
    var sample = 1
    while (max(width, height) / (sample * 2) >= maxSide) sample *= 2
    return sample
}

/** Wymiary po zmniejszeniu tak, by dłuższy bok miał najwyżej [maxSide] (proporcje bez zmian, bez powiększania). */
internal fun scaledSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
    val longer = max(width, height)
    if (longer <= maxSide) return width to height
    val scale = maxSide.toFloat() / longer
    return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
}

/** Aparat zapisuje zdjęcie "na leżąco" i w EXIF dopisuje, o ile je obrócić. */
private fun rotationDegrees(file: File): Float =
    when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
