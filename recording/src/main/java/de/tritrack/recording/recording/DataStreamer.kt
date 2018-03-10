package de.tritrack.recording.recording

import android.location.Location
import android.os.Handler
import android.os.SystemClock
import android.util.Log

import java.util.ArrayList
import java.util.HashMap

//import rx.Observable;
//import rx.Observer;
//import rx.functions.Function;
//import rx.functions.FuncN;
//import rx.subjects.PublishSubject;

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.subjects.PublishSubject

/**
 * Created by till on 03.06.17.
 */

internal class DataStreamer {

    private val mInputs: MutableMap<ActivityFeature, PublishSubject<Double>>
    private val mProviders: MutableMap<ActivityFeature, Observable<Double>>
    private val mDataListeners: MutableMap<ActivityFeature, UICommunication.UIDataListener>
    private val mOperators: MutableList<Operator>
    private var mStorageManager: StorageManager? = null

    private var mTimePublisher: PublishSubject<Double>? = null
    private val mTimeHandler: Handler

    private var mIsResumed = false
    private var mCheckpointTimeMs: Long = 0
    private var mTimeOffsetMs: Long = 0

    private val elapsedTimeSinceResume: Long
        get() {
            val curMs = SystemClock.elapsedRealtime()
            return curMs - mCheckpointTimeMs
        }

    val totalTime: Double
        get() = if (!mIsResumed) {
            mTimeOffsetMs / 1000.0
        } else {
            (elapsedTimeSinceResume + mTimeOffsetMs) / 1000.0
        }

    init {
        mInputs = HashMap()
        mProviders = HashMap()
        mDataListeners = HashMap<ActivityFeature, UICommunication.UIDataListener>()
        mOperators = ArrayList()
        mTimeHandler = Handler()
    }

    fun addDataListeners(listeners: Map<ActivityFeature, UICommunication.UIDataListener>) {
        Log.i(TAG, "Settiong data listeners")
        mDataListeners.putAll(listeners)
    }

    fun resetState(): StorageManager {
        // TODO: reset the UI
        mProviders.clear()
        mOperators.clear()
        //mDataListeners.clear();
        mStorageManager = StorageManager()

        mTimeHandler.removeCallbacksAndMessages(null)
        mTimeOffsetMs = 0
        mTimePublisher = setInput(ActivityFeature.TIME_S, true)

        return mStorageManager!!
    }

    fun setResumed(isResumed: Boolean) {
        mIsResumed = isResumed
        if (isResumed) {
            startTimeRecording()
        } else {
            mTimeHandler.removeCallbacksAndMessages(null)
            mTimeOffsetMs += elapsedTimeSinceResume
        }
    }

    private fun startTimeRecording() {
        // TODO: do delays matter for accuracy, are they detectable?
        mCheckpointTimeMs = SystemClock.elapsedRealtime()
        mTimeHandler.post(object : Runnable {
            override fun run() {
                mTimePublisher!!.onNext(totalTime)
                mTimeHandler.postDelayed(this, TIME_DELAY_MS.toLong())
            }
        })
    }

    fun hasInputSource(activityFeature: ActivityFeature): Boolean {
        return mProviders.containsKey(activityFeature)
    }

    fun setInput(feature: ActivityFeature,
                 logFeature: Boolean): PublishSubject<Double> {
        Log.i(TAG, "adding source for feature " + feature)

        // TODO: this way all inputs are produced using the same PublishSubject, maybe use different
        // ones for every input source and decide based on priority and availability which source
        // to use at any given time
        // TODO: in case no inputs are produced for some time or all input sources are unavailable/
        // disconnected, 0 or some other default values should be produced
        var inSubject: PublishSubject<Double>? = mInputs[feature]
        if (inSubject == null) {
            inSubject = PublishSubject.create()
            mInputs[feature] = inSubject
            val obs = inSubject!!.map({v -> v})
            addUiObserver(feature, obs)
            if (logFeature) {
                mStorageManager!!.addFeature(feature)
                addLoggingObserver(feature, obs)
            }
            mProviders[feature] = obs
            checkDependantOperators(feature)
        }

        return inSubject
    }

