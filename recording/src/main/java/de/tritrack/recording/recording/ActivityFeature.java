package de.tritrack.recording.recording;

import android.support.annotation.NonNull;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.datatype.Duration;

/**
 * Created by till on 22.12.16.
 */

public enum ActivityFeature {
    TIME_S ("Duration", "", new TimeFormatter(true)),
    LATITUDE ("Lat", "deg", new DecimalFormat("#.#######")),
    LONGITUDE ("Lon", "deg", new DecimalFormat("#.#######")),
    ALTITUDE ("Altitude", "m", new DecimalFormat("#")),
    DISTANCE_INCREMENT_M ("Distance increment", "m", new DecimalFormat("#")),
    DISTANCE_M ("Distance", "m", new DecimalFormat("#")),
    DISTANCE_KM ("Distance", "km", new DecimalFormat("#.##")),
    DISTANCE_KM_REV ("Distance (REV)", "km", new DecimalFormat("#.##")),
    ELEVATION_GAIN ("Elevation gain", "m", new DecimalFormat("#")),
    SPEED_MS ("Speed", "m/s", new DecimalFormat("#.#")),
    SPEED_KMH ("Speed", "km/h", new DecimalFormat("#.#")),
    SPEED_KMH_REV ("Speed", "km/h", new DecimalFormat("#.#")),
    AVG_SPEED_KMH ("Avg Speed", "km/h", new DecimalFormat("#.#")),
    MAX_SPEED_KMH ("Max Speed", "km/h", new DecimalFormat("#.#")),
    PACE ("Pace", "min/km", new TimeFormatter(false)),
    AVG_PACE ("Avg Pace", "min/km", new TimeFormatter(false)),
    HEART_RATE ("Heart Rate", "bpm", new DecimalFormat("#")),
    AVG_HEART_RATE ("Avg Heart Rate", "bpm", new DecimalFormat("#")),
    MAX_HEART_RATE ("Max Heart Rate", "bpm", new DecimalFormat("#")),
    POWER_LEFT ("Power left", "W", new DecimalFormat("#.#")),
    POWER_RIGHT ("Power right", "W", new DecimalFormat("#.#")),
    POWER_COMBINED ("Power combined", "W", new DecimalFormat("#.#")),
    AVG_POWER_COMBINED ("Avg Power combined", "W", new DecimalFormat("#")),
    MAX_POWER_COMBINED ("Max Power combined", "W", new DecimalFormat("#")),
    CUMULATIVE_WHEEL_REVOLUTIONS ("Cumulative Wheel Revolutions", "#", new DecimalFormat("#")),
    LAST_WHEEL_EVENT ("Last Wheel Event", "1/1024s", new DecimalFormat("#")),
    CUMULATIVE_CRANK_REVOLUTIONS ("Cumulative Crank Revolutions", "#", new DecimalFormat("#")),
    LAST_CRANK_EVENT ("Last Crank Event", "1/1024s", new DecimalFormat("#")),
    CADENCE ("Cadence", "rpm", new DecimalFormat("#")),
    AVG_CADENCE ("Avg Cadence", "rpm", new DecimalFormat("#")),
    AVG_NORM_POWER("Avg Power (norm)", "W", new DecimalFormat("#")),
    AVG_NORM_CADENCE("Avg Cadence (norm)", "rpm", new DecimalFormat("#")),
    SMOOTH_POWER("Power (3s)", "W", new DecimalFormat("#.#"));

    private final String mDescription;
    private final String mUnit;
    private final Format mFormat;

    ActivityFeature(String description, String unit, Format format) {
        mDescription = description;
        mUnit = unit;
        mFormat = format;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getUnit() {
        return mUnit;
    }

    public String format(Double val) {
        return mFormat.format(val);
    }

    // TODO: this is a workaround
    private static class TimeFormatter extends Format {

        private boolean mIncludeHours;

        public TimeFormatter(boolean includeHours) {
            mIncludeHours = includeHours;
        }

        @Override
        public StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
            // it actually is a double
            double secD = (Double) number;
            int s = (int) secD;
            String duration = String.format("%02d:%02d", (s % 3600) / 60, (s % 60));
            if (mIncludeHours)
                duration = String.format("%d:", s / 3600) + duration;
            toAppendTo.append(duration);
            return toAppendTo;
        }

        @Override
        public Object parseObject(String source, @NonNull ParsePosition pos) {
            throw new UnsupportedOperationException("not implemented");
        }
    }
}
