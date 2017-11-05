package de.tritrack.recording.recording;

import android.content.Context;
import android.location.Location;
import android.os.Handler;

import java.util.List;
import java.util.Map;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesWithFallbackProvider;

import rx.subjects.PublishSubject;

/**
 * Created by till on 22.12.16.
 */

public class Recorder {

    private static final String TAG = "de.tritrack.Recorder";
    private static final float ACCURACY_THRES = 50.f;
    private static final float AUTO_PAUSE_SPEED_THRES = 1.5f;
    private static final float AUTO_PAUSE_TIME_THRES = 10.f;

    private boolean mIsStarted = false;
    private boolean mIsResumed = false;
    private double mLastSpeedGreaterPoint = 0.f;

    private SmartLocation.LocationControl mLocation;
    private BleRecorder mBleRecorder;
    private DataStreamer mDataStreamer;
    private StorageManager mStorageManager;
    private Handler mHandler;

    private static Recorder instance = null;

    public static Recorder getInstance(Context context) {
        if (instance == null)
            instance = new Recorder(context);
        return instance;
    }

    private Recorder(Context context) {
        mLocation = SmartLocation.with(context).location(new LocationGooglePlayServicesWithFallbackProvider(context));
        mLocation.config(LocationParams.NAVIGATION);
        mBleRecorder = new BleRecorder(context);
        mDataStreamer = new DataStreamer();
        mHandler = new Handler();
//        startPeriodicScanning();
    }

    public void startBleScan(BlePool.SensorDeviceScanListener scanListener) {
        mBleRecorder.startDeviceScan(scanListener);
    }

    public void stopBleScan() {
        mBleRecorder.stopDeviceScan();
    }

    public void setDataListeners(Map<ActivityFeature, UICommunication.UIDataListener> listeners) {
        mDataStreamer.setDataListeners(listeners);
    }

    private void startPeriodicScanning() {
        // TODO: do we need to stop this at some point?
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBleRecorder.periodicSensorCheck(mDataStreamer);
                mHandler.postDelayed(this, 2000);
            }
        });
    }

    /**
     * @return True if the recording was started, false if it was stopped
     */
    public boolean toggleRecording() {
        if (mIsStarted) {
            // stop recording
            mIsStarted = false;
            mIsResumed = false;
            mLocation.stop();
            mBleRecorder.stopRecording();
            mStorageManager.stopStoring();
            mDataStreamer.setResumed(false);
            //mHandler.removeCallbacksAndMessages(null);
            return false;
        }

        // start recording
        mIsStarted = true;
        mIsResumed = true;
        mStorageManager = mDataStreamer.resetState();

        final PublishSubject<Double> latPublisher = mDataStreamer
                .setInput(ActivityFeature.LATITUDE, true);
        final PublishSubject<Double> lonPublisher = mDataStreamer
                .setInput(ActivityFeature.LONGITUDE, true);
        final PublishSubject<Double> speedPublisher = mDataStreamer
                .setInput(ActivityFeature.SPEED_MS, false);
        final PublishSubject<Double> altitudePublisher= mDataStreamer
                .setInput(ActivityFeature.ALTITUDE, true);

        OnLocationUpdatedListener locListener = new OnLocationUpdatedListener() {
            @Override
            public void onLocationUpdated(Location location) {
                // TODO: maybe let this depend on the distance from the last fix and the speed
//                Log.i(TAG, "accuracy is: " + location.getAccuracy());
//                if (location.hasAccuracy() && location.getAccuracy() > ACCURACY_THRES)
//                    // the accuracy is too low to be useful
//                    return;
                latPublisher.onNext(location.getLatitude());
                lonPublisher.onNext(location.getLongitude());
//                if (location.hasSpeed()) {
                    speedPublisher.onNext((double) location.getSpeed());
                    // TODO
                    if (mIsResumed && location.getSpeed() < AUTO_PAUSE_SPEED_THRES
                            && mDataStreamer.getTotalTime() - mLastSpeedGreaterPoint > AUTO_PAUSE_TIME_THRES) {
                        togglePause();
                    } else if (location.getSpeed() > AUTO_PAUSE_SPEED_THRES){
                        mLastSpeedGreaterPoint = mDataStreamer.getTotalTime();
                        if (!mIsResumed)
                            // TODO: do this only if the user didn't pause
                            togglePause();
                    }
//                } else {
//                    // TODO: correct?
//                    speedPublisher.onNext(0.0);
//                }
                if (location.hasAltitude())
                    altitudePublisher.onNext(location.getAltitude());
                //Log.i(TAG, "location changed");
            }
        };
        mLocation.start(locListener);
        mBleRecorder.startRecording(mDataStreamer);
        mDataStreamer.setResumed(true);
        mStorageManager.startStoring();

//        startPeriodicScanning();

        return true;
    }

//    public void stopRecording() {
//        assert mIsStarted;
//        mIsResumed = false;
//        mIsStarted = false;
//        mLocation.stop();
//        mBleRecorder.stopRecording();
//        mTimeHandler.removeCallbacksAndMessages(null);
//        mStorageManager.stopStoring();
//    }

    public boolean isRecording() {
        return mIsStarted;
    }

    /**
     * @return True if recording has commenced, false if it was paused
     */
    public boolean togglePause() {
        assert mIsStarted;
        mIsResumed = !mIsResumed;
        mDataStreamer.setResumed(mIsResumed);
        if (mIsResumed) {
            // TODO: simply stopping the storing is not enough, with this format there will be a jump
            mStorageManager.startStoring();
        } else {
            mStorageManager.stopStoring();
        }
        return mIsResumed;
    }

}
