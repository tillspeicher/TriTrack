package de.tritrack.recording.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import de.tritrack.recording.R
import de.tritrack.recording.recording.ActFeature
import de.tritrack.recording.recording.ActivityData
import de.tritrack.recording.recording.OpType
import de.tritrack.recording.recording.Recorder
import rx.subjects.BehaviorSubject

class DataScreenFragment : Fragment() {

    private var mScreenLayout: Array<Array<ActivityData>>? = null
    private var mObservations: List<List<BehaviorSubject<Double>>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screenId = arguments!!.getInt(ARG_SCREEN_ID)
        mScreenLayout = getDataDescriptors(screenId)

        val segmentManager = Recorder.getInstance(activity!!.applicationContext).segmentManager
        val segmentId = arguments!!.getInt(ARG_SEGMENT_ID)
        val segmentFeatures = mScreenLayout!!.flatten().toSet()
        val segmentObservations = segmentManager.getSegmentObservations(segmentId, segmentFeatures)
        mObservations = mScreenLayout!!.map { row -> row.map {
            actData -> segmentObservations[actData]!! }}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contentLayout: LinearLayout = inflater.inflate(R.layout.data_screen_content, container,
                false) as LinearLayout

        DataTableProvider.getTableView(inflater, contentLayout, mScreenLayout!!, mObservations!!)

        return contentLayout
    }

    companion object {

        private val ARG_SCREEN_ID = "screen_id"
        private val ARG_SEGMENT_ID = "segment_id"

        fun newInstance(id: Int, segmentId: Int): DataScreenFragment {
            val args = Bundle()
            args.putInt(ARG_SCREEN_ID, id)
            args.putInt(ARG_SEGMENT_ID, segmentId)
            val fragment = DataScreenFragment()
            fragment.arguments = args
            return fragment
        }

        fun getAllDataDescriptors(): Set<ActivityData> {
            // TODO: hacky, come up with something better once screen content can be adjusted
            return (getDataDescriptors(0).flatten() +
                    getDataDescriptors(1).flatten() +
                    getDataDescriptors(2).flatten()).toSet()
        }

        private fun getDataDescriptors(id: Int): Array<Array<ActivityData>> {
            // TODO: make configurable
            return when(id) {
                0 -> arrayOf(arrayOf(ActivityData(ActFeature.DURATION_S, OpType.ID),
                        ActivityData(ActFeature.DISTANCE_KM, OpType.ID)),
                        arrayOf(ActivityData(ActFeature.SPEED_KMH, OpType.ID),
                                ActivityData(ActFeature.SPEED_KMH, OpType.AVG)),
                        arrayOf(ActivityData(ActFeature.ELEVATION_GAIN, OpType.ID),
                                ActivityData(ActFeature.SPEED_KMH, OpType.MAX)),
                        arrayOf(ActivityData(ActFeature.HEART_RATE, OpType.ID),
                                ActivityData(ActFeature.HEART_RATE, OpType.AVG)),
                        arrayOf(ActivityData(ActFeature.POWER_COMBINED, OpType.ID),
                                ActivityData(ActFeature.POWER_COMBINED, OpType.NORM_AVG)),
                        arrayOf(ActivityData(ActFeature.CADENCE, OpType.ID),
                                ActivityData(ActFeature.CADENCE, OpType.NORM_AVG)))
                1 -> arrayOf(arrayOf(ActivityData(ActFeature.DISTANCE_KM_REV, OpType.ID),
                        ActivityData(ActFeature.SPEED_KMH_REV, OpType.ID)),
                        arrayOf(ActivityData(ActFeature.HEART_RATE, OpType.MAX),
                                ActivityData(ActFeature.POWER_COMBINED, OpType.MAX)),
                        arrayOf(ActivityData(ActFeature.POWER_LEFT, OpType.ID),
                                ActivityData(ActFeature.POWER_RIGHT, OpType.ID)),
                        arrayOf(ActivityData(ActFeature.POWER_COMBINED, OpType.AVG),
                                ActivityData(ActFeature.CADENCE, OpType.AVG)))
                // TODO: this is the interval part, make it configurable, etc.
                else -> arrayOf(arrayOf(ActivityData(ActFeature.DURATION_S, OpType.OFFSET),
                        ActivityData(ActFeature.DISTANCE_KM, OpType.OFFSET)),
                        arrayOf(ActivityData(ActFeature.SPEED_KMH, OpType.ID),
                                ActivityData(ActFeature.SPEED_KMH, OpType.AVG)),
                        arrayOf(ActivityData(ActFeature.ELEVATION_GAIN, OpType.OFFSET),
                                ActivityData(ActFeature.SPEED_KMH, OpType.MAX)),
                        arrayOf(ActivityData(ActFeature.HEART_RATE, OpType.ID),
                                ActivityData(ActFeature.HEART_RATE, OpType.AVG)),
                        arrayOf(ActivityData(ActFeature.POWER_COMBINED, OpType.ID),
                                ActivityData(ActFeature.POWER_COMBINED, OpType.NORM_AVG)),
                        arrayOf(ActivityData(ActFeature.CADENCE, OpType.ID),
                                ActivityData(ActFeature.CADENCE, OpType.NORM_AVG)))
            }
        }

    }


}