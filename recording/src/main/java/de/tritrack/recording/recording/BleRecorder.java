package de.tritrack.recording.recording;

import android.app.Activity;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import com.movisens.smartgattlib.Characteristic;
import com.movisens.smartgattlib.Service;
import com.movisens.smartgattlib.characteristics.HeartRateMeasurement;
import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.RxBleScanResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

/**
 * Created by till on 05.05.17.
 */

class BleRecorder {

    private static final String TAG = "de.tritrack.BleRecorder";

//    private static final UUID UUID_HEART_RATE_MEASUREMENT = uuidFromShortCode16("2A37");
//    private static final String BASE_BLUETOOTH_UUID_POSTFIX = "0000-1000-8000-00805F9B34FB";
    // scan for 10 seconds
    private static final long BT_SCAN_PERIOD_MS = 10000;
    private static final long BT_SERVICE_DISCOVERY_PERIOD_MS = 5000;

//    public static UUID uuidFromShortCode16(String shortCode16) {
//        return UUID.fromString("0000" + shortCode16 + "-" + BASE_BLUETOOTH_UUID_POSTFIX);
//    }
//
//    public static UUID uuidFromShortCode32(String shortCode32) {
//        return UUID.fromString(shortCode32 + "-" + BASE_BLUETOOTH_UUID_POSTFIX);
//    }

    private static RxBleClient mBleClient = null;
    private BlePool mBlePool;

    private Subscription mScanSubscription;
    private Subscription mServiceSubscription;
    private List<Subscription> mReadSubscriptions;

    public BleRecorder(Context context) {
        if (mBleClient == null) {
            mBleClient = RxBleClient.create(context);
        }
        mBlePool = new BlePool();
        mReadSubscriptions = new ArrayList<>();
    }

    public void startDeviceScan(UICommunication.BleScanListener scanListener) {
        mBlePool.setUiListener(scanListener);
        startBleScanning();
    }

    public void stopDeviceScan() {
        stopBleScanning();
        stopServiceDiscovery();
        mBlePool.clearUiListener();
    }

