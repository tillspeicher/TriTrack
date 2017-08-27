package de.tritrack.tritrack

import android.content.Context
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView

import java.util.ArrayList

import de.tritrack.recording.recording.BlePool
import de.tritrack.recording.recording.Recorder
import de.tritrack.recording.recording.UICommunication
import de.tritrack.recording.ui.SensorListFragment
import de.tritrack.recording.ui.dummy.DummyContent

class BleDeviceSelectionActivity : AppCompatActivity(), SensorListFragment.OnListFragmentInteractionListener {

    private var mRecorder: Recorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_device_selection)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

//        val devicesView = findViewById(R.id.list_view_ble_devices) as ListView
//        val devices = ArrayList<BlePool.BleDevice>()
//        val devicesAdapter = BleDeviceArrayAdapter(this, devices)
//        devicesView.adapter = devicesAdapter
//        devicesView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id -> devices[position].connect() }
//
//        mRecorder = Recorder.getInstance(applicationContext)
//        val bleScanListener = object : UICommunication.BleScanListener {
//            override fun resultAvailable(newDevices: List<BlePool.BleDevice>) {
//                // TODO: can this be done better?
//                devices.clear()
//                devices.addAll(newDevices)
//                devicesAdapter.notifyDataSetChanged()
//            }
//        }
//        mRecorder!!.startBleScan(bleScanListener)
    }
//
//    private inner class BleDeviceArrayAdapter(context: Context, internal var mDevices: List<BlePool.BleDevice>) : ArrayAdapter<BlePool.BleDevice>(context, R.layout.layout_ble_device, mDevices) {
//
//        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//            val inflater = applicationContext
//                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//            val deviceView = inflater.inflate(R.layout.layout_ble_device, parent, false)
//            val nameView = deviceView.findViewById(R.id.text_ble_dev_name) as TextView
//            val device = mDevices[position]
//            nameView.text = device.name
//            return deviceView
//        }
//    }
//
//    override fun onStop() {
//        mRecorder!!.stopBleScan()
//        super.onStop()
//    }

    public override fun onListFragmentInteraction(item: DummyContent.DummyItem?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
