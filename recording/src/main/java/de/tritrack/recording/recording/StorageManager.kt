package de.tritrack.recording.recording

import android.os.Environment
import android.os.Handler
import android.util.Log

import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

import au.com.bytecode.opencsv.CSVWriter

/**
 * Created by till on 01.07.17.
 */

internal class StorageManager {

    private val mFeaturePositions: MutableMap<ActFeature, Int>
    private val mCurData: MutableList<Double>
    private val mHandler: Handler
    private var mWriter: CSVWriter? = null

    init {
        mFeaturePositions = HashMap()
        mCurData = ArrayList()
        mHandler = Handler()
    }

    fun addFeature(feature: ActFeature) {
        // TODO: disallow adding features while recording is in progress
        assert(!mFeaturePositions.containsKey(feature))
        val featurePos = mCurData.size
        mFeaturePositions[feature] = featurePos
        mCurData.add(0.0)
    }

    fun setValue(feature: ActFeature, value: Double) {
        assert(mFeaturePositions.containsKey(feature))
        Log.i(TAG, "getting feature $feature")
        val featurePos = (mFeaturePositions[feature])!!
        mCurData.set(featurePos, value)
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
            mWriter = CSVWriter(fw, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER)
        } catch (ex: IOException) {
            Log.e(TAG, "Error creating out file: " + ex.message)
            return
        }

        val featureDescriptions = arrayOfNulls<String>(mCurData.size)
        for ((key, value) in mFeaturePositions) {
            featureDescriptions[value] = key.description
        }
        mWriter!!.writeNext(featureDescriptions)
        resumeStoring()
    }

    // TODO: check that these methods are called in the right way/states
    fun resumeStoring() {
        mHandler.post(object : Runnable {
            internal var values = arrayOfNulls<String>(mCurData.size)
            override fun run() {
                for (i in mCurData.indices) {
                    val data = mCurData[i]
                    values[i] = data?.toString() ?: ""
                }
                mWriter!!.writeNext(values)
                mHandler.postDelayed(this, STORAGE_INTERVAL_MS)
            }
        })
    }

    fun pauseStoring() {
        mHandler.removeCallbacksAndMessages(null)
    }

    fun stopStoring() {
        pauseStoring()
        try {
            mWriter!!.close()
        } catch (ex: IOException) {
            Log.e(TAG, "Error closing csv writer: " + ex.message)
        }

    }

    companion object {

        private val TAG = "StorageManager"
        private val STORAGE_INTERVAL_MS: Long = 4000
    }

}
