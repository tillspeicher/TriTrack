package de.tritrack.recording.recording

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.util.Log

import java.io.File
import java.io.IOException

import rx.subjects.BehaviorSubject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by till on 01.07.17.
 */

class StorageManager(context: Context, private val segmentManager: SegmentManager) {

    private val handler = Handler()
    private val configManager = ConfigManager.getInstance(context)
    private var storageFeatures = configManager.getStorageFeatures()
    //private val logFile = File(context.filesDir, "activity.log")
    //private var activityLog = ActivityLog(storageFeatures, logFile)
    // TODO: configure the set of features here
    //private var recordingFeatures = arrayOf(ActFeature.TIME_S, ActFeature.DISTANCE_KM)
    private var recordingFeatures = storageFeatures.map { it.feature }.toTypedArray()
    private var activityRecording: ActivityRecording? = null

    private var featureObs = emptyMap<ActFeature, BehaviorSubject<Double>>()
    private var overallObs = emptyMap<ActivityData, BehaviorSubject<Double>>()
    private var curSegmentObs = emptyMap<ActivityData, BehaviorSubject<Double>>()

    init {
        segmentManager.monitorFeatures(storageFeatures.asIterable(), {
            curSegmentObs = segmentManager.getSegmentObservations(it, storageFeatures.asIterable())
            writeCurState(ActivityLog.RecordType.SEGMENT_BOUNDARY)
        })
        overallObs = segmentManager.getSegmentObservations(SegmentManager.GLOBAL_SEGMENT_ID,
                storageFeatures.asIterable())
        featureObs = segmentManager.getSegmentObservations(SegmentManager.GLOBAL_SEGMENT_ID,
                recordingFeatures.map { ActivityData(it, OpType.ID) }).mapKeys { it.key.feature }
    }

    internal fun startStoring() {
        activityRecording = ActivityRecording(System.currentTimeMillis(), recordingFeatures)
        resumeStoring()
    }

    // TODO: check that these methods are called in the right way/states
    private fun resumeStoring() {
        handler.post(object: Runnable {
            override fun run() {
                writeCurState(ActivityLog.RecordType.REGULAR)
                handler.postDelayed(this, STORAGE_INTERVAL_MS)
            }
        })
    }

    internal fun pauseStoring() {
        // TODO: update values again
        handler.removeCallbacksAndMessages(null)
        writeCurState(ActivityLog.RecordType.PAUSE_START)
    }

    internal fun endStoring() {
        pauseStoring()
        writeCurState(ActivityLog.RecordType.END)

        val outDir = getStorageDir()
        outDir?.let {
            val time = activityRecording!!.startTimestampMs
            val date_formatter = SimpleDateFormat("dd/MM/yyyy-HH:mm:ss")
            date_formatter.timeZone = TimeZone.getDefault()
            val human_readable_time = date_formatter.format(time)
            Log.i("tag", human_readable_time)
            val outFile = File(outDir, "Track_$time.fit")
            ActivityRecording.toFitFile(activityRecording!!, outFile)
        }
    }

    private fun writeCurState(recType: ActivityLog.RecordType) {
        // TODO: store global state as well
        // and try to have only one most up to date version of it
        //val curVals = storageFeatures.associate { it to curSegmentObs[it]!!.value }
        //activityLog.addDataPoint(curVals, recType)
        val timeMs = System.currentTimeMillis()
        val curVals = recordingFeatures.associate { it to featureObs[it]!!.value }
        activityRecording!!.addData(timeMs, curVals)
    }

    companion object {

        private const val TAG = "StorageManager"
        private val STORAGE_INTERVAL_MS: Long = 5000

        fun getStorageDir(): File? {
            // TODO: use internal directory and only export on request
            val outDir = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "TriTracks")
            if (!outDir.exists() && !outDir.mkdirs()) {
                Log.e(TAG, "Cannot create out dir " + outDir.path)
                return null
            }
            return outDir
        }
    }

}
