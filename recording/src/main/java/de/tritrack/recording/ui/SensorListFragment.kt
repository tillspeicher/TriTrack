package de.tritrack.recording.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import de.tritrack.recording.R
import de.tritrack.recording.recording.BlePool
import de.tritrack.recording.recording.Recorder

/**
 * A fragment showing a list of BLE sensor devices.
 */
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
class SensorListFragment : Fragment() {

    private var mSensorListener: BlePool.SensorDeviceScanListener? = null
    private var mRecorder: Recorder? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.sensor_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            val context = view.getContext()
            view.layoutManager = LinearLayoutManager(context)
            val adapter = SensorListAdapter()
            view.adapter = adapter
            mSensorListener = adapter
        }
        return view
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mRecorder = Recorder.getInstance(context)
    }

    override fun onStart() {
        super.onStart()

        // TODO: fix this
        if (ActivityCompat.checkSelfPermission(context!!, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
            // TODO: show rationale?
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    2)
        } else {
            mRecorder!!.startBleScan(mSensorListener!!)
        }
    }

    // TODO: this is hacky, fix it
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            2 -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // TODO: revise this behavior
                mRecorder!!.startBleScan(mSensorListener!!)
            } else {
                // TODO
                throw IllegalStateException()
            }
        }
    }


    override fun onStop() {
        super.onStop()
        // TODO: scan in Background
        mRecorder!!.stopBleScan()
    }

    override fun onDetach() {
        super.onDetach()
    }

}
