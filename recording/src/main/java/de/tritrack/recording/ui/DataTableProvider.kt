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
                     dataSources: List<List<Observable<Double>>>): List<Disposable> {
        val tableView = inflater.inflate(R.layout.data_table, null)
                .findViewById<TableLayout>(R.id.table_data)
        val subscriptions = ArrayList<Disposable>()
        // TODO: always add it at index 0?
        root.addView(tableView, 0)
        for ((layoutRow, sourcesRow) in layout.zip(dataSources)) {
            val rowView = inflater.inflate(R.layout.data_row, null) as TableRow
            tableView.addView(rowView)
            for ((dataDescriptor, dataSource) in layoutRow.zip(sourcesRow)) {
                val (actFeature, opType) = dataDescriptor
                val featureView = inflater.inflate(R.layout.data_item, null)
                        .findViewById<View>(R.id.item_data)
                rowView.addView(featureView)

                val descriptionView = featureView.findViewById<View>(R.id.text_description) as TextView
                descriptionView.text = opType.prefix + actFeature.description
                val unitView = featureView.findViewById<View>(R.id.text_unit) as TextView
                unitView.text = actFeature.unit
                val dataView = featureView.findViewById<View>(R.id.text_data) as TextView

                subscriptions.add(dataSource.forEach { dataView.text = actFeature.format(it) })
            }
        }
        return subscriptions
    }

}
