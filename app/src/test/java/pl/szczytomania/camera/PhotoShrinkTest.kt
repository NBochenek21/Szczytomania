package pl.szczytomania.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoShrinkTest {

    @Test
    fun smallPhotoIsNotEnlarged() {
        assertEquals(1200 to 900, scaledSize(1200, 900, MAX_PHOTO_SIDE))
        assertEquals(1, sampleSize(1200, 900, MAX_PHOTO_SIDE))
    }

    @Test
    fun longerSideIsLimitedAndProportionsKept() {
        assertEquals(1600 to 1200, scaledSize(4000, 3000, MAX_PHOTO_SIDE))
        // Zdjęcie pionowe.
        assertEquals(1200 to 1600, scaledSize(3000, 4000, MAX_PHOTO_SIDE))
    }

    @Test
    fun sampleSizeLeavesEnoughPixelsForFinalScaling() {
        // 12 Mpx: wczytane 2000×1500, potem dokładnie do 1600×1200.
        assertEquals(2, sampleSize(4000, 3000, MAX_PHOTO_SIDE))
        // 50 Mpx: wczytane 2040×1530.
        assertEquals(4, sampleSize(8160, 6120, MAX_PHOTO_SIDE))
        // Co drugi piksel dałby mniej niż 1600 – wczytujemy całe.
        assertEquals(1, sampleSize(3199, 2000, MAX_PHOTO_SIDE))
    }
}