    @Throws(IllegalArgumentException::class)
    private fun addOperator(dependingFeatures: Array<ActivityFeature>, resFeature: ActivityFeature,
                            op: Operator, logFeature: Boolean) {
        if (mProviders.containsKey(resFeature))
            throw IllegalArgumentException("Cannot add operator for feature $resFeature twice.")
        Log.i(TAG, "adding operator for feature " + resFeature)

        val inObservables = ArrayList<Observable<Double>>()
        for (depFeature in dependingFeatures) {
            val inObs = mProviders[depFeature]
            if (inObs != null)
                //inObservables.add(inObs.onBackpressureLatest());
                // TODO: use the last one
                inObservables.add(inObs)
            else
                throw IllegalArgumentException("No provider for feature $depFeature available.")
        }

//        resObs = Observable.zip(inObservables, Function { values ->
        val resObs: Observable<Double> = Observable.combineLatest(inObservables, Function { values ->
            try {
                val doubleVals = Array<Double>(values.size, { pos -> values[pos] as Double })
                return@Function op.apply(doubleVals)
            } catch (e: Exception) {
                // TODO: catching all exceptions is problematic
                Log.e(TAG, "Error in operator $resFeature:" + e.message)
                return@Function 0.0
            }
        })
        addUiObserver(resFeature, resObs)
        if (logFeature) {
            mStorageManager!!.addFeature(resFeature)
            addLoggingObserver(resFeature, resObs)
        }

        mProviders[resFeature] = resObs
        mOperators.add(op)
        checkDependantOperators(resFeature)
    }

