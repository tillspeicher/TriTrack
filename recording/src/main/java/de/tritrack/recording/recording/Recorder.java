package de.tritrack.recording.recording;

import android.content.Context;
import android.location.Location;

import java.util.Map;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesWithFallbackProvider;

import io.reactivex.subjects.PublishSubject;

/**
 * Created by till on 22.12.16.
 */

public class Recorder {

    private enum RecorderState {
        STOPPED,
        USER_PAUSED,
        AUTO_PAUSED,
        RUNNING
    }

    private static final String TAG = "de.tritrack.Recorder";
    private static final float ACCURACY_THRES = 50.f;
    private static final float AUTO_PAUSE_SPEED_KMH_THRES = 6.f; //1.5f;
    //private static final float AUTO_PAUSE_SPEED_KMH_THRES = -1.f;
    private static final float AUTO_PAUSE_TIME_THRES = 10.f;

    private RecorderState mRecorderState = RecorderState.STOPPED;
    // TODO: replace this with a function call

    private SmartLocation.LocationControl mLocation;
    private BleRecorder mBleRecorder;
    private DataStreamer mDataStreamer;
    private StorageManager mStorageManager;

    private static Recorder instance = null;

    public static Recorder getInstance(Context context) {
        if (instance == null)
            instance = new Recorder(context);
        return instance;
    }

    private Recorder(Context context) {
        mRecorderState = RecorderState.STOPPED;
        mLocation = SmartLocation.with(context).location(new LocationGooglePlayServicesWithFallbackProvider(context));
        mLocation.config(LocationParams.NAVIGATION);
        mBleRecorder = new BleRecorder(context);
        mDataStreamer = new DataStreamer();
    }

    public void startBleScan(BlePool.SensorDeviceScanListener scanListener) {
        mBleRecorder.startNewDevicesScan(scanListener);
    }

    public void stopBleScan() {
        mBleRecorder.stopNewDevicesScan();
    }

    public void addDataListeners(Map<ActivityFeature, UICommunication.UIDataListener> listeners) {
        // TODO: add settings switch for auto-pause
//        addAutoPauseListener(listeners);
        mDataStreamer.addDataListeners(listeners);
    }

    private void addAutoPauseListener(Map<ActivityFeature, UICommunication.UIDataListener> listeners) {
        // intercept speed listener for auto-pause
        final UICommunication.UIDataListener autoPauseListener = new UICommunication.UIDataListener() {
            private double lastSufficientSpeedTime = 0.f;

            @Override
            public void onFeatureChanged(double newSpeed) {
                // TODO: can it happen that there are no GPS updates anymore and newSpeed won't get
                // updated correctly?
                // TODO: implement different auto-pause threshold depending on the activity
                if (isRunning() && newSpeed < AUTO_PAUSE_SPEED_KMH_THRES
                        && mDataStreamer.getTotalTime() - lastSufficientSpeedTime > AUTO_PAUSE_TIME_THRES) {
                    // TODO: update the UI (the pause button)
                    togglePause(true);
                } else if (newSpeed >= AUTO_PAUSE_SPEED_KMH_THRES) {
                    lastSufficientSpeedTime = mDataStreamer.getTotalTime();
                    if (mRecorderState == RecorderState.AUTO_PAUSED)
                        togglePause(true);
                }
            }
        };
        final UICommunication.UIDataListener uiSpeedListener = listeners.get(ActivityFeature.SPEED_KMH);
        UICommunication.UIDataListener speedListener;
        if (uiSpeedListener == null) {
            speedListener = autoPauseListener;
        } else {
            speedListener = new UICommunication.UIDataListener() {
                @Override
                public void onFeatureChanged(double newSpeed) {
                    uiSpeedListener.onFeatureChanged(newSpeed);
                    autoPauseListener.onFeatureChanged(newSpeed);
                }
            };
        }
        listeners.put(ActivityFeature.SPEED_KMH, speedListener);
    }

    /**
     * @return True if the recording was started, false if it was stopped
     */
    public boolean toggleRecording() {
        if (mRecorderState != RecorderState.STOPPED) {
            // stop recording
            mRecorderState = RecorderState.STOPPED;
            mLocation.stop();
            mBleRecorder.stopRecording();
            mStorageManager.stopStoring();
            mDataStreamer.setResumed(false);
            return false;
        }

        // start recording
        mRecorderState = RecorderState.RUNNING;
        mStorageManager = mDataStreamer.resetState();

        final PublishSubject<Double> latPublisher = mDataStreamer
                .setInput(ActivityFeature.LATITUDE, true);
        final PublishSubject<Double> lonPublisher = mDataStreamer
                .setInput(ActivityFeature.LONGITUDE, true);
        final PublishSubject<Double> altitudePublisher= mDataStreamer
                .setInput(ActivityFeature.ALTITUDE, true);

        OnLocationUpdatedListener locListener = new OnLocationUpdatedListener() {
            @Override
            public void onLocationUpdated(Location location) {
                // TODO: maybe let this depend on the distance from the last fix and the speed
                //Log.i(TAG, "accuracy is: " + location.getAccuracy());
                if (location.hasAccuracy() && location.getAccuracy() > ACCURACY_THRES)
                    // the accuracy is too low to be useful
                    return;

                latPublisher.onNext(location.getLatitude());
                lonPublisher.onNext(location.getLongitude());

                if (location.hasAltitude())
                    altitudePublisher.onNext(location.getAltitude());
            }
        };
        mLocation.start(locListener);
        mBleRecorder.startRecording(mDataStreamer);
        mDataStreamer.setResumed(true);
        mStorageManager.startStoring();

//        startPeriodicScanning();

        return true;
    }

    public boolean isRecording() {
        return mRecorderState != RecorderState.STOPPED;
    }

    private boolean isRunning() {
        return mRecorderState == RecorderState.RUNNING;
    }

    public boolean togglePause() {
        return togglePause(false);
    }

    /**
     * @return True if recording has commenced, false if it was paused
     */
    private boolean togglePause(boolean autoPaused) {
        assert mRecorderState != RecorderState.STOPPED;
        if (isRunning()) {
            if (autoPaused)
                mRecorderState = RecorderState.AUTO_PAUSED;
            else
                mRecorderState = RecorderState.USER_PAUSED;
            mDataStreamer.setResumed(false);
            // TODO: simply stopping the storing is not enough, with this format there will be a jump
            mStorageManager.stopStoring();
            return false;
        } else {
            mRecorderState = RecorderState.RUNNING;
            mDataStreamer.setResumed(true);
            // TODO: simply stopping the storing is not enough, with this format there will be a jump
            mStorageManager.startStoring();
            return true;
        }
    }

}
