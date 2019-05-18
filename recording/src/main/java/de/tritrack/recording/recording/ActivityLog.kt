package de.tritrack.recording.recording

import android.media.MediaCodecList
import android.os.SystemClock
import au.com.bytecode.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import java.util.*

class ActivityLog(private val features: Array<ActivityData>, logFile: File) {

    private val activityData: Map<ActivityData, MutableList<Double>> = features.associate {
        it to ArrayList<Double>() }
    private val timestampsMs: MutableList<Long> = ArrayList()
    private val recordTypes: MutableList<RecordType> = ArrayList()

    val dataWriter = CSVWriter(FileWriter(logFile))

    init {
        // TODO: maybe only write the header when the recording has actually started
        val header = arrayOf(TIMESTAMP_KEY, RECORD_TYPE_KEY) + features.map { it.toString() }
        dataWriter.writeNext(header)
    }

    fun addDataPoint(data: Map<ActivityData, Double>, recType: RecordType=RecordType.REGULAR) {
        //assert(lastRecType != RecordType.PAUSE_START || recType != RecordType.PAUSE_START)

        val recordingTime = SystemClock.elapsedRealtime()
        timestampsMs.add(recordingTime)
        data.forEach {
            activityData[it.key]!!.add(it.value)
        }
        recordTypes.add(recType)

        logData(recordingTime, RecordType.REGULAR, data)

        if (recType == RecordType.END)
            dataWriter.close()
    }

    private fun logData(recordingTime: Long, recordType: RecordType,
                          data: Map<ActivityData, Double>) {
        val line = arrayOf(recordingTime.toString(), recordType.toString()) +
                features.map { if (data.containsKey(it)) data[it].toString() else "" }
        dataWriter.writeNext(line)
    }

    enum class RecordType {
        REGULAR,
        PAUSE_START,
        SEGMENT_BOUNDARY,
        END
    }

    companion object {
        val TIMESTAMP_KEY = "timestamp_ms"
        val RECORD_TYPE_KEY = "record_type"
    }

}