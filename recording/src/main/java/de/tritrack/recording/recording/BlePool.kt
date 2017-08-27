package de.tritrack.recording.recording

import com.polidea.rxandroidble.RxBleDevice
import java.util.*

/**
 * Created by till on 03.06.17.
 */

object BlePool {

    // TODO: currently this should only be accessed through the UI Thread but there might
    // be a need at some point to synchronize access to this guy
    class SensorDevice(var mRxDevice: RxBleDevice, private val mSensorFeatures: Map<SensorFeature, List<ActivityFeature>>) {
        private val mLastSeen: Long = 0
        var isConnected = false
            private set

        val name: String
            get() = mRxDevice.name

        val supportedFeatures: Set<SensorFeature>
            get() = mSensorFeatures.keys

        fun getActivityFeatures(sensorFeature: SensorFeature): List<ActivityFeature> {
            val actFeatures = mSensorFeatures[sensorFeature]
            if (actFeatures == null)
                return Collections.emptyList()
            return actFeatures
        }

        fun connect() {
            isConnected = true
            // TODO: somehow block the device for other people to connect
        }
    }

    // synchronize access
    private val mDiscoveredDevices: MutableMap<String, SensorDevice>
    private var mUiListener: UICommunication.BleScanListener? = null
    // TODO: periodically remove devices that weren't seen for a while

    init {
        mDiscoveredDevices = HashMap()
    }

    fun setUiListener(uiListener: UICommunication.BleScanListener) {
        mUiListener = uiListener
    }

    fun clearUiListener() {
        mUiListener = null
    }

    @Synchronized
    fun probeDevice(rxDevice: RxBleDevice): Boolean {
        // TODO: store connected Mac addresses and autoconnect
        return if (!mDiscoveredDevices.containsKey(rxDevice.macAddress)) false else true
        // TODO: update device timestamps
    }

    /**
     * Adds `device` to the list of discovered devices if it is not already part of the list
     * and notifies the UI of the change
     * @param device
     */
    @Synchronized
    fun addSensorDevice(device: RxBleDevice,
                     sensorFeatures: Map<SensorFeature, List<ActivityFeature>>) {
        assert(!mDiscoveredDevices.containsKey(device.macAddress))

        val bleDevice = SensorDevice(device, sensorFeatures)
        mDiscoveredDevices.put(device.macAddress, bleDevice)
        UICommunication.runOnUiThread(Runnable { mUiListener!!.resultAvailable(ArrayList(mDiscoveredDevices.values)) })
        // TODO: when to remove devices? if the get selected for connection, go out of range, etc.
    }

    val connectedDevices: List<SensorDevice>
        @Synchronized get() {
            val connectedDevices = ArrayList<SensorDevice>()
            for (dev in mDiscoveredDevices.values) {
                if (dev.isConnected)
                    connectedDevices.add(dev)
            }
            return connectedDevices
        }

}
