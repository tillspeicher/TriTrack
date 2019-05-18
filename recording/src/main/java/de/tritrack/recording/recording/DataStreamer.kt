package de.tritrack.recording.recording

import android.location.Location
import android.os.Handler
import android.os.SystemClock
import android.util.Log

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import io.reactivex.subjects.PublishSubject

/**
 * Created by till on 03.06.17.
 */

internal class DataStreamer {

    private val mInputs: MutableMap<ActFeature, PublishSubject<Double>>
    private val mProvidedInputs: MutableSet<ActFeature>
    private val mProviders: MutableMap<ActFeature, Observable<Double>>

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
        mProvidedInputs = HashSet()
        mProviders = HashMap()
        //mDataListeners = HashMap()
        mTimeHandler = Handler()
    }

//    fun addDataListener(actFeature: ActFeature, opType: OpType,
//                        listener: UICommunication.UIDataListener): Disposable {
//        Log.i(TAG, "Adding listener for $actFeature, $opType")
//        val operator = getOperator(opType, actFeature)
////        return operator.subscribe { newVal -> listener.onFeatureChanged(newVal) }
//        return operator.forEach { listener.onFeatureChanged(it) }
//    }

    fun resetState() {
        // TODO: reset the UI

        //mInputs.clear()
        //mProviders.clear()
        mProvidedInputs.clear()

        mTimeHandler.removeCallbacksAndMessages(null)
        mTimeOffsetMs = 0
        mTimePublisher = getInputProvider(ActFeature.DURATION_S)
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

    // TODO: get rid of this when multi-source inputs with fallback behaviour are implemented
    fun hasInputSource(actFeature: ActFeature): Boolean {
        return mProvidedInputs.contains(actFeature)
    }

    fun getInputProvider(feature: ActFeature, provideInput: Boolean = false): PublishSubject<Double> {
        Log.i(TAG, "adding source for feature " + feature)

        // TODO: this way all inputs are produced using the same PublishSubject, maybe use different
        // ones for every input source and decide based on priority and availability which source
        // to use at any given time
        // TODO: in case no inputs are produced for some time or all input sources are unavailable/
        // disconnected, 0 or some other default values should be produced
        var inSubject: PublishSubject<Double>? = mInputs[feature]
        // TODO: check this assertion
        if (inSubject == null) {
            inSubject = PublishSubject.create()
            mInputs[feature] = inSubject
            registerProvider(feature, inSubject)
        }

        if (provideInput)
            assert(mProvidedInputs.add(feature))

        return inSubject!!
    }

    fun getOperator(dataDescriptor: ActivityData):
            Observable<Double> {
        val featureProvider = getProvider(dataDescriptor.feature)
        // TODO: maybe store the wrapped operators somewhere to make sure they're not garbage
        // collected when the activity moves to the background
        return when (dataDescriptor.op) {
            OpType.ID -> featureProvider
            OpType.AVG -> featureProvider.map( TimeAvgOperator() )
            OpType.MAX -> featureProvider.map( MaxOperator() )
            OpType.NORM_AVG -> featureProvider.map( TimeAvgOperator(true) )
            // TODO: the offset operator should be initialized with the current value for the targetFeature
            // at this point in time, otherwise it might get initialized too late
            OpType.OFFSET -> featureProvider.map( OffsetOperator() )
        }
    }

    private fun getProvider(targetFeature: ActFeature): Observable<Double> {
        if (mProviders.containsKey(targetFeature))
            return mProviders[targetFeature]!!

        val resObs: Observable<Double> = when (targetFeature) {
            ActFeature.DISTANCE_INCREMENT_M -> {
                val latObs = getProvider(ActFeature.LATITUDE)
                val longObs = getProvider(ActFeature.LONGITUDE)
                Observable.zip(latObs, longObs,
                        object : BiFunction<Double, Double, Double> {
                            internal var lastLat: Double? = null
                            internal var lastLong: Double? = null

                            override fun apply(curLat: Double, curLong: Double): Double {
                                if (lastLat == null) {
                                    lastLat = curLat
                                    lastLong = curLong
                                    return 0.0
                                }
                                val distRes = FloatArray(1)
                                Location.distanceBetween(lastLat!!, lastLong!!, curLat, curLong, distRes)
                                lastLat = curLat
                                lastLong = curLong
                                return distRes[0].toDouble()
                            }
                        })
            }
            ActFeature.DISTANCE_M -> {
                val distIncObs = getProvider(ActFeature.DISTANCE_INCREMENT_M)
                distIncObs.map( object : Function<Double, Double> {
                    internal var totalDist = 0.0

                    override fun apply(increment: Double): Double {
                        if (!mIsResumed)
                            return totalDist
                        totalDist += increment
                        return totalDist
                    }
                })
            }
            ActFeature.SPEED_MS -> {
                val distIncObs = getProvider(ActFeature.DISTANCE_INCREMENT_M)
                distIncObs.map( object: TimedOperator(false) {
                    // TODO: is it necessary to force updates in periodic time intervals in
                    // case the GPS connection breaks?

                    override fun apply(distDiff: Double): Double {
                        val timeDiff = timeIncrement()
                        if (timeDiff <= 0.0) {
                            return 0.0
                        }
                        return distDiff / timeDiff
                    }
                })
            }
            ActFeature.DISTANCE_KM -> {
                val distMObs = getProvider(ActFeature.DISTANCE_M)
                distMObs.map { distM -> distM / 1000.0 }
            }
            ActFeature.SPEED_KMH -> {
                val speedMsObs = getProvider(ActFeature.SPEED_MS)
                speedMsObs.map { speedMS -> speedMS * 3.6 }
            }
            ActFeature.PACE -> {
                val speedMsObs = getProvider(ActFeature.SPEED_MS)
                speedMsObs.map { speedMS ->  if (speedMS == 0.0) 0.0 else 100.0 / 6.0 / speedMS }
            }
            ActFeature.ELEVATION_GAIN -> {
                val altitudeObs = getProvider(ActFeature.ALTITUDE)
                altitudeObs.map(object : Function<Double, Double> {
                    internal var gain = 0.0
                    internal var lastAltitude: Double? = null

                    override fun apply(curAltitude: Double): Double? {
                        if (!mIsResumed) {
                            lastAltitude = null
                            return gain
                        }
                        if (lastAltitude == null)
                            lastAltitude = curAltitude
                        gain += Math.max(curAltitude - lastAltitude!!, 0.0)
                        lastAltitude = curAltitude
                        return gain
                    }
                })
            }
            // TODO: merge this with the GPS-based distance computation and use only one of them
            ActFeature.DISTANCE_KM_REV -> {
                val wheel_circumference = WHEEL_CIRCUMFERENCE
                val cumWheelRevObs = getProvider(ActFeature.CUMULATIVE_WHEEL_REVOLUTIONS)
                cumWheelRevObs.map { cumWheelRevs -> cumWheelRevs * wheel_circumference / 1000 }
            }
            ActFeature.SPEED_KMH_REV -> {
                val wheel_circumference = WHEEL_CIRCUMFERENCE
                val cumWheelRevObs = getProvider(ActFeature.CUMULATIVE_WHEEL_REVOLUTIONS)
                val lastWheelEventObs = getProvider(ActFeature.LAST_WHEEL_EVENT)
                Observable.zip(cumWheelRevObs, lastWheelEventObs,
                        EventPerMinOperator(wheel_circumference * 0.06))
            }
            ActFeature.CADENCE -> {
                val cumCrankRevObs = getProvider(ActFeature.CUMULATIVE_CRANK_REVOLUTIONS)
                val lastCrankEventObs = getProvider(ActFeature.LAST_CRANK_EVENT)
                Observable.zip(cumCrankRevObs, lastCrankEventObs, EventPerMinOperator(1.0))
            }
            ActFeature.POWER_COMBINED -> {
                val powerLeftObs = getProvider(ActFeature.POWER_LEFT)
                val powerRightObs = getProvider(ActFeature.POWER_RIGHT)
                Observable.combineLatest(powerLeftObs, powerRightObs, BiFunction<Double, Double, Double> {
                    powerLeft, powerRight -> powerLeft + powerRight
                })
            }
            // getInputProvider() already registers so no need to register again
            else -> return getInputProvider(targetFeature)

        }
        return registerProvider(targetFeature, resObs)
    }

    @Throws(IllegalArgumentException::class)
    private fun registerProvider(resFeature: ActFeature, obs: Observable<Double>):
            Observable<Double> {
        if (mProviders.containsKey(resFeature))
            throw IllegalArgumentException("Cannot add operator for feature $resFeature twice.")
        Log.i(TAG, "adding operator for feature " + resFeature)

        // When someone subscribes, give them the last output
        // TODO: check that this does not leak memory somehow
        // TODO: check that a ref count dropping to 0 does not cause unsubscriptions even though we
        // later want to resubscribe.
        val sharedObs = obs.replay(1).refCount()
        mProviders[resFeature] = sharedObs
        return sharedObs
    }


    private abstract inner class TimedOperator(val useNetTime: Boolean = true) :
            Function<Double, Double> {
        protected var initTime: Double = -1.0
        protected var lastTimeMs: Double = -1.0

        internal fun timeIncrement(): Double {
            val curTime = curTime()

            val timeIncrement = curTime - lastTimeMs
            lastTimeMs = curTime
            return timeIncrement
        }

        internal fun totalTime(): Double {
            val curTime = curTime()
            return curTime - initTime
        }

        internal fun curTime(): Double {
            val curTime = if (useNetTime)
                totalTime
            else
                SystemClock.elapsedRealtime().toDouble() / 1000.0

            if (lastTimeMs < 0) {
                initTime = curTime
                lastTimeMs = curTime
            }
            return curTime
        }
    }

    private inner class TimeAvgOperator(val normalized: Boolean) : TimedOperator(true) {
        constructor(): this(false)

        private var lastAvg = -1.0

        override fun apply(curVal: Double): Double {
            val timeDiff = timeIncrement()
            if (timeDiff < 0)
                return if (lastAvg < 0) 0.0 else lastAvg

            // TODO: refine this condition
            if (normalized && curVal < 5) {
                // below activation threshold
                lastTimeMs = curTime()
                return lastAvg
            }

            val totalTime = totalTime()
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

    private inner class OffsetOperator() : Function<Double, Double> {

        var initialVal: Double? = null

        override fun apply(nextVal: Double): Double {
            if (initialVal == null)
                initialVal = nextVal
            return nextVal - initialVal!!
        }

    }

    private inner class MaxOperator : Function<Double, Double> {
        private var maxVal = 0.0

        override fun apply(value: Double): Double {
            if (!mIsResumed)
                maxVal
            val v = value
            assert(v >= 0)
            maxVal = Math.max(v, maxVal)
            return maxVal
        }
    }

    private inner class EventPerMinOperator(val multiplicator: Double) : BiFunction<Double, Double, Double> {

        private var lastCumulativeRevolutions = 0.0
        private var lastTime = -1.0

        override fun apply(revolutions: Double, time: Double): Double {
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

        private const val TAG = "DataStreamer"
        private const val TIME_DELAY_MS = 1000

        // TODO: make configurable
        private const val WHEEL_CIRCUMFERENCE = 2.11
    }

}
