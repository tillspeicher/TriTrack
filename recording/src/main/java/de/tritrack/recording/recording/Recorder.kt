package de.tritrack.recording.recording

import android.content.Context

import io.nlopez.smartlocation.OnLocationUpdatedListener
import io.nlopez.smartlocation.SmartLocation
import io.nlopez.smartlocation.location.config.LocationParams
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesWithFallbackProvider
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import rx.subjects.BehaviorSubject
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

/**
 * Created by till on 22.12.16.
 */

class Recorder private constructor(context: Context) {

    private var mRecorderState = RecorderState.STOPPED
    // TODO: replace this with a function call
    var wasStarted = false
        private set

    private val mLocation: SmartLocation.LocationControl
    private val mBleRecorder = BleRecorder(context)
    private val mDataStreamer = DataStreamer()
    private var mStorageManager: StorageManager? = null
    private var mAutoPauseSubscription: Disposable? = null

    val isRecording: Boolean
        get() = mRecorderState != RecorderState.STOPPED

    val isRunning: Boolean
        get() = mRecorderState == RecorderState.RUNNING

    private enum class RecorderState {
        STOPPED,
        USER_PAUSED,
        AUTO_PAUSED,
        RUNNING,
        RESTARTED
    }

    private val aggregatedActivityFeatures: MutableSet<ActivityData> = HashSet()
    private val segmentSubscribers: MutableList<(Int) -> Unit> = ArrayList()
    private val overallObservables: MutableMap<ActivityData, BehaviorSubject<Double>> = HashMap()
    // We need to hold on to references here to prevent the Observables getting garbage collected
    // in the UI
    private val segmentObservables: MutableList<Map<ActivityData, BehaviorSubject<Double>>> =
            ArrayList()
    private val curSegmentSubscriptions: MutableList<Disposable> = ArrayList()

    init {
        mLocation = SmartLocation.with(context).location(
                LocationGooglePlayServicesWithFallbackProvider(context))
        mLocation.config(LocationParams.NAVIGATION)
    }

    fun startBleScan(scanListener: BlePool.SensorDeviceScanListener) {
        mBleRecorder.startNewDevicesScan(scanListener)
    }

    fun stopBleScan() {
        mBleRecorder.stopNewDevicesScan()
    }

    fun monitorFeatures(features: Set<ActivityData>, listener: (Int) -> Unit) {
        assert(mRecorderState == RecorderState.STOPPED)

        aggregatedActivityFeatures.addAll(features)
        segmentSubscribers.add(listener)
        features.forEach {
            if (!overallObservables.contains(it)) {
                // TODO: check share with what happens in UI currently
                val interceptSubject = BehaviorSubject.create<Double>()
                mDataStreamer.getOperator(it).forEach { interceptSubject.onNext(it) }
                overallObservables[it] = interceptSubject
            }
        }
    }

    fun getSegmentObservations(segmentId: Int, features: Set<ActivityData>):
            Map<ActivityData, BehaviorSubject<Double>> {
        if (segmentId == GLOBAL_SEGMENT_ID)
            return adjustToSubscriber(overallObservables, features)
        return adjustToSubscriber(segmentObservables[segmentId], features)
    }

    fun nextSegment() {
        // unsubscribe the previous segment observables
        curSegmentSubscriptions.forEach { it.dispose() }
        curSegmentSubscriptions.clear()

        val newSegmentObservables = aggregatedActivityFeatures.map {
            val sourceOp = mDataStreamer.getOperator(it)
            // TODO: try to find a better way for canceling subscriptions that does not involve
            // creating an intermediate Behavior Subject
            val interceptSubject = BehaviorSubject.create<Double>()
            val subscription = sourceOp.forEach { interceptSubject.onNext(it) }
            curSegmentSubscriptions.add(subscription)
            // TODO: do we need replay and ref count here?
            it to interceptSubject }.toMap()
        val segmentId = segmentObservables.size
        segmentObservables.add(newSegmentObservables)
        segmentSubscribers.forEach { it(segmentId) }
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
            mStorageManager = null
            return false
        }

        // start recording
        mRecorderState = RecorderState.RUNNING
        wasStarted = true
        mDataStreamer.resetState()
        mStorageManager = StorageManager(mDataStreamer)
        // TODO: add settings switch for auto-pause
        addAutoPauseListener()
        nextSegment()

        val latPublisher = mDataStreamer.getInputProvider(ActFeature.LATITUDE)
        val lonPublisher = mDataStreamer.getInputProvider(ActFeature.LONGITUDE)
        val altitudePublisher = mDataStreamer.getInputProvider(ActFeature.ALTITUDE)

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
        val speedFeature = ActivityData(ActFeature.SPEED_KMH, OpType.ID)
        mAutoPauseSubscription = mDataStreamer.getOperator(speedFeature).forEach(
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

        const val GLOBAL_SEGMENT_ID = -1

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

        private fun adjustToSubscriber(source: Map<ActivityData, BehaviorSubject<Double>>,
                                       target: Set<ActivityData>):
                Map<ActivityData, BehaviorSubject<Double>> {
            return source.filterKeys { featureDescriptor: ActivityData ->
                target.contains(featureDescriptor) }
        }

    }

}
