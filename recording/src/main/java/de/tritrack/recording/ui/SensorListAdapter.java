package de.tritrack.recording.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import de.tritrack.recording.R;
import de.tritrack.recording.recording.BlePool;
import de.tritrack.recording.ui.SensorListFragment.OnSensorListInteractionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link de.tritrack.recording.recording.BlePool.SensorDevice} and makes a call to the
 * specified {@link SensorListFragment.OnSensorListInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class SensorListAdapter extends RecyclerView.Adapter<SensorListAdapter.ViewHolder>
        implements BlePool.SensorDeviceScanListener {

    private List<BlePool.SensorDevice> mDevices;
    private final SensorListFragment.OnSensorListInteractionListener mListener;

    public SensorListAdapter(OnSensorListInteractionListener listener) {
        mDevices = new ArrayList<>();
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sensor_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final BlePool.SensorDevice dev = mDevices.get(position);
        holder.mItem = dev;
        holder.mNameView.setText(dev.getName());
        holder.mEnabledSwitch.setChecked(dev.isEnabled());

        holder.mEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                //mListener.onSensorEnableChange(holder.mItem, isChecked);
                dev.setEnabled(isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mNameView;
        public final Switch mEnabledSwitch;
        public BlePool.SensorDevice mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mNameView = (TextView) view.findViewById(R.id.text_dev_name);
            mEnabledSwitch = (Switch) view.findViewById(R.id.switch_use_dev);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mNameView.getText() + "'";
        }
    }

    @Override
    public void resultAvailable(List<BlePool.SensorDevice> devices) {
        mDevices.clear();
        mDevices.addAll(devices);
        notifyDataSetChanged();
    }
}
