package de.tritrack.recording.recording

import android.util.Log

import com.movisens.smartgattlib.Characteristic
import com.movisens.smartgattlib.characteristics.HeartRateMeasurement

import java.util.ArrayList
import java.util.UUID

import rx.functions.Func1

/**
 * Created by till on 05.06.17.
 */

enum class SensorFeature private constructor(val characteristic: UUID, private val mReader: Func1<ByteArray, List<Double>>) {
    UNSUPPORTED_FEATURE(UUID(0, 0), Func1<ByteArray, List<Double>> { _ ->
        throw UnsupportedOperationException()
    }),
    HEART_RATE(Characteristic.HEART_RATE_MEASUREMENT,
            Func1<ByteArray, List<Double>> { characteristicValue ->
                val hrm = HeartRateMeasurement(characteristicValue)
                val res = ArrayList<Double>()
                res.add(hrm.hr.toDouble())
                res
            }),
    CYCLING_POWER(Characteristic.CYCLING_POWER_MEASUREMENT, Func1<ByteArray, List<Double>> { characteristicValue ->
        val cpm = CyclingPowerMeasurement(characteristicValue)
        val res = ArrayList<Double>()
        res.add(cpm.power.toDouble())
        if (cpm.isCrankRevPresent)
        // TODO: need to do conversion here
        //res.add((double) cpm.getCumulativeCrankRevolutions());
            res.add(cpm.lastCrankEventTime.toDouble())
        else
            res.add(0.0)
        res
    });

    fun readData(data: ByteArray): List<Double> {
        return mReader.call(data)
    }

}
