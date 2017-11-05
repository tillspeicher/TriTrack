package de.tritrack.recording.recording

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.RxBleDevice
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * Created by till on 03.06.17.
 */

object BlePool {

    val TAG = "BlePool"

    // TODO: currently this should only be accessed through the UI Thread but there might
    // be a need at some point to synchronize access to this guy
    class SensorDevice(var mRxDevice: RxBleDevice,
                       private val mSensorFeatures: Map<SensorFeature, List<ActivityFeature>>,
                       enabled: Boolean) {

        var isEnabled = enabled
            set(value) {
                field = value
                updateUi()

                if (value)
                    mEnabledMacAddresses.add(mRxDevice.macAddress)
                else
                    mEnabledMacAddresses.remove(mRxDevice.macAddress)
                val editor = mDeviceConfigStore?.edit()
                editor?.putStringSet(KEY_MAC_ADDRESSES, mEnabledMacAddresses)
                editor?.apply()
            }

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

        val isConnected: Boolean
            // TODO: what about connecting and disconnecting?
            get() = mRxDevice.connectionState != RxBleConnection.RxBleConnectionState.DISCONNECTED
    }

    private var DEV_PREFS_NAME = "SavedDevices"
    private var KEY_MAC_ADDRESSES = "MacAddresses"

    // synchronize access
    private val mDiscoveredDevices: MutableMap<String, SensorDevice>
    private var mUiListener: SensorDeviceScanListener? = null

    private var mDeviceConfigStore: SharedPreferences? = null
    private val mDisconnectedSavedDevices: MutableMap<String, RxBleDevice>
    private val mEnabledMacAddresses: MutableSet<String>

    init {
        mDiscoveredDevices = HashMap()
        mDisconnectedSavedDevices = HashMap()
        mEnabledMacAddresses = HashSet()
    }

    fun loadSavedDevices(context: Context, bleClient: RxBleClient) {
        if (mDeviceConfigStore != null)
            throw IllegalArgumentException("config store was already set")
        val configStore = context.getSharedPreferences(DEV_PREFS_NAME, 0)
        mDeviceConfigStore = configStore
        mEnabledMacAddresses.addAll(configStore.getStringSet(KEY_MAC_ADDRESSES, HashSet()))
        mEnabledMacAddresses.forEach ({ macAddr -> mDisconnectedSavedDevices.put(macAddr, bleClient.getBleDevice(macAddr)) })
        mEnabledMacAddresses.forEach({
            macAddr -> Log.i(TAG, "Loaded stored BLE device " + mDisconnectedSavedDevices[macAddr])
        })
    }

    fun setUiListener(uiListener: SensorDeviceScanListener) {
        mUiListener = uiListener
        updateUi()
    }

    fun clearUiListener() {
        mUiListener = null
    }

    @Synchronized
    fun probeDevice(rxDevice: RxBleDevice): Boolean {
        // TODO: store connected Mac addresses and autoconnect
        return mDiscoveredDevices.containsKey(rxDevice.macAddress)
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

        val disconnectedSavedDevice = mDisconnectedSavedDevices.remove(device.macAddress)
        val enabled = disconnectedSavedDevice != null
        val bleDevice = SensorDevice(device, sensorFeatures, enabled)
        mDiscoveredDevices.put(device.macAddress, bleDevice)
        updateUi()
        // TODO: when to remove devices? if the get selected for connection, go out of range, etc.
    }

    val disconnectedSavedDevices: Collection<RxBleDevice>
        get() = mDisconnectedSavedDevices.values

    val enabledDevices: List<SensorDevice>
        @Synchronized get() {
            return ArrayList<SensorDevice>(mDiscoveredDevices.values.filter{ dev -> dev.isEnabled })
        }

    fun updateUi() {
        UICommunication.runOnUiThread(Runnable { mUiListener!!.resultAvailable(ArrayList(mDiscoveredDevices.values)) })
    }

    interface SensorDeviceScanListener {
        fun resultAvailable(devices: List<BlePool.SensorDevice>)
    }

}
