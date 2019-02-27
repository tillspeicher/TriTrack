package de.tritrack.recording.recording

class ActivityRecording(features: Array<ActFeature>) {

    private class Segment(features: Array<ActFeature>) {

        private val activityData: Map<ActFeature, MutableList<Double>> = features.associate {
            it to ArrayList<Double>() }
        private val activitySummary = ActivitySummary(features)

        fun addDataPoint(data: Map<ActFeature, Double>) {
            data.forEach {
                activityData[it.key]!!.add(it.value)
            }
        }
    }

    private class ActivitySummary(features: Array<ActFeature>) {
        private val summaryData: Map<ActFeature, MutableMap<OpType, Double>> =
                features.associate { it to HashMap<OpType, Double>() }
    }

    private val features = features
    private val segments: MutableList<Segment> = ArrayList()
    private var curSegment: Segment? = null
    private val activitySummary = ActivitySummary(features)


    fun nextSegment() {
        curSegment = Segment(features)
        segments.add(curSegment!!)
        // maybe write previous segment
    }

    fun addDataPoint(data: Map<ActFeature, Double>) {
        curSegment!!.addDataPoint(data)
    }

    fun endRecording() {
        // TODO: write back
    }

}