package de.tritrack.recording.recording

import android.graphics.Path
import android.os.Environment
import android.os.Handler
import android.util.Log

import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

import au.com.bytecode.opencsv.CSVWriter
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer

/**
 * Created by till on 01.07.17.
 */

internal class StorageManager(dataStreamer: DataStreamer,
                              vararg loggingFeatures: ActivityData) {

    // TODO: can we directly create a mutable map?
    // TODO: should we synchronize access to this map?
    private val curValues: MutableMap<ActivityData, Double> =
            loggingFeatures.associate { it to 0.0 }.toMutableMap()
    private val listenerHandles = mutableListOf<Disposable>()
    private val handler = Handler()
    //private val activityRecording = ActivityRecording(loggingFeatures)

    init {
        loggingFeatures.map { loggingFeature ->
            // TODO: do we need to share? Try to get rid of it here and in the DataScreenFragment
            // and move it to the DataStreamer instead
            val featureObs = dataStreamer.getOperator(loggingFeature)//.share()
            listenerHandles.add(featureObs.forEach { featureVal ->
                curValues[loggingFeature] = featureVal })
        }
    }

    fun startStoring() {
        // TODO: use internal directory and only export on request
        val outDir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "TriTracks")
        if (!outDir.exists() && !outDir.mkdirs()) {
            Log.e(TAG, "Cannot create out dir " + outDir.path)
            return
        }
        val time = System.currentTimeMillis() / 1000
        val outFile = File(outDir, "Track_$time")

        try {
            outFile.createNewFile()
            val fw = FileWriter(outFile, true)
            // TODO
            //mWriter = CSVWriter(fw, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER)
        } catch (ex: IOException) {
            Log.e(TAG, "Error creating out file: " + ex.message)
            return
        }

        resumeStoring()
    }

    // TODO: check that these methods are called in the right way/states
    fun resumeStoring() {
        handler.post(object : Runnable {
            override fun run() {
                //activityRecording.addDataPoint(curValues)
            }
        })
    }

    fun pauseStoring() {
        handler.removeCallbacksAndMessages(null)
    }

    fun stopStoring() {
        pauseStoring()
        try {
            //mWriter!!.close()
        } catch (ex: IOException) {
            Log.e(TAG, "Error closing csv writer: " + ex.message)
        }

    }

    companion object {

        private val TAG = "StorageManager"
        private val STORAGE_INTERVAL_MS: Long = 5000
    }

}
