package pl.szczytomania.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.location.LocationCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import pl.szczytomania.data.model.Peak
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface SummitCheckResult {
    data class Checked(val check: SummitCheck) : SummitCheckResult

    /** Do zdobycia szczytu potrzebna jest dokładna lokalizacja – przybliżona ma błąd ponad 150 m. */
    data object NoPreciseLocation : SummitCheckResult

    /** Lokalizacja jest wyłączona w ustawieniach telefonu. */
    data object LocationOff : SummitCheckResult

    data object NoSignal : SummitCheckResult
}

/** Sprawdza na miejscu, czy użytkownik może zdobyć szczyt. GPS działa bez internetu. */
class SummitChecker(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    suspend fun check(peak: Peak): SummitCheckResult {
        if (context.locationAccess() != LocationAccess.PRECISE) return SummitCheckResult.NoPreciseLocation
        if (!isLocationEnabled()) return SummitCheckResult.LocationOff

        val reading = gpsReadings().bestReading(GPS_TIMEOUT) ?: return SummitCheckResult.NoSignal
        return SummitCheckResult.Checked(SummitCheck.of(peak, reading))
    }

    private fun isLocationEnabled(): Boolean =
        context.getSystemService<LocationManager>()?.let(LocationManagerCompat::isLocationEnabled) == true

    @SuppressLint("MissingPermission") // Uprawnienie sprawdza check().
    private fun gpsReadings(): Flow<GpsReading> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(it.toGpsReading()) }
            }
        }
        // Tylko świeże pozycje: stara z pamięci telefonu mogła powstać w zupełnie innym miejscu.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMaxUpdateAgeMillis(0)
            .build()
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { close(it) }
        awaitClose { client.removeLocationUpdates(callback) }
    }.catch { Log.e(TAG, "Nie udało się odczytać lokalizacji", it) }

    private companion object {
        const val TAG = "SummitChecker"
        const val UPDATE_INTERVAL_MS = 1_000L

        // GPS po włączeniu potrzebuje chwili, a w górach sygnał łapie się wolniej.
        val GPS_TIMEOUT = 30.seconds
    }
}

/**
 * Pierwsze pozycje bywają niedokładne (z sieci, zanim GPS złapie satelity), dlatego czekamy na odczyt
 * mieszczący się w limicie dokładności, a po [timeout] bierzemy najlepszy dotychczasowy.
 * Odczyt symulowany kończy czekanie od razu – nie może go przykryć żaden inny.
 */
internal suspend fun Flow<GpsReading>.bestReading(timeout: Duration): GpsReading? {
    var best: GpsReading? = null
    val good = withTimeoutOrNull(timeout) {
        firstOrNull { reading ->
            if (best.let { it == null || reading.accuracyMeters < it.accuracyMeters }) best = reading
            reading.isMock || reading.accuracyMeters <= SummitRules.MAX_ACCURACY_METERS
        }
    }
    return good ?: best
}

// Pozycja bez podanej dokładności nie może przejść sprawdzenia – Location zwróciłoby wtedy 0.
private fun Location.toGpsReading() = GpsReading(
    lat = latitude,
    lon = longitude,
    accuracyMeters = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
    isMock = LocationCompat.isMock(this),
)
