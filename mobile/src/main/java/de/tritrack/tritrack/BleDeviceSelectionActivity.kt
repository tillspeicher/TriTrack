package de.tritrack.tritrack

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import de.tritrack.recording.recording.BlePool

import de.tritrack.recording.recording.Recorder
import de.tritrack.recording.ui.SensorListFragment

class BleDeviceSelectionActivity : AppCompatActivity(), SensorListFragment.OnSensorListInteractionListener {

    private var mRecorder: Recorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_device_selection)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
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

    override fun onSensorEnableChange(item: BlePool.SensorDevice?, isEnabled: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
