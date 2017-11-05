package de.tritrack.recording.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.tritrack.recording.R;
import de.tritrack.recording.recording.BlePool;
import de.tritrack.recording.recording.Recorder;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnSensorListInteractionListener}
 * interface.
 */
public class SensorListFragment extends Fragment {

    private OnSensorListInteractionListener mListener;
    private BlePool.SensorDeviceScanListener mSensorListener;
    private Recorder mRecorder;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SensorListFragment() {
    }

//    // TODO: Customize parameter initialization
//    @SuppressWarnings("unused")
//    public static SensorListFragment newInstance() {
//        SensorListFragment fragment = new SensorListFragment();
////        Bundle args = new Bundle();
////        fragment.setArguments(args);
//        return fragment;
//    }

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sensor_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            SensorListAdapter adapter = new SensorListAdapter(mListener);
            recyclerView.setAdapter(adapter);
            mSensorListener = adapter;
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSensorListInteractionListener) {
            mListener = (OnSensorListInteractionListener) context;
            mRecorder = Recorder.getInstance(context);
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnSensorListInteractionListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mRecorder.startBleScan(mSensorListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        // TODO: scan in Background
        mRecorder.stopBleScan();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     */
    public interface OnSensorListInteractionListener {
        void onSensorEnableChange(BlePool.SensorDevice item, boolean isEnabled);
    }
}
