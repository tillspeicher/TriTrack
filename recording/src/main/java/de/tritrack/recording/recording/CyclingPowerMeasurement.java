package de.tritrack.recording.recording;

import android.util.Log;

import com.movisens.smartgattlib.GattByteBuffer;
import com.movisens.smartgattlib.GattUtils;

// TODO: move into SmartGattLib
public class CyclingPowerMeasurement {

    public static final int MAX_CUMULATIVE_CRANK_REVS = 65535;
    public static final long MAX_CUMULATIVE_WHEEL_REVS = 4294967295L;

    private boolean pedalPowerBalancePresent;
    private boolean accumulatedTorquePresent;
    private boolean wheelRevPresent;
    private boolean crankRevPresent;

    private int instantaneousPower;
    private short pedalPowerBalance;
    private int accumulatedTorque;
    private long cumulativeWheelRevolutions;
    private int lastWheelEventTime;
    private int cumulativeCrankRevolutions;
    private int lastCrankEventTime;


    public CyclingPowerMeasurement(byte[] value) {
        GattByteBuffer bb = GattByteBuffer.wrap(value);

        int flags = bb.getInt16();
//        for (byte b : value) {
//            Log.i("CPM", String.format("byte: %02X", b));
//        }
        pedalPowerBalancePresent = pedalPowerBalancePresent(flags);
        accumulatedTorquePresent = accumulatedTorquePresent(flags);
        wheelRevPresent = wheelRevPresent(flags);
        crankRevPresent = crankRevPresent(flags);

        instantaneousPower = bb.getInt16();

        if (pedalPowerBalancePresent)
            pedalPowerBalance = bb.getUint8();
        if (accumulatedTorquePresent)
            accumulatedTorque = bb.getUint16();

        if (wheelRevPresent) {
            cumulativeWheelRevolutions = bb.getUint32();
            lastWheelEventTime = bb.getUint16();
        }

        if (crankRevPresent) {
            cumulativeCrankRevolutions = bb.getUint16();
            lastCrankEventTime = bb.getUint16();
        }
    }

    public int getPower() {
        return instantaneousPower;
    }

    // TODO: copy past from CyclingSpeedCadenceMeasurement, can we add inheritance?
    public boolean isWheelRevPresent() {
        return wheelRevPresent;
    }

    public boolean isCrankRevPresent() {
        return crankRevPresent;
    }

    public long getCumulativeWheelRevolutions() {
        return cumulativeWheelRevolutions;
    }

    //unit has resolution of 1/1024s
    public int getLastWheelEventTime() {
        return lastWheelEventTime;
    }

    public int getCumulativeCrankRevolutions() {
        return cumulativeCrankRevolutions;
    }

    //unit has resolution of 1/1024s
    public int getLastCrankEventTime() {
        return lastCrankEventTime;
    }

    private boolean pedalPowerBalancePresent(int flags) {
        return (flags & GattUtils.FIRST_BITMASK) != 0;
    }

    private boolean accumulatedTorquePresent(int flags) {
        return (flags & GattUtils.THIRD_BITMASK) != 0;
    }

    private boolean wheelRevPresent(int flags) {
        return (flags & GattUtils.FIFTH_BITMASK) != 0;
    }

    private boolean crankRevPresent(int flags) {
        return (flags & GattUtils.SIXTH_BITMASK) != 0;
    }

    @Override
    public String toString() {
        return "CyclingPowerMeasurement{" +
                "pedalPowerBalancePresent=" + pedalPowerBalancePresent +
                ", accumulatedTorquePresent=" + accumulatedTorquePresent +
                ", wheelRevPresent=" + wheelRevPresent +
                ", crankRevPresent=" + crankRevPresent +
                ", instantaneousPower=" + instantaneousPower +
                ", predalPowerBalance=" + pedalPowerBalance +
                ", accumulatedTorque=" + accumulatedTorque +
                ", cumulativeWheelRevolutions=" + cumulativeWheelRevolutions +
                ", lastWheelEventTime=" + lastWheelEventTime +
                ", cumulativeCrankRevolutions=" + cumulativeCrankRevolutions +
                ", lastCrankEventTime=" + lastCrankEventTime +
                '}';
    }
}
