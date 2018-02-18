package de.tritrack.recording.recording

import android.util.Log

import com.movisens.smartgattlib.Characteristic
import com.movisens.smartgattlib.Service
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

    companion object {

        fun fromUuid(serviceUuid: UUID): SensorFeature {
            return if (serviceUuid == Service.HEART_RATE)
                SensorFeature.HEART_RATE
            else if (serviceUuid == Service.CYCLING_POWER)
                SensorFeature.CYCLING_POWER
            else if (serviceUuid == Service.CYCLING_SPEED_AND_CADENCE)
                SensorFeature.CYCLING_SPEED_CADENCE
            else
                SensorFeature.UNSUPPORTED_FEATURE
        }

        fun getActivityFeatures(sensFeature: SensorFeature, devName: String): List<ActivityFeature> {
            // TODO: don't use device name, use the service feature
            val activityFeatures = ArrayList<ActivityFeature>()
            when (sensFeature) {
                SensorFeature.HEART_RATE -> {
                    activityFeatures.add(ActivityFeature.HEART_RATE)
                }
                SensorFeature.CYCLING_POWER -> {
                    // TODO: find a better way than this
                    if (devName.endsWith("L"))
                        activityFeatures.add(ActivityFeature.POWER_LEFT)
                    else if (devName.endsWith("R"))
                        activityFeatures.add(ActivityFeature.POWER_RIGHT)
                    else
                        activityFeatures.add(ActivityFeature.POWER_COMBINED)
                    // TODO: check whether the sensor supports this
                    activityFeatures.add(ActivityFeature.CUMULATIVE_CRANK_REVOLUTIONS)
                    activityFeatures.add(ActivityFeature.LAST_CRANK_EVENT)
                }
                SensorFeature.CYCLING_SPEED_CADENCE -> {
                    activityFeatures.add(ActivityFeature.CUMULATIVE_WHEEL_REVOLUTIONS)
                    activityFeatures.add(ActivityFeature.LAST_WHEEL_EVENT)

                    // TODO: not always
                    activityFeatures.add(ActivityFeature.CUMULATIVE_CRANK_REVOLUTIONS)
                    activityFeatures.add(ActivityFeature.LAST_CRANK_EVENT)
                }
            }
            return activityFeatures
        }
    }

}
