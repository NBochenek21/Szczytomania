package pl.szczytomania.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.step
import org.maplibre.android.style.expressions.Expression.stop
import org.maplibre.android.style.expressions.Expression.zoom
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textOptional
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import pl.szczytomania.data.model.Peak
import pl.szczytomania.location.distanceMeters
import kotlin.math.roundToInt

/** Warstwa szczytów na mapie: szary trójkąt i nazwa pod nim. */
object PeakLayer {

    private const val SOURCE_ID = "peaks"
    private const val LAYER_ID = "peaks"
    private const val ICON_ID = "peak"

    private const val PROPERTY_ID = "id"
    private const val PROPERTY_NAME = "name"

    // Poniżej tego przybliżenia (widok całej Polski) pokazujemy same trójkąty, bez nazw.
    private const val NAMES_MIN_ZOOM = 7f

    // Trójkąt ma 20 dp – pole stuknięcia jest większe, żeby łatwo było trafić palcem.
    private const val TAP_RADIUS_DP = 24

    // Czcionka musi istnieć na serwerze stylu – "Noto Sans Regular" mają OpenFreeMap i MapTiler.
    private val FONT = arrayOf("Noto Sans Regular")

    /** Dodaje pustą warstwę do świeżo wczytanego stylu. Szczyty ustawia [setPeaks]. */
    fun addTo(style: Style, context: Context) {
        style.addImage(ICON_ID, peakIcon(context.resources.displayMetrics.density))
        style.addSource(GeoJsonSource(SOURCE_ID))
        style.addLayer(
            SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(
                iconImage(ICON_ID),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                textField(step(zoom(), literal(""), stop(NAMES_MIN_ZOOM, get(PROPERTY_NAME)))),
                textFont(FONT),
                textSize(12f),
                textAnchor(Property.TEXT_ANCHOR_TOP),
                textOffset(arrayOf(0f, 0.9f)),
                // Gdy nazwy na siebie nachodzą, chowamy nazwę, ale trójkąt zostaje.
                textOptional(true),
                textColor("#1F2937"),
                textHaloColor("#FFFFFF"),
                textHaloWidth(1.5f),
            ),
        )
    }

    fun setPeaks(style: Style, peaks: List<Peak>) {
        val features = peaks.map { peak ->
            Feature.fromGeometry(Point.fromLngLat(peak.lon, peak.lat)).apply {
                addStringProperty(PROPERTY_ID, peak.id)
                addStringProperty(PROPERTY_NAME, peak.name)
            }
        }
        style.getSourceAs<GeoJsonSource>(SOURCE_ID)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    /** Identyfikator szczytu najbliższego stuknięciu w [point] albo null, gdy stuknięto obok. */
    fun peakIdAt(map: MapLibreMap, point: LatLng, density: Float): String? {
        val tap = map.projection.toScreenLocation(point)
        val slop = TAP_RADIUS_DP * density
        return map.queryRenderedFeatures(RectF(tap.x - slop, tap.y - slop, tap.x + slop, tap.y + slop), LAYER_ID)
            .minByOrNull { feature ->
                val peak = feature.geometry() as Point
                distanceMeters(point.latitude, point.longitude, peak.latitude(), peak.longitude())
            }
            ?.getStringProperty(PROPERTY_ID)
    }

    private fun peakIcon(density: Float): Bitmap {
        val size = (20 * density).roundToInt()
        val stroke = 1.5f * density
        val triangle = Path().apply {
            moveTo(size / 2f, stroke)
            lineTo(size - stroke, size - stroke)
            lineTo(stroke, size - stroke)
            close()
        }
        return createBitmap(size, size).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawPath(triangle, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PEAK_COLOR
                style = Paint.Style.FILL
            })
            canvas.drawPath(triangle, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = OUTLINE_COLOR
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeJoin = Paint.Join.ROUND
            })
        }
    }

    // Szary = szczyt do zdobycia (legenda z mockupu mapy).
    private const val PEAK_COLOR = 0xFF6B7280.toInt()
    private const val OUTLINE_COLOR = 0xFFFFFFFF.toInt()
}
