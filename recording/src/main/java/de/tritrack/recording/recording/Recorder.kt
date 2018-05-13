package de.tritrack.recording.recording

import android.content.Context
import android.util.Log

import io.nlopez.smartlocation.OnLocationUpdatedListener
import io.nlopez.smartlocation.SmartLocation
import io.nlopez.smartlocation.location.config.LocationParams
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesWithFallbackProvider
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import java.util.*

/**
 * Created by till on 22.12.16.
 */

class Recorder private constructor(context: Context) {

    private var mRecorderState = RecorderState.STOPPED
    // TODO: replace this with a function call
    var wasStarted = false
        private set

    private val mLocation: SmartLocation.LocationControl
    private val mBleRecorder: BleRecorder
    private val mDataStreamer: DataStreamer
    private var mStorageManager: StorageManager? = null
    private var mAutoPauseSubscription: Disposable? = null

    val isRecording: Boolean
        get() = mRecorderState != RecorderState.STOPPED

    private val isRunning: Boolean
        get() = mRecorderState == RecorderState.RUNNING

    private enum class RecorderState {
        STOPPED,
        USER_PAUSED,
        AUTO_PAUSED,
        RUNNING,
        RESTARTED
    }

    init {
        mRecorderState = RecorderState.STOPPED
        mLocation = SmartLocation.with(context).location(LocationGooglePlayServicesWithFallbackProvider(context))
        mLocation.config(LocationParams.NAVIGATION)
        mBleRecorder = BleRecorder(context)
        mDataStreamer = DataStreamer()
    }

    fun startBleScan(scanListener: BlePool.SensorDeviceScanListener) {
        mBleRecorder.startNewDevicesScan(scanListener)
    }

    fun stopBleScan() {
        mBleRecorder.stopNewDevicesScan()
    }

    fun getDataObservable(feature: ActFeature, op: OpType): Observable<Double> {
        return mDataStreamer.getOperator(feature, op)
    }

    /**
     * @return True if the recording was started, false if it was stopped
     */
    fun toggleRecording(): Boolean {
        if (mRecorderState != RecorderState.STOPPED) {
            // stop recording
            mRecorderState = RecorderState.STOPPED
            mAutoPauseSubscription!!.dispose()
            mLocation.stop()
            mBleRecorder.stopRecording()
            mStorageManager!!.stopStoring()
            mDataStreamer.setResumed(false)
            return false
        }

        // start recording
        mRecorderState = RecorderState.RUNNING
        wasStarted = true
        mStorageManager = mDataStreamer.resetState()
        // TODO: add settings switch for auto-pause
        addAutoPauseListener()

        val latPublisher = mDataStreamer
                .getInputProvider(ActFeature.LATITUDE)
        val lonPublisher = mDataStreamer
                .getInputProvider(ActFeature.LONGITUDE)
        val altitudePublisher = mDataStreamer
                .getInputProvider(ActFeature.ALTITUDE)

        val locListener = OnLocationUpdatedListener { location ->
            // TODO: maybe let this depend on the distance from the last fix and the speed
            //Log.i(TAG, "accuracy is: " + location.getAccuracy());
            if (location.hasAccuracy() && location.accuracy > ACCURACY_THRES)
            // the accuracy is too low to be useful
                return@OnLocationUpdatedListener

            latPublisher.onNext(location.latitude)
            lonPublisher.onNext(location.longitude)

            if (location.hasAltitude())
                altitudePublisher.onNext(location.altitude)
        }
        mLocation.start(locListener)
        mBleRecorder.startRecording(mDataStreamer)
        mDataStreamer.setResumed(true)
        mStorageManager!!.startStoring()

        //        startPeriodicScanning();

        return true
    }

    fun togglePause(): Boolean {
        return togglePause(false)
    }

    /**
     * @return True if recording has commenced, false if it was paused
     */
    private fun togglePause(autoPaused: Boolean): Boolean {
        assert(mRecorderState != RecorderState.STOPPED)
        if (isRunning) {
            if (autoPaused)
                mRecorderState = RecorderState.AUTO_PAUSED
            else
                mRecorderState = RecorderState.USER_PAUSED
            mDataStreamer.setResumed(false)
            // TODO: simply stopping the storing is not enough, with this format there will be a jump
            mStorageManager!!.stopStoring()
            return false
        } else {
            mRecorderState = RecorderState.RUNNING
            mDataStreamer.setResumed(true)
            // TODO: simply stopping the storing is not enough, with this format there will be a jump
            mStorageManager!!.startStoring()
            return true
        }
    }

    private fun addAutoPauseListener() {
        mAutoPauseSubscription = mDataStreamer.getOperator(ActFeature.SPEED_KMH, OpType.ID).forEach(
                object: Consumer<Double> {
            private var lastSufficientSpeedTime = 0.0

            override fun accept(newSpeed: Double) {
                // TODO: can it happen that there are no GPS updates anymore and newSpeed won't get
                // updated correctly?
                // TODO: implement different auto-pause threshold depending on the activity
                if (isRunning && newSpeed < AUTO_PAUSE_SPEED_KMH_THRES
                        && mDataStreamer.totalTime - lastSufficientSpeedTime > AUTO_PAUSE_TIME_THRES) {
                    // TODO: update the UI (the pause button)
                    togglePause(true)
                } else if (newSpeed >= AUTO_PAUSE_SPEED_KMH_THRES) {
                    lastSufficientSpeedTime = mDataStreamer.totalTime
                    if (mRecorderState == RecorderState.AUTO_PAUSED)
                        togglePause(true)
                }
            }
        })
    }

    companion object {

        private const val TAG = "de.tritrack.Recorder"
        private const val ACCURACY_THRES = 50f
        private const val AUTO_PAUSE_SPEED_KMH_THRES = 5f //1.5f;
        //private const val AUTO_PAUSE_SPEED_KMH_THRES = -1.f;
        private const val AUTO_PAUSE_TIME_THRES = 5f

        private var instance: Recorder? = null

        fun getInstance(context: Context): Recorder {
            // TODO: could probably be made more efficient using double-checked locking
            synchronized(this) {
                if (instance == null)
                    instance = Recorder(context)
                return instance!!
            }
        }
    }

}