    private void startBleScanning() {
        assert mScanSubscription == null;
        stopServiceDiscovery();
        Log.i(TAG, "scanning for BLE devices");
        mScanSubscription = mBleClient.scanBleDevices().subscribe(
                new Action1<RxBleScanResult>() {
                    @Override
                    public void call(RxBleScanResult rxBleScanResult) {
                        RxBleDevice device = rxBleScanResult.getBleDevice();
                        Log.i(TAG, "found ble device, result: " + device.getName());
                        if (!mBlePool.probeDevice(device))
                            startServiceDiscovery(device);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "Error getting BLE device: " + throwable.toString(), throwable);
                    }
                });
    }

    private void startServiceDiscovery(final RxBleDevice device) {
        assert mServiceSubscription == null;
        stopBleScanning();
        // TODO: can we not stop scanning? what if multiple devices need to be connected?
        Log.i(TAG, "checking with connection state: " + device.getConnectionState());
        mServiceSubscription = device.establishConnection(false)
                .flatMap(new Func1<RxBleConnection, Observable<RxBleDeviceServices>>() {
                    @Override
                    public Observable<RxBleDeviceServices> call(RxBleConnection rxBleConnection) {
                        return rxBleConnection.discoverServices();
                    }
                })
                .subscribe(new Action1<RxBleDeviceServices>() {
                    @Override
                    public void call(RxBleDeviceServices rxBleDeviceServices) {
                        List<BluetoothGattService> services = rxBleDeviceServices.getBluetoothGattServices();
                        // TODO: check if a device for this service is already connected
                        // TODO: do not connect to the same device twice
                        Map<SensorFeature, List<ActivityFeature>> sensorFeatures = new HashMap<>();
                        for (BluetoothGattService service : services) {
                            UUID serviceUuid = service.getUuid();
                            SensorFeature feature = convertService(serviceUuid);
                            Log.i(TAG, "device has feature " + feature);
                            if (feature != SensorFeature.UNSUPPORTED_FEATURE) {
                                List<ActivityFeature> activityFeatures = getActivityFeatures(feature,
                                        device.getName());
                                sensorFeatures.put(feature, activityFeatures);
                            }
                        }
                        if (!sensorFeatures.isEmpty()) {
                            mBlePool.addSensorDevice(device, sensorFeatures);
                        }
                        // resume scanning
                        startBleScanning();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "Error scanning BLE services: " + throwable.toString(), throwable);
                    }
                });
    }

    private static SensorFeature convertService(UUID serviceUuid) {
        if (Service.HEART_RATE.equals(serviceUuid))
            return SensorFeature.HEART_RATE;
        else if (Service.CYCLING_POWER.equals(serviceUuid))
            return SensorFeature.CYCLING_POWER;
        else
            return SensorFeature.UNSUPPORTED_FEATURE;
    }

    private static List<ActivityFeature> getActivityFeatures(SensorFeature sensFeature, String devName) {
        // TODO: don't use device name, use the service feature
        List<ActivityFeature> activityFeatures = new ArrayList<>();
        switch(sensFeature) {
            case HEART_RATE:
                activityFeatures.add(ActivityFeature.HEART_RATE);
                return activityFeatures;
            case CYCLING_POWER:
                // TODO: find a better way than this
                if (devName.endsWith("L"))
                    activityFeatures.add(ActivityFeature.POWER_LEFT);
                else if (devName.endsWith("R"))
                    activityFeatures.add(ActivityFeature.POWER_RIGHT);
                else
                    activityFeatures.add(ActivityFeature.POWER_COMBINED);
                // TODO: check whether the sensor supports this
                activityFeatures.add(ActivityFeature.LAST_CRANK_EVENT);
                return activityFeatures;
            default:
                return null;
        }
    }

    // directly adds inputs, is this ok or can the workflow be improved?
    public void startRecording(DataStreamer streamer) {
        stopDeviceScan();
        Log.i(TAG, "Start BLE recording");
        for (BlePool.SensorDevice device : mBlePool.getConnectedDevices()) {
            // TODO: what happens if a connected device gets disconnected?
            for (SensorFeature sensFeature : device.getSupportedFeatures()) {
                startDeviceRecording(device.getMRxDevice(), sensFeature,
                        device.getActivityFeatures(sensFeature), streamer);
            }
        }
    }

    private void startDeviceRecording(RxBleDevice device, final SensorFeature sensFeature,
                                      final List<ActivityFeature> activityFeatures, DataStreamer streamer) {
        Log.i(TAG, "adding listener for feature " + sensFeature);
        final List<PublishSubject<Double>> dataPublishers = new ArrayList<>();
        for (ActivityFeature actFeature : activityFeatures) {
            if (!streamer.hasInputSource(actFeature))
                // to avoid adding something like cadence twice
                dataPublishers.add(streamer.addInput(actFeature, true));
        }

        Subscription readSubscription = device.establishConnection(false)
                .flatMap(new Func1<RxBleConnection, Observable<Observable<byte[]>>>() {
                    @Override
                    public Observable<Observable<byte[]>> call(RxBleConnection rxBleConnection) {
                        return rxBleConnection.setupNotification(sensFeature.getCharacteristic());
                    }
                })
                .flatMap(new Func1<Observable<byte[]>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(Observable<byte[]> observable) {
                        return observable;
                    }
                })
                .subscribe(new Action1<byte[]>() {
                    @Override
                    public void call(byte[] characteristicValue) {
                        final List<Double> values = sensFeature.readData(characteristicValue);
                        UICommunication.INSTANCE.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                assert values.size() >= dataPublishers.size();
                                for (int i = 0; i < dataPublishers.size(); i++) {
                                    //Log.i(TAG, "reading value for " + activityFeatures.get(i));
                                    assert dataPublishers.get(i) != null;

                                    dataPublishers.get(i).onNext(values.get(i));
                                }
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "Error recording from BLE device: " + throwable.toString(), throwable);
                    }
                });
        mReadSubscriptions.add(readSubscription);
    }

    public void stopRecording() {
        Log.i(TAG, "Stop BLE recording");
        for (Subscription mReadSubscription : mReadSubscriptions) {
            mReadSubscription.unsubscribe();
        }
        mReadSubscriptions.clear();
    }

    private void stopBleScanning() {
        // TODO: do the observable methods run in different threads? do we have to worry
        // about thread-safety?
        if (mScanSubscription != null) {
            mScanSubscription.unsubscribe();
            mScanSubscription = null;
        }
    }

    private void stopServiceDiscovery() {
        if (mServiceSubscription != null) {
            mServiceSubscription.unsubscribe();
            mServiceSubscription = null;
        }
    }

    public interface BleDataListener {
        public void hrUpdate(int hr);
    }

}
