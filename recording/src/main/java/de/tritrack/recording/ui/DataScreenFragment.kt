package de.tritrack.recording.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import de.tritrack.recording.R
import de.tritrack.recording.recording.ActFeature
import de.tritrack.recording.recording.OpType
import de.tritrack.recording.recording.Recorder
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.*

class DataScreenFragment : Fragment() {

    companion object {

        private val ARG_SCREEN_ID = "screen_id"

        fun newInstance(id: Int): DataScreenFragment {
            val args = Bundle()
            args.putInt(ARG_SCREEN_ID, id)
            val fragment = DataScreenFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var mScreenLayout: Array<Array<Pair<ActFeature, OpType>>>? = null
    private var mObservables: List<List<Observable<Double>>>? = null
    private var mSubscriptions: List<Disposable>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screenId = arguments!!.getInt(ARG_SCREEN_ID)
        mScreenLayout = getDataDescriptors(screenId)

        val recorder = Recorder.getInstance(activity!!.applicationContext)
        mObservables = mScreenLayout!!.map { row -> row.map {
            (feature, op) -> recorder.getDataObservable(feature, op) }}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contentLayout: LinearLayout = inflater.inflate(R.layout.data_screen_content, container,
                false) as LinearLayout

        mSubscriptions = DataTableProvider.getTableView(inflater, contentLayout,
                mScreenLayout!!, mObservables!!)

        return contentLayout
    }

    override fun onDestroyView() {
        mSubscriptions!!.forEach { it.dispose() }
        super.onDestroyView()
    }

    private fun getDataDescriptors(id: Int): Array<Array<Pair<ActFeature, OpType>>> {
        // TODO: make configurable
        val featureLayout: Array<Array<Pair<ActFeature, OpType>>> = when(id) {
            0 -> arrayOf(arrayOf(Pair(ActFeature.TIME_S, OpType.ID), Pair(ActFeature.DISTANCE_KM, OpType.ID)),
                    arrayOf(Pair(ActFeature.SPEED_KMH, OpType.ID), Pair(ActFeature.SPEED_KMH, OpType.AVG)),
                    arrayOf(Pair(ActFeature.ELEVATION_GAIN, OpType.ID), Pair(ActFeature.SPEED_KMH, OpType.MAX)),
                    arrayOf(Pair(ActFeature.HEART_RATE, OpType.ID), Pair(ActFeature.HEART_RATE, OpType.AVG)),
                    arrayOf(Pair(ActFeature.POWER_COMBINED, OpType.ID), Pair(ActFeature.POWER_COMBINED, OpType.NORM_AVG)),
                    arrayOf(Pair(ActFeature.CADENCE, OpType.ID), Pair(ActFeature.CADENCE, OpType.NORM_AVG)))
            1 -> arrayOf(arrayOf(Pair(ActFeature.DISTANCE_KM_REV, OpType.ID), Pair(ActFeature.SPEED_KMH_REV, OpType.ID)),
                    arrayOf(Pair(ActFeature.HEART_RATE, OpType.MAX), Pair(ActFeature.POWER_COMBINED, OpType.MAX)),
                    arrayOf(Pair(ActFeature.POWER_LEFT, OpType.ID), Pair(ActFeature.POWER_RIGHT, OpType.ID)),
                    arrayOf(Pair(ActFeature.POWER_COMBINED, OpType.AVG), Pair(ActFeature.CADENCE, OpType.AVG)))
        // TODO: this is the interval part, make it configurable, etc.
            else -> arrayOf(arrayOf(Pair(ActFeature.TIME_S, OpType.OFFSET), Pair(ActFeature.DISTANCE_KM, OpType.OFFSET)),
                    arrayOf(Pair(ActFeature.SPEED_KMH, OpType.ID), Pair(ActFeature.SPEED_KMH, OpType.AVG)),
                    arrayOf(Pair(ActFeature.ELEVATION_GAIN, OpType.OFFSET), Pair(ActFeature.SPEED_KMH, OpType.MAX)),
                    arrayOf(Pair(ActFeature.HEART_RATE, OpType.ID), Pair(ActFeature.HEART_RATE, OpType.AVG)),
                    arrayOf(Pair(ActFeature.POWER_COMBINED, OpType.ID), Pair(ActFeature.POWER_COMBINED, OpType.NORM_AVG)),
                    arrayOf(Pair(ActFeature.CADENCE, OpType.ID), Pair(ActFeature.CADENCE, OpType.NORM_AVG)))
        }
        return featureLayout
    }

}