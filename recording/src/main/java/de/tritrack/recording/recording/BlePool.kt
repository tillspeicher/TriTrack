package de.tritrack.recording.recording

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.RxBleDevice
import rx.Subscription
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * Created by till on 03.06.17.
 */

object BlePool {

    private const val TAG = "BlePool"

    // TODO: currently this should only be accessed through the UI Thread but there might
    // be a need at some point to synchronize access to this guy
    class SensorDevice internal constructor(rxDevice: RxBleDevice,
                                            name: String,
                                            sensorFeatures: List<SensorFeature>,
                                            enabled: Boolean) {

        // TODO: check property handling, can this be simplified?
        val mRxDevice = rxDevice
            @Synchronized get

        // TODO: the null thing is ugly the whole process should be refactored to scan the BLE device for what
        // features it supports
        private val mSensorFeatures = getActivityFeatureMap(sensorFeatures, name)

        var isEnabled = enabled
            @Synchronized get
            @Synchronized
            set(value) {
                field = value
                updateUi()
                saveEnabledState(mRxDevice.macAddress, value, name, mSensorFeatures.keys)
                // TODO: should disabled, disconnected devices be completely removed?
            }

        val name = name

        val supportedFeatures: Set<SensorFeature>
            @Synchronized
            get() = mSensorFeatures.keys

        @Synchronized
        fun getActivityFeatures(sensorFeature: SensorFeature): List<ActFeature> {
            val actFeatures = mSensorFeatures[sensorFeature]
            return actFeatures ?: Collections.emptyList()
        }

        val isConnected: Boolean
            // TODO: monitor connection changes in UI
            @Synchronized
            get() {
//                Log.i(TAG, "Dev %s connection state: %s".format(name, mRxDevice.connectionState))
                return mRxDevice.connectionState != RxBleConnection.RxBleConnectionState.DISCONNECTED
            }

    }

    private const val KEY_DEV_PREFS = "ble_sensor_configuration"
    private const val KEY_MAC_ADDRESSES = "mac_addresses"
    private const val KEY_SENSOR_FEATURES_POSTFIX = "_sensor_features"
    private const val KEY_DEV_NAME_POSTFIX = "_dev_name"


    // synchronize access
    private val mDevices: MutableMap<String, SensorDevice>
    private var mUiListener: SensorDeviceScanListener? = null
    private val mConnectionStateSubscriptions = ArrayList<Subscription>()

    private var mDeviceConfigStore: SharedPreferences? = null
    private val mEnabledMacAddresses: MutableSet<String>

    init {
        mDevices = HashMap()
        mEnabledMacAddresses = HashSet()
    }

    fun loadSavedDevices(context: Context, bleClient: RxBleClient) {
        if (mDeviceConfigStore != null)
            throw IllegalArgumentException("config store was already set")
        // TODO
        mDeviceConfigStore = context.getSharedPreferences(KEY_DEV_PREFS, Context.MODE_PRIVATE)

        mEnabledMacAddresses.addAll(mDeviceConfigStore!!.getStringSet(KEY_MAC_ADDRESSES, HashSet()))
        mEnabledMacAddresses.forEach({ macAddr ->
            val devName = mDeviceConfigStore!!.getString(macAddr + KEY_DEV_NAME_POSTFIX, "<missing>")
            val sensorFeatures = mDeviceConfigStore!!
                    .getStringSet(macAddr + KEY_SENSOR_FEATURES_POSTFIX, HashSet())
                    .map { featureName -> SensorFeature.valueOf(featureName) }
            val sensorDevice = SensorDevice(bleClient.getBleDevice(macAddr), devName,
                    sensorFeatures, true)
            mDevices[macAddr] = sensorDevice

            Log.i(TAG, "Loaded stored BLE device " + sensorDevice)
        })
    }

    fun setUiListener(uiListener: SensorDeviceScanListener) {
        assert(mUiListener == null)
        assert(mConnectionStateSubscriptions.isEmpty())

        mUiListener = uiListener
        updateUi()

        // TODO: reconnect if device connections get interruped
        // TODO: enable showing the device connection state while scanning
//        mConnectionStateSubscriptions.addAll(mDevices.values.map{ dev ->
//            dev.mRxDevice.observeConnectionStateChanges().subscribe({connectionState ->
//                // TODO: find better way than updating the complete list
//                uiListener.resultAvailable(ArrayList(mDevices.values))
//            })
//        })
    }

    fun clearUiListener() {
        mUiListener = null
//        mConnectionStateSubscriptions.forEach({ subscription -> subscription.unsubscribe() })
//        mConnectionStateSubscriptions.clear()
    }

    @Synchronized
    fun probeDevice(rxDevice: RxBleDevice): Boolean {
        return mDevices.containsKey(rxDevice.macAddress)
    }

    /**
     * Adds `device` to the list of discovered devices if it is not already part of the list
     * and notifies the UI of the change
     * @param device
     */
    @Synchronized
    fun addSensorDevice(device: RxBleDevice,
                     sensorFeatures: List<SensorFeature>) {
        assert(!mDevices.containsKey(device.macAddress))

        val bleDevice = SensorDevice(device, device.name, sensorFeatures, false)
        mDevices[device.macAddress] = bleDevice
        updateUi()
        // TODO: when to remove devices? if the get selected for connection, go out of range, etc.
    }

    val enabledDevices: List<SensorDevice>
        @Synchronized get() {
            return ArrayList<SensorDevice>(mDevices.values.filter{ dev -> dev.isEnabled })
        }

    interface SensorDeviceScanListener {
        // TODO: try to not update the entire list, just the devices that changed
        fun resultAvailable(devices: List<BlePool.SensorDevice>)
    }


    private fun updateUi() {
        UICommunication.runOnUiThread(Runnable { mUiListener!!.resultAvailable(ArrayList(mDevices.values)) })
    }

    @Synchronized
    private fun saveEnabledState(macAddress: String, isEnabled: Boolean, devName: String,
                                 sensorFeatures: Set<SensorFeature>) {
        if (isEnabled)
            assert(mEnabledMacAddresses.add(macAddress))
        else
            assert(mEnabledMacAddresses.remove(macAddress))

        with (mDeviceConfigStore!!.edit()) {
            putStringSet(KEY_MAC_ADDRESSES, mEnabledMacAddresses)

            val sensorFeatureKey = macAddress + KEY_SENSOR_FEATURES_POSTFIX
            val devNameKey = macAddress + KEY_DEV_NAME_POSTFIX
            if (isEnabled) {
                putStringSet(sensorFeatureKey,
                        HashSet(sensorFeatures.map { sf -> sf.name }))
                putString(devNameKey, devName)
            }else {
                remove(sensorFeatureKey)
                remove(devNameKey)
            }

            commit()
        }
    }

    private fun getActivityFeatureMap(sensFeatures: List<SensorFeature>, devName: String):
            Map<SensorFeature, List<ActFeature>> {
        val activityFeatures = HashMap<SensorFeature, List<ActFeature>>()
        sensFeatures.forEach({ sf -> activityFeatures.put(sf,
                SensorFeature.getActivityFeatures(sf, devName)) })
        return activityFeatures
    }

}
