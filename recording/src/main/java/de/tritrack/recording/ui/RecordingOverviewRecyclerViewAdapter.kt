package de.tritrack.recording.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.tritrack.recording.R
import de.tritrack.recording.recording.ActivityLoader


import de.tritrack.recording.ui.RecordingOverviewFragment.OnListFragmentInteractionListener
import java.util.*

class RecordingOverviewRecyclerViewAdapter(context: Context,
                                   private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<RecordingOverviewRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener
    private val activityLoader = ActivityLoader.getInstance()

    init {
        mOnClickListener = View.OnClickListener { v ->
            val recordingId = v.id
            //val item = v.tag as ActivityRecording
            mListener?.onListFragmentInteraction(recordingId)
        }
        activityLoader.loadRecordings()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.recording_overview_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recording = activityLoader.recordings[position]
        val cal = Calendar.getInstance(Locale.ENGLISH)
        cal.timeInMillis = recording.startTimestampMs
        holder.mTimeView.text = DateFormat.format("EEE dd-MM-yyyy HH:mm", cal).toString()
        holder.mDistanceView.text = "1 km" //item.contentitem_number

        with(holder.mView) {
            tag = recording//item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = activityLoader.numRecordings()

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mTimeView: TextView = mView.findViewById(R.id.recording_time)
        val mDistanceView: TextView = mView.findViewById(R.id.recording_distance)

        override fun toString(): String {
            return super.toString() + "TT3"
        }
    }
}
