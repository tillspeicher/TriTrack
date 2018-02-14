package de.tritrack.recording.recording

import android.util.Log

import com.movisens.smartgattlib.Characteristic
import com.movisens.smartgattlib.characteristics.HeartRateMeasurement
import com.movisens.smartgattlib.characteristics.CyclingSpeedCadenceMeasurement

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
        if (cpm.isCrankRevPresent) {
            // TODO: can RMP be computed here?
            res.add(cpm.cumulativeCrankRevolutions.toDouble())
            res.add(cpm.lastCrankEventTime.toDouble())
        } else
            res.add(0.0)
            res.add(0.0)
        res
    }),
    CYCLING_SPEED_CADENCE(Characteristic.CSC_MEASUREMENT, Func1<ByteArray, List<Double>> { characteristicValue ->
        val cscm = CyclingSpeedCadenceMeasurement(characteristicValue)
        // TODO: more flexibility
        assert(cscm.isWheelRevPresent)

        val res = ArrayList<Double>()
        res.add(cscm.cumulativeWheelRevolutions.toDouble())
        res.add(cscm.lastWheelEventTime.toDouble())

        if (cscm.isCrankRevPresent) {
            res.add(cscm.cumulativeCrankRevolutions.toDouble())
            res.add(cscm.lastCrankEventTime.toDouble())
        }

        res
    });


    fun readData(data: ByteArray): List<Double> {
        return mReader.call(data)
    }

}
