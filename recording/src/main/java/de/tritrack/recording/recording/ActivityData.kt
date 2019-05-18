package de.tritrack.recording.recording

import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition

class ActivityData(val feature: ActFeature, val op: OpType) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActivityData

        return feature == other.feature && op == other.op
    }

    override fun hashCode(): Int {
        var result = feature.hashCode()
        result = 31 * result + op.hashCode()
        return result
    }

    override fun toString(): String {
        return "($feature, $op)"
    }
}

/**
 * Created by till on 22.12.16.
 */

enum class ActFeature(val description: String, val unit: String, private val mFormat: Format) {
    TIMESTAMP("Timestamp", "", TimeFormatter(true)),
    DURATION_S("Duration", "", TimeFormatter(true)),
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
    PACE("Pace", "min/km", TimeFormatter(false)),
    HEART_RATE("Heart Rate", "bpm", DecimalFormat("#")),
    POWER_LEFT("Power left", "W", DecimalFormat("#")),
    POWER_RIGHT("Power right", "W", DecimalFormat("#")),
    POWER_COMBINED("Power", "W", DecimalFormat("#")),
    CUMULATIVE_WHEEL_REVOLUTIONS("Cumulative Wheel Revolutions", "#", DecimalFormat("#")),
    LAST_WHEEL_EVENT("Last Wheel Event", "1/1024s", DecimalFormat("#")),
    CUMULATIVE_CRANK_REVOLUTIONS("Cumulative Crank Revolutions", "#", DecimalFormat("#")),
    LAST_CRANK_EVENT("Last Crank Event", "1/1024s", DecimalFormat("#")),
    CADENCE("Cadence", "rpm", DecimalFormat("#")),
    SMOOTH_POWER("Power (3s)", "W", DecimalFormat("#.#"));

    fun format(`val`: Double?): String {
        return if (`val` == null)
            "-"
        else
            mFormat.format(`val`)
    }

    // TODO: this is a workaround
    private class TimeFormatter(private val includeHours: Boolean, private val isTimestamp: Boolean = false) : Format() {

        override fun format(number: Any, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
            // it actually is a double
            val secD = number as Double
            val s = secD.toInt()
            var duration = String.format("%02d:%02d", s % 3600 / 60, s % 60)
            if (includeHours)
                duration = String.format("%d:", s / 3600) + duration
            toAppendTo.append(duration)
            return toAppendTo
        }

        override fun parseObject(source: String, pos: ParsePosition): Any {
            throw UnsupportedOperationException("not implemented")
        }
    }
}

enum class OpType(val prefix: String) {
    ID (""),
    AVG ("Avg "),
    MAX ("Max "),
    NORM_AVG ("Avg norm. "),
    OFFSET ("") // used to make values like time or distance start at 0 when they are subscribed later
}