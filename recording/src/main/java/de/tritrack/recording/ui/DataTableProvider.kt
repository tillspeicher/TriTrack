package de.tritrack.recording.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView

import java.util.HashMap

import de.tritrack.recording.R
import de.tritrack.recording.recording.ActFeature
import de.tritrack.recording.recording.OpType
import de.tritrack.recording.recording.UICommunication

/**
 * Created by till on 04.06.17.
 */

object DataTableProvider {

    private val TAG = "DataTableProvider"

    fun getTableView(layout: Array<Array<Pair<ActFeature, OpType>>>, inflater: LayoutInflater,
                     root: LinearLayout, textViews: MutableList<TextView>):
            Map<Pair<ActFeature, OpType>, UICommunication.UIDataListener> {
        val listeners = HashMap<Pair<ActFeature, OpType>, UICommunication.UIDataListener>()
        val tableView = inflater.inflate(R.layout.data_table, null)
                .findViewById<TableLayout>(R.id.table_data)
        // TODO: always add it at index 0?
        root.addView(tableView, 0)
        for (row in layout) {
            val rowView = inflater.inflate(R.layout.data_row, null) as TableRow// TableRow(context);
            tableView.addView(rowView)
            for ((actFeature, opType) in row) {
                val featureView = inflater.inflate(R.layout.data_item, null)
                        .findViewById<View>(R.id.item_data)
                rowView.addView(featureView)

                val descriptionView = featureView.findViewById<View>(R.id.text_description) as TextView
                descriptionView.text = opType.prefix + actFeature.description
                textViews.add(descriptionView)
                val unitView = featureView.findViewById<View>(R.id.text_unit) as TextView
                unitView.text = actFeature.unit
                textViews.add(unitView)
                val dataView = featureView.findViewById<View>(R.id.text_data) as TextView
                textViews.add(dataView)
                val viewListener = object : UICommunication.UIDataListener {
                    override fun onFeatureChanged(newVal: Double) {
                        Log.i(TAG, "listening $actFeature, $opType to $newVal")
                        dataView.text = actFeature.format(newVal)
                    }
                }
                //                Log.i(TAG, "adding listener for feature " + feature);
                listeners[Pair(actFeature, opType)] = viewListener
            }
        }
        return listeners
    }

}