    // TODO: maybe move the declaration of the correspondences
    private fun checkDependantOperators(feature: ActivityFeature) {
        when (feature) {
            ActivityFeature.HEART_RATE -> {
                addOperator(arrayOf(ActivityFeature.HEART_RATE),
                        ActivityFeature.AVG_HEART_RATE, TimeAvgOperator(), false)
                addOperator(arrayOf(ActivityFeature.HEART_RATE),
                        ActivityFeature.MAX_HEART_RATE, MaxOperator(), false)
            }
            ActivityFeature.POWER_LEFT, ActivityFeature.POWER_RIGHT -> {
                if (!mProviders.containsKey(ActivityFeature.POWER_RIGHT) ||
                        !mProviders.containsKey(ActivityFeature.POWER_LEFT))
                    return
                // TODO: the time correspondence of the values should be checked
                addOperator(arrayOf(ActivityFeature.POWER_LEFT, ActivityFeature.POWER_RIGHT),
                        ActivityFeature.POWER_COMBINED, object : Operator() {
                    override fun apply(vals: Array<Double>): Double? {
                        return vals[0] + vals[1]
                    }
                },false)
            }
            ActivityFeature.POWER_COMBINED -> {
                addOperator(arrayOf(ActivityFeature.POWER_COMBINED),
                        ActivityFeature.AVG_POWER_COMBINED, TimeAvgOperator(), false)
                addOperator(arrayOf(ActivityFeature.POWER_COMBINED),
                        ActivityFeature.MAX_POWER_COMBINED, MaxOperator(), false)
            }
            ActivityFeature.LATITUDE, ActivityFeature.LONGITUDE -> {
                if (!mProviders.containsKey(ActivityFeature.LONGITUDE) ||
                        !mProviders.containsKey(ActivityFeature.LATITUDE))
                    return
                addOperator(arrayOf(ActivityFeature.LATITUDE, ActivityFeature.LONGITUDE),
                        ActivityFeature.DISTANCE_RAW_M, object : Operator() {
                    internal var lastLat: Double? = null
                    internal var lastLong: Double? = null
                    internal var dist = 0.0
                    override fun apply(vals: Array<Double>): Double? {
                        val curLat = vals[0]
                        val curLong = vals[1]
                        if (lastLat == null) {
                            lastLat = curLat
                            lastLong = curLong
                            return dist
                        }
                        val distRes = FloatArray(1)
                        Location.distanceBetween(lastLat!!, lastLong!!, curLat, curLong, distRes)
                        //                                Log.i(TAG, "distance is " + distRes[0]);
                        dist += distRes[0].toDouble()
                        lastLat = curLat
                        lastLong = curLong
                        return dist
                    }
                }, false /*ok?*/)
            }
            ActivityFeature.DISTANCE_RAW_M -> {
                addOperator(arrayOf(ActivityFeature.DISTANCE_RAW_M),
                        ActivityFeature.DISTANCE_M, object : Operator() {
                    internal var totalDist = 0.0
                    internal var lastDist: Double? = 0.0

                    override fun apply(vals: Array<Double>): Double? {
                        if (!mIsResumed) {
                            lastDist = null
                            return totalDist
                        }
                        val rawDist = vals[0]
                        if (lastDist == null)
                            lastDist = rawDist
                        val distDiff = rawDist - lastDist!!
                        totalDist += distDiff
                        return totalDist
                    }
                }, false)
                addOperator(arrayOf(ActivityFeature.DISTANCE_RAW_M),
                        ActivityFeature.SPEED_MS, object : TimedOperator() {
                    // TODO: is it necessary to force updates in periodic time intervals in
                    // case the GPS connection breaks?
                    internal var lastDist: Double? = null

                    override fun apply(vals: Array<Double>): Double? {
                        val timeDiff = timeCheckpoint(true)
                        val distM = vals[0]
                        if (lastDist == null || timeDiff <= 0.0) {
                            lastDist = distM
                            return 0.0
                        }
                        val distDiff = distM - lastDist!!
                        lastDist = distM
                        return distDiff / timeDiff
                    }
                }, false)
            }
            ActivityFeature.DISTANCE_M -> addOperator(arrayOf(ActivityFeature.DISTANCE_M),
                    ActivityFeature.DISTANCE_KM, object : Operator() {
                override fun apply(vals: Array<Double>): Double? {
                    return vals[0] / 1000.0
                }
            }, false)
            ActivityFeature.SPEED_MS -> {
                addOperator(arrayOf(ActivityFeature.SPEED_MS),
                        ActivityFeature.SPEED_KMH, object : Operator() {
                    override fun apply(speedMs: Array<Double>): Double? {
                        return speedMs[0] * 3.6
                    }
                }, true)
                addOperator(arrayOf(ActivityFeature.SPEED_MS),
                        ActivityFeature.PACE, object : Operator() {
                    override fun apply(vals: Array<Double>): Double? {
                        val speed = vals[0]
                        return if (speed == 0.0) 0.0 else 100.0 / 6.0 / speed
                    }
                }, false)
            }
            ActivityFeature.SPEED_KMH -> {
                addOperator(arrayOf(ActivityFeature.SPEED_KMH),
                        ActivityFeature.AVG_SPEED_KMH, TimeAvgOperator(), false)
                addOperator(arrayOf(ActivityFeature.SPEED_KMH),
                        ActivityFeature.MAX_SPEED_KMH, MaxOperator(), false)
            }
            ActivityFeature.PACE -> addOperator(arrayOf(ActivityFeature.PACE),
                    ActivityFeature.AVG_PACE, TimeAvgOperator(), false)
            ActivityFeature.ALTITUDE -> addOperator(arrayOf(ActivityFeature.ALTITUDE),
                    ActivityFeature.ELEVATION_GAIN, object : Operator() {
                //new Function<Double[], Double>() {
                internal var gain = 0.0
                internal var lastAltitude: Double? = null
                override fun apply(value: Array<Double>): Double? {
                    if (!mIsResumed) {
                        lastAltitude = null
                        return gain
                    }
                    val curAltitude = value[0]
                    if (lastAltitude == null)
                        lastAltitude = curAltitude
                    gain += Math.max(curAltitude - lastAltitude!!, 0.0)
                    lastAltitude = curAltitude
                    return gain
                }
            }, false)
            ActivityFeature.LAST_WHEEL_EVENT -> {
                val wheel_circumference = 2.76 // TODO: check, make configurable
                addOperator(arrayOf(ActivityFeature.CUMULATIVE_WHEEL_REVOLUTIONS,
                        ActivityFeature.LAST_WHEEL_EVENT), ActivityFeature.DISTANCE_KM_REV,
                        object : Operator() {
                            override fun apply(vals: Array<Double>): Double? {
                                return vals[0] * wheel_circumference / 1000
                            }
                        }, false)
                addOperator(arrayOf(ActivityFeature.CUMULATIVE_WHEEL_REVOLUTIONS,
                        ActivityFeature.LAST_WHEEL_EVENT), ActivityFeature.SPEED_KMH_REV,
                        EventPerMinOperator(wheel_circumference * 0.06), false)
            }
            ActivityFeature.LAST_CRANK_EVENT -> addOperator(arrayOf(
                    ActivityFeature.CUMULATIVE_CRANK_REVOLUTIONS, ActivityFeature.LAST_CRANK_EVENT),
                    ActivityFeature.CADENCE,
                    EventPerMinOperator(1.0), true)
            ActivityFeature.CADENCE -> addOperator(arrayOf(ActivityFeature.CADENCE),
                    ActivityFeature.AVG_CADENCE, TimeAvgOperator(), false)
        }
    }

