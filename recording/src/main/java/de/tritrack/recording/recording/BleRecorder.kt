package de.tritrack.recording.recording

import android.content.Context
import android.util.Log

import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleDevice

import java.util.ArrayList

import rx.Subscription

/**
 * Created by till on 05.05.17.
 */

internal class BleRecorder(context: Context) {

    private val mBlePool: BlePool

    private var mScanSubscription: Subscription? = null
    private var mServiceSubscription: Subscription? = null
    private val mReadSubscriptions: MutableList<Subscription>

    init {
        // TODO: check this initialization
        if (mBleClient == null) {
            mBleClient = RxBleClient.create(context)
        }
        mBlePool = BlePool
        // TODO: this should probably only called once
        mBlePool.loadSavedDevices(context, mBleClient!!)
        mReadSubscriptions = ArrayList()

    }

    fun startNewDevicesScan(scanListener: BlePool.SensorDeviceScanListener) {
        // TODO: synchronize pool
        mBlePool.setUiListener(scanListener)
        startBleScanning()
    }

    fun stopNewDevicesScan() {
        stopBleScanning()
        stopServiceDiscovery()
    }

    private fun startBleScanning() {
        assert(mScanSubscription == null)
        stopServiceDiscovery()
        Log.i(TAG, "scanning for BLE devices")
        mScanSubscription = mBleClient!!.scanBleDevices().subscribe(
                { rxBleScanResult ->
                    val device = rxBleScanResult.bleDevice
                    Log.i(TAG, "found ble device, result: " + device.name)
                    if (!mBlePool.probeDevice(device))
                        startServiceDiscovery(device)
                }) { throwable -> Log.i(TAG, "Error getting BLE device: " + throwable.toString(), throwable) }
    }

    private fun startServiceDiscovery(device: RxBleDevice) {
        assert(mServiceSubscription == null)
        stopBleScanning()
        // TODO: can we not stop scanning? what if multiple devices need to be connected?
        Log.i(TAG, "checking with connection state: " + device.connectionState)
        mServiceSubscription = device.establishConnection(false)
                .flatMap { rxBleConnection -> rxBleConnection.discoverServices() }
                .subscribe({ rxBleDeviceServices ->
                    val services = rxBleDeviceServices.bluetoothGattServices
                    // TODO: check if a device for this service is already connected
                    // TODO: do not connect to the same device twice
                    val sensorFeatures = ArrayList<SensorFeature>()
                    for (service in services) {
                        val serviceUuid = service.uuid
                        val feature = SensorFeature.fromUuid(serviceUuid)
                        Log.i(TAG, "device has feature " + feature)
                        if (feature !== SensorFeature.UNSUPPORTED_FEATURE) {
                            sensorFeatures.add(feature)
                        }
                    }
                    if (!sensorFeatures.isEmpty()) {
                        mBlePool.addSensorDevice(device, sensorFeatures)
                    }
                    // resume scanning
                    startBleScanning()
                }) { throwable -> Log.i(TAG, "Error scanning BLE services: " + throwable.toString(), throwable) }
    }

    // directly adds inputs, is this ok or can the workflow be improved?
    fun startRecording(streamer: DataStreamer) {
        stopNewDevicesScan()
        // TODO: make sure during a recording we do not scan for new devices and do not allow do
        // activate or deactivate existing ones. Alternatively add functionality to handle
        // disconnects
        Log.i(TAG, "Start BLE recording")
        for (device in mBlePool.enabledDevices) {
            if (device.isConnected) {
                continue
            }
            // TODO: what happens if a connected device gets disconnected?
//            for (sensFeature in device.supportedFeatures) {
//                startDeviceRecording(device.mRxDevice, sensFeature,
//                        device.getActivityFeatures(sensFeature), streamer)
//            }
            startDeviceRecording(device, streamer)
        }
    }

    private fun startDeviceRecording(device: BlePool.SensorDevice, streamer: DataStreamer) {
//        Log.i(TAG, "adding listener for feature " + sensFeature)

        val bleConnection = device.mRxDevice.establishConnection(true)
        for (sensFeature in device.supportedFeatures) {
            val dataPublishers = device.getActivityFeatures(sensFeature)
                    .filter{ actFeature -> !streamer.hasInputSource(actFeature) }
                    .map{ actFeature ->
                        streamer.getInputProvider(actFeature, true)
                    }
            val readSubscription = bleConnection.flatMap { rxBleConnection ->
                rxBleConnection.setupNotification(sensFeature.characteristic) }
                    .flatMap { observable -> observable }
                    // TODO: check if the retry overhead can be limited
                    .retry()
                    .subscribe({ characteristicValue ->
                        val values = sensFeature.readData(characteristicValue)
                        UICommunication.runOnUiThread(Runnable {
                            assert(values.size >= dataPublishers.size)
                            for (i in dataPublishers.indices) {
                                //Log.i(TAG, "reading value for " + activityFeatures.get(i));
                                assert(dataPublishers[i] != null)

                                dataPublishers[i].onNext(values[i])
                            }
                        })
                    }) { throwable: Throwable ->
                        Log.i(TAG, "Error recording from BLE device: " + throwable.toString(), throwable)
                    }

            mReadSubscriptions.add(readSubscription)
        }



        // TODO: remove subscriptions for disconnected devices
//        device.observeConnectionStateChanges().subscribe({ state: RxBleConnection.RxBleConnectionState? ->
//            if (state == RxBleConnection.RxBleConnectionState.DISCONNECTED) {
//                // TODO: also already when disconnecting?
//                // TODO: necessary?
//                readSubscription.unsubscribe()
//            }
//        })
    }

    fun stopRecording() {
        Log.i(TAG, "Stop BLE recording")
        for (mReadSubscription in mReadSubscriptions) {
            mReadSubscription.unsubscribe()
        }
        mReadSubscriptions.clear()
    }

    private fun stopBleScanning() {
        // TODO: do the observable methods run in different threads? do we have to worry
        // about thread-safety?
        if (mScanSubscription != null) {
            mScanSubscription!!.unsubscribe()
            mScanSubscription = null
        }
    }

    private fun stopServiceDiscovery() {
        if (mServiceSubscription != null) {
            mServiceSubscription!!.unsubscribe()
            mServiceSubscription = null
        }
    }

    companion object {

        private val TAG = "de.tritrack.BleRecorder"

        //    private static final UUID UUID_HEART_RATE_MEASUREMENT = uuidFromShortCode16("2A37");
        //    private static final String BASE_BLUETOOTH_UUID_POSTFIX = "0000-1000-8000-00805F9B34FB";
        // scan for 10 seconds
        private val BT_SCAN_PERIOD_MS: Long = 10000
        private val BT_SERVICE_DISCOVERY_PERIOD_MS: Long = 5000

        //    public static UUID uuidFromShortCode16(String shortCode16) {
        //        return UUID.fromString("0000" + shortCode16 + "-" + BASE_BLUETOOTH_UUID_POSTFIX);
        //    }
        //
        //    public static UUID uuidFromShortCode32(String shortCode32) {
        //        return UUID.fromString(shortCode32 + "-" + BASE_BLUETOOTH_UUID_POSTFIX);
        //    }

        private var mBleClient: RxBleClient? = null

    }

}
