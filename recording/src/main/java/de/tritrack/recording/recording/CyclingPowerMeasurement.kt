package de.tritrack.recording.recording

import android.util.Log

import com.movisens.smartgattlib.GattByteBuffer
import com.movisens.smartgattlib.GattUtils

// TODO: move into SmartGattLib
class CyclingPowerMeasurement(value: ByteArray) {

    private val pedalPowerBalancePresent: Boolean
    private val accumulatedTorquePresent: Boolean
    // TODO: copy past from CyclingSpeedCadenceMeasurement, can we add inheritance?
    val isWheelRevPresent: Boolean
    val isCrankRevPresent: Boolean

    val power: Int
    private var pedalPowerBalance: Short = 0
    private var accumulatedTorque: Int = 0
    var cumulativeWheelRevolutions: Long = 0
        private set
    //unit has resolution of 1/1024s
    var lastWheelEventTime: Int = 0
        private set
    var cumulativeCrankRevolutions: Int = 0
        private set
    //unit has resolution of 1/1024s
    var lastCrankEventTime: Int = 0
        private set


    init {
        val bb = GattByteBuffer.wrap(value)

        val flags = bb.int16!!.toInt()
        //        for (byte b : value) {
        //            Log.i("CPM", String.format("byte: %02X", b));
        //        }
        pedalPowerBalancePresent = pedalPowerBalancePresent(flags)
        accumulatedTorquePresent = accumulatedTorquePresent(flags)
        isWheelRevPresent = wheelRevPresent(flags)
        isCrankRevPresent = crankRevPresent(flags)

        power = bb.int16!!.toInt()

        if (pedalPowerBalancePresent)
            pedalPowerBalance = bb.uint8!!
        if (accumulatedTorquePresent)
            accumulatedTorque = bb.uint16!!

        if (isWheelRevPresent) {
            cumulativeWheelRevolutions = bb.uint32!!
            lastWheelEventTime = bb.uint16!!
        }

        if (isCrankRevPresent) {
            cumulativeCrankRevolutions = bb.uint16!!
            lastCrankEventTime = bb.uint16!!
        }
    }

    private fun pedalPowerBalancePresent(flags: Int): Boolean {
        return flags and GattUtils.FIRST_BITMASK != 0
    }

    private fun accumulatedTorquePresent(flags: Int): Boolean {
        return flags and GattUtils.THIRD_BITMASK != 0
    }

    private fun wheelRevPresent(flags: Int): Boolean {
        return flags and GattUtils.FIFTH_BITMASK != 0
    }

    private fun crankRevPresent(flags: Int): Boolean {
        return flags and GattUtils.SIXTH_BITMASK != 0
    }

    override fun toString(): String {
        return "CyclingPowerMeasurement{" +
                "pedalPowerBalancePresent=" + pedalPowerBalancePresent +
                ", accumulatedTorquePresent=" + accumulatedTorquePresent +
                ", wheelRevPresent=" + isWheelRevPresent +
                ", crankRevPresent=" + isCrankRevPresent +
                ", instantaneousPower=" + power +
                ", predalPowerBalance=" + pedalPowerBalance +
                ", accumulatedTorque=" + accumulatedTorque +
                ", cumulativeWheelRevolutions=" + cumulativeWheelRevolutions +
                ", lastWheelEventTime=" + lastWheelEventTime +
                ", cumulativeCrankRevolutions=" + cumulativeCrankRevolutions +
                ", lastCrankEventTime=" + lastCrankEventTime +
                '}'
    }

    companion object {

        val MAX_CUMULATIVE_CRANK_REVS = 65535
        val MAX_CUMULATIVE_WHEEL_REVS = 4294967295L
    }
}