    private fun addUiObserver(feature: ActivityFeature, obs: Observable<Double>) {
        val listener = mDataListeners[feature]
        if (listener == null) {
            Log.i(TAG, "cannot find listener for feature " + feature)
            // TODO: maybe change this such that you can only register listeners if there is a
            // provider for the feature
            return
        }
        obs.subscribe(object : Observer<Double> {
            override fun onSubscribe(d: Disposable) {}

            override fun onNext(v: Double) {
                listener.onFeatureChanged(v)
            }

            override fun onError(e: Throwable) {
                Log.i(TAG, "Error updating the UI: " + e.message, e)
            }

            override fun onComplete() {
                Log.i(TAG, "Completed UI updating")
            }
        })
    }

    private fun addLoggingObserver(feature: ActivityFeature, obs: Observable<Double>) {
        obs.subscribe(object : Observer<Double> {
            override fun onSubscribe(d: Disposable) {}

            override fun onNext(v: Double) {
                //Log.i(TAG, "receiving value for " + feature + " for logging: " + val);
                mStorageManager!!.setValue(feature, v)
            }

            override fun onError(e: Throwable) {
                Log.i(TAG, "Error in logging subscription: " + e.message, e)
            }

            override fun onComplete() {
                Log.i(TAG, "Completed logging subscription")
            }
        })
    }

    private abstract inner class Operator : Function<Array<Double>, Double>

    private abstract inner class TimedOperator : Operator() {
        private var lastTimeMs: Long = -1

        internal fun timeCheckpoint(ignoreResumes: Boolean): Double {
            if (!ignoreResumes && !mIsResumed) {
                lastTimeMs = -1
                return -1.0
            }
            val curMs = SystemClock.elapsedRealtime()
            val timeDiff: Long
            if (lastTimeMs < 0) {
                // TODO: if the recording was paused and resumed but this method was not called in
                // between, this will be off, maybe we should use a counter for resumes
                timeDiff = curMs - mCheckpointTimeMs
            } else {
                timeDiff = curMs - lastTimeMs
            }
            lastTimeMs = curMs
            return timeDiff / 1000.0
        }

    }

    private inner class TimeAvgOperator : TimedOperator() {
        private var lastAvg = -1.0

        override fun apply(vals: Array<Double>): Double {
            val timeDiff = timeCheckpoint(false)
            if (timeDiff < 0)
                return if (lastAvg < 0) 0.0 else lastAvg

            val totalTime = totalTime
            val curVal = vals[0]
            if (lastAvg < 0)
                lastAvg = curVal
            val curFrac = if (totalTime == 0.0) 0.0 else timeDiff / totalTime
            // TODO: this is a stepwise function, should we interpolate?
            val avgVal = curVal * curFrac + (1.0 - curFrac) * lastAvg
            lastAvg = avgVal
            assert(lastAvg >= 0)
            return avgVal
        }
    }

    private inner class MaxOperator : Operator() {
        private var maxVal = 0.0

        override fun apply(vals: Array<Double>): Double {
            if (!mIsResumed)
                maxVal
            val v = vals[0]
            assert(v >= 0)
            maxVal = Math.max(v, maxVal)
            return maxVal
        }
    }

    private inner class EventPerMinOperator(multiplicator: Double) : Operator() {

        private val multiplicator = multiplicator
        private var lastCumulativeRevolutions = 0.0
        private var lastTime = -1.0

        override fun apply(doubles: Array<Double>): Double {
            val revolutions = doubles[0]
            val time = doubles[1]
            if (lastTime < 0) {
                lastCumulativeRevolutions = revolutions
                lastTime = time
                return 0.0
            }
            if (time == lastTime)
                return 0.0

            var revDiff = revolutions - lastCumulativeRevolutions
            if (revolutions < lastCumulativeRevolutions)
                revDiff += 65535.0

            var timeDiff = time - lastTime
            if (time < lastTime)
                timeDiff += 65535.0

            val rpm = revDiff * 60.0 * 1024.0 / timeDiff

            lastCumulativeRevolutions = revolutions
            lastTime = time
            return rpm * multiplicator
        }
    }

    companion object {

        private val TAG = "DataStreamer"
        private val TIME_DELAY_MS = 1000
    }

}
