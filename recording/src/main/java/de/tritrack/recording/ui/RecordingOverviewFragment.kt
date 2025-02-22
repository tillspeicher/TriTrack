package de.tritrack.recording.ui

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.tritrack.recording.R

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [RecordingOverviewFragment.OnListFragmentInteractionListener] interface.
 */
class RecordingOverviewFragment : Fragment() {

    // TODO: Customize parameters
    private var recordingId = 0

    private var listener: OnListFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            recordingId = it.getInt(ARG_RECORDING_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_recording_overview_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter = RecordingOverviewRecyclerViewAdapter(context, listener)
            }
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(recordingId: Int)
    }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_RECORDING_ID = "recording-id"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(recordingId: Int) =
                RecordingOverviewFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_RECORDING_ID, recordingId)
                    }
                }
    }
}
