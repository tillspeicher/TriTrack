package de.tritrack.recording.recording

import android.util.Log
import io.reactivex.disposables.Disposable
import rx.subjects.BehaviorSubject

class SegmentManager internal constructor(private val dataStreamer: DataStreamer) {

    // TODO: refactor this to become the general control handle
    // i.e. add callbacks for any sort of state changes

    private var isStarted = false

    private val aggregatedActivityFeatures: MutableSet<ActivityData> = HashSet()
    private val segmentSubscribers: MutableList<(Int) -> Unit> = ArrayList()
    private val overallObservables: MutableMap<ActivityData, BehaviorSubject<Double>> = HashMap()
    private val overallSubscriptions: MutableList<Disposable> = ArrayList()
    // We need to hold on to references here to prevent the Observables getting garbage collected
    // in the UI
    private val segmentObservables: MutableList<Map<ActivityData, BehaviorSubject<Double>>> =
            ArrayList()
    private val curSegmentSubscriptions: MutableList<Disposable> = ArrayList()

    fun monitorFeatures(features: Iterable<ActivityData>, listener: (Int) -> Unit) {
        assert(!isStarted)

        aggregatedActivityFeatures.addAll(features)
        segmentSubscribers.add(listener)
        features.forEach {
            if (!overallObservables.contains(it)) {
                val interceptSubject = BehaviorSubject.create<Double>()
                val sub = dataStreamer.getOperator(it).forEach { interceptSubject.onNext(it) }
                overallObservables[it] = interceptSubject
                overallSubscriptions.add(sub)
            }
        }
    }

    fun getSegmentObservations(segmentId: Int, features: Iterable<ActivityData>):
            Map<ActivityData, BehaviorSubject<Double>> {
        if (segmentId == GLOBAL_SEGMENT_ID)
            return adjustToSubscriber(overallObservables, features)
        return adjustToSubscriber(segmentObservables[segmentId], features)
    }

    fun nextSegment() {
        isStarted = true

        // unsubscribe the previous segment observables
        curSegmentSubscriptions.forEach { it.dispose() }
        curSegmentSubscriptions.clear()

        val newSegmentObservables = aggregatedActivityFeatures.map {
            val sourceOp = dataStreamer.getOperator(it)
            val interceptSubject = BehaviorSubject.create<Double>()
            val subscription = sourceOp.forEach { interceptSubject.onNext(it) }
            curSegmentSubscriptions.add(subscription)
            // TODO: do we need replay and ref count here?
            it to interceptSubject }.toMap()
        val segmentId = segmentObservables.size
        segmentObservables.add(newSegmentObservables)
        segmentSubscribers.forEach { it(segmentId) }
    }

    internal fun end() {
        overallSubscriptions.forEach { it.dispose() }
        overallSubscriptions.clear()
        isStarted = false
    }

    companion object {

        const val GLOBAL_SEGMENT_ID = -1

        private fun adjustToSubscriber(source: Map<ActivityData, BehaviorSubject<Double>>,
                                       target: Iterable<ActivityData>):
                Map<ActivityData, BehaviorSubject<Double>> {
            val tag = "SegmentManager"
            Log.i(tag, "sources: ${source.toString()}")
            Log.i(tag, "targets: ${target.toString()}")
            return target.associate { Log.i(tag, "Adding ${it.feature}"); it to source[it]!! }
        }

    }
}