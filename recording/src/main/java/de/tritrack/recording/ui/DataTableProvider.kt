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
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

/**
 * Created by till on 04.06.17.
 */

object DataTableProvider {

    private val TAG = "DataTableProvider"

    fun getTableView(inflater: LayoutInflater, root: LinearLayout,
                     layout: Array<Array<Pair<ActFeature, OpType>>>,
                     dataSources: List<List<Observable<Double>>>,
                     curVals: List<List<Double?>>): List<Disposable> {
        val tableView = inflater.inflate(R.layout.data_table, null)
                .findViewById<TableLayout>(R.id.table_data)
        val subscriptions = ArrayList<Disposable>()
        // TODO: always add it at index 0?
        root.addView(tableView, 0)
        for ((i1, layoutRow) in layout.withIndex()) {
            val sourcesRow = dataSources[i1]
            val curValsRow = curVals[i1]

            val rowView = inflater.inflate(R.layout.data_row, null) as TableRow
            tableView.addView(rowView)
            for ((i2, dataDescriptor) in layoutRow.withIndex()) {
                val (actFeature, opType) = dataDescriptor
                val dataSource = sourcesRow[i2]
                val curVal = curValsRow[i2]

                val featureView = inflater.inflate(R.layout.data_item, null)
                        .findViewById<View>(R.id.item_data)
                rowView.addView(featureView)

                val descriptionView = featureView.findViewById<View>(R.id.text_description) as TextView
                descriptionView.text = opType.prefix + actFeature.description
                val unitView = featureView.findViewById<View>(R.id.text_unit) as TextView
                unitView.text = actFeature.unit
                val dataView = featureView.findViewById<View>(R.id.text_data) as TextView
                dataView.text = actFeature.format(curVal)

                subscriptions.add(dataSource.forEach { dataView.text = actFeature.format(it) })
            }
        }
        return subscriptions
    }

}
