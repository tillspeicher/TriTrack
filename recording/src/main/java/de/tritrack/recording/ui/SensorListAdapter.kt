package de.tritrack.recording.ui

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView

import de.tritrack.recording.R
import de.tritrack.recording.recording.BlePool

import java.util.ArrayList

/**
 * [RecyclerView.Adapter] that can display a [de.tritrack.recording.recording.BlePool.SensorDevice]
 */
class SensorListAdapter : RecyclerView.Adapter<SensorListAdapter.ViewHolder>(), BlePool.SensorDeviceScanListener {

    private val mDevices: MutableList<BlePool.SensorDevice>

    init {
        mDevices = ArrayList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.sensor_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dev = mDevices[position]
        holder.mItem = dev
        holder.mNameView.text = dev.name
        // TODO: add information about whether the device is currently in range or not
//        Log.i("Adapter", "Setting dev %s to connected %b".format(dev.name, dev.isConnected))
//        holder.mNameView.isEnabled = dev.isConnected
        holder.mEnabledSwitch.isChecked = dev.isEnabled

        holder.mEnabledSwitch.setOnCheckedChangeListener { buttonView, isChecked -> dev.isEnabled = isChecked }
    }

    override fun getItemCount(): Int {
        return mDevices.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mNameView: TextView
        val mEnabledSwitch: Switch
        var mItem: BlePool.SensorDevice? = null

        init {
            mNameView = mView.findViewById(R.id.text_dev_name)
            mEnabledSwitch = mView.findViewById(R.id.switch_use_dev)
        }

        override fun toString(): String {
            return super.toString() + " '" + mNameView.text + "'"
        }
    }

    override fun resultAvailable(devices: List<BlePool.SensorDevice>) {
        mDevices.clear()
        mDevices.addAll(devices)
        notifyDataSetChanged()
    }
}
