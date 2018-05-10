package de.tritrack.recording.recording

import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition

/**
 * Created by till on 22.12.16.
 */

enum class ActivityFeature(val description: String, val unit: String, private val mFormat: Format) {
    TIME_S("Duration", "", TimeFormatter(true)),
    LATITUDE("Lat", "deg", DecimalFormat("#.#######")),
    LONGITUDE("Lon", "deg", DecimalFormat("#.#######")),
    ALTITUDE("Altitude", "m", DecimalFormat("#")),
    DISTANCE_INCREMENT_M("Distance increment", "m", DecimalFormat("#")),
    DISTANCE_M("Distance", "m", DecimalFormat("#")),
    DISTANCE_KM("Distance", "km", DecimalFormat("#.##")),
    DISTANCE_KM_REV("Distance (REV)", "km", DecimalFormat("#.##")),
    ELEVATION_GAIN("Elevation gain", "m", DecimalFormat("#")),
    SPEED_MS("Speed", "m/s", DecimalFormat("#.#")),
    SPEED_KMH("Speed", "km/h", DecimalFormat("#.#")),
    SPEED_KMH_REV("Speed", "km/h", DecimalFormat("#.#")),
    AVG_SPEED_KMH("Avg Speed", "km/h", DecimalFormat("#.#")),
    MAX_SPEED_KMH("Max Speed", "km/h", DecimalFormat("#.#")),
    PACE("Pace", "min/km", TimeFormatter(false)),
    AVG_PACE("Avg Pace", "min/km", TimeFormatter(false)),
    HEART_RATE("Heart Rate", "bpm", DecimalFormat("#")),
    AVG_HEART_RATE("Avg Heart Rate", "bpm", DecimalFormat("#")),
    MAX_HEART_RATE("Max Heart Rate", "bpm", DecimalFormat("#")),
    POWER_LEFT("Power left", "W", DecimalFormat("#.#")),
    POWER_RIGHT("Power right", "W", DecimalFormat("#.#")),
    POWER_COMBINED("Power combined", "W", DecimalFormat("#.#")),
    AVG_POWER_COMBINED("Avg Power combined", "W", DecimalFormat("#")),
    MAX_POWER_COMBINED("Max Power combined", "W", DecimalFormat("#")),
    CUMULATIVE_WHEEL_REVOLUTIONS("Cumulative Wheel Revolutions", "#", DecimalFormat("#")),
    LAST_WHEEL_EVENT("Last Wheel Event", "1/1024s", DecimalFormat("#")),
    CUMULATIVE_CRANK_REVOLUTIONS("Cumulative Crank Revolutions", "#", DecimalFormat("#")),
    LAST_CRANK_EVENT("Last Crank Event", "1/1024s", DecimalFormat("#")),
    CADENCE("Cadence", "rpm", DecimalFormat("#")),
    AVG_CADENCE("Avg Cadence", "rpm", DecimalFormat("#")),
    AVG_NORM_POWER("Avg Power (norm)", "W", DecimalFormat("#")),
    AVG_NORM_CADENCE("Avg Cadence (norm)", "rpm", DecimalFormat("#")),
    SMOOTH_POWER("Power (3s)", "W", DecimalFormat("#.#"));

    fun format(`val`: Double?): String {
        return mFormat.format(`val`)
    }

    // TODO: this is a workaround
    private class TimeFormatter(private val mIncludeHours: Boolean) : Format() {

        override fun format(number: Any, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
            // it actually is a double
            val secD = number as Double
            val s = secD.toInt()
            var duration = String.format("%02d:%02d", s % 3600 / 60, s % 60)
            if (mIncludeHours)
                duration = String.format("%d:", s / 3600) + duration
            toAppendTo.append(duration)
            return toAppendTo
        }

        override fun parseObject(source: String, pos: ParsePosition): Any {
            throw UnsupportedOperationException("not implemented")
        }
    }
}
