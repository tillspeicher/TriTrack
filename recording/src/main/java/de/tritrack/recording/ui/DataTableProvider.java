package de.tritrack.recording.ui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.tritrack.recording.R;
import de.tritrack.recording.recording.ActivityFeature;
import de.tritrack.recording.recording.UICommunication;

/**
 * Created by till on 04.06.17.
 */

public class DataTableProvider {

    private static final String TAG = "DataTableProvider";

    public static Map<ActivityFeature, UICommunication.UIDataListener> getTableView(
            ActivityFeature[][] layout, LayoutInflater inflater, LinearLayout root, List<TextView> textViews) {
        Map<ActivityFeature, UICommunication.UIDataListener> listeners = new HashMap<>();
        TableLayout tableView = inflater.inflate(R.layout.data_table, null)
                .findViewById(R.id.table_data);
        // TODO: always add it at index 0?
        root.addView(tableView, 0);
        for (ActivityFeature[] row : layout) {
            TableRow rowView = (TableRow) inflater.inflate(R.layout.data_row, null);// TableRow(context);
            tableView.addView(rowView);
            for (final ActivityFeature feature : row) {
                View featureView = inflater.inflate(R.layout.data_item, null)
                        .findViewById(R.id.item_data);
                rowView.addView(featureView);

                TextView descriptionView = (TextView) featureView.findViewById(R.id.text_description);
                descriptionView.setText(feature.getDescription());
                textViews.add(descriptionView);
                TextView unitView = (TextView) featureView.findViewById(R.id.text_unit);
                unitView.setText(feature.getUnit());
                textViews.add(unitView);
                final TextView dataView = (TextView) featureView.findViewById(R.id.text_data);
                textViews.add(dataView);
                UICommunication.UIDataListener viewListener = new UICommunication.UIDataListener() {
                    @Override
                    public void onFeatureChanged(double newVal) {
                        dataView.setText(feature.format(newVal));
                    }
                };
//                Log.i(TAG, "adding listener for feature " + feature);
                listeners.put(feature, viewListener);
            }
        }
        return listeners;
    }

}
