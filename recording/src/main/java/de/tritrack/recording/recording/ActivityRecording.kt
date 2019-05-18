package de.tritrack.recording.recording

import android.util.Log
import com.garmin.fit.*
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.HashMap

class ActivityRecording(val startTimestampMs: Long, private val features: Array<ActFeature>) {

    private val timestampsMs: MutableList<Long> = ArrayList()
    private val activityData: Map<ActFeature, MutableList<Double?>> = features.associate {
        it to ArrayList<Double?>() }
    private val segments: MutableList<Segment> = ArrayList()
    private var activitySummary: Segment? = null

    private var curSegmentStartIndex = 0

    fun addData(timestamp: Long, data: Map<ActFeature, Double>) {
        // Implicitly stops an ongoing pause if there is one
        assert(activitySummary == null)
        // TODO: assert that the features match
        timestampsMs.add(timestamp)
        data.forEach { activityData[it.key]!!.add(it.value) }
    }

    fun startPause() {
        timestampsMs.add(PAUSE_TS)
        activityData.forEach {
            it.value.add(PAUSE_VAL)
        }
    }

    fun endSegment(startTimestamp: Long, summary: Map<ActivityData, Double>) {
        assert(activitySummary == null)
        // TODO: make sure there is at least one datapoint in the finished segment
        val segment = Segment(startTimestamp, curSegmentStartIndex, timestampsMs.size - 1,
                summary)
        segments.add(segment)
        curSegmentStartIndex = timestampsMs.size
    }

    fun endActivity(startTimestamp: Long, summary: Map<ActivityData, Double>) {
        assert(activitySummary == null)
        activitySummary = Segment(startTimestamp, 0, timestampsMs.size - 1,
                summary)
    }

    private fun iterDataRows(iterFeatures: Array<ActFeature>): Iterator<Pair<Long, List<Double?>>> {
        return object : Iterator<Pair<Long, List<Double?>>> {
            private var iterPos = 0

            override fun hasNext(): Boolean {
                return iterPos < timestampsMs.size
            }

            override fun next(): Pair<Long, List<Double?>> {
                Log.i(TAG, "Iter features: $iterFeatures")
                Log.i(TAG, "activityData: $activityData")
                val res = Pair(timestampsMs[iterPos],
                        iterFeatures.map { activityData[it]!![iterPos] })
                ++iterPos
                return res
            }
        }
    }

    private data class Segment(val startTimestamp: Long, val firstRecord: Int,
                               val lastRecord: Int,
                               val summary: Map<ActivityData, Double>)

    companion object {
        private const val TAG = "ActivityRecording"

        private const val PAUSE_TS = -1L
        private const val PAUSE_VAL = -1.0

        fun toFitFile(rec: ActivityRecording, outFile: File) {
            val encoder: FileEncoder
            try {
                encoder = FileEncoder(outFile, Fit.ProtocolVersion.V2_0)
            } catch (e: FitRuntimeException) {
                Log.e(TAG, "Error initializing fit file encoder: " + e.message)
                return
            }

            encoder.write(FileIdMesg().apply {
                manufacturer = Manufacturer.DYNASTREAM // TODO: change manufacturer
                type = com.garmin.fit.File.ACTIVITY
                product = 0 // TODO: change product
                serialNumber = 0 // TODO: change serial
            })

            //// TODO: check this
            //val appId = byteArrayOf(
            //    0x1, 0x1, 0x2, 0x3,
            //    0x5, 0x8, 0xD, 0x15,
            //    0x22, 0x37, 0x59, 0x90 as Byte,
            //    0xE9 as Byte, 0x79, 0x62, 0xDB as Byte
            //)
            //encoder.write(DeveloperDataIdMesg().apply {
            //    appId.forEachIndexed { index, value -> applicationId[index] = value }
            //    developerDataIndex = 0
            //})

//            val record1 = RecordMesg()
//            record1.timestamp = DateTime(System.currentTimeMillis() / 1000)
//            record1.setFieldValue(RecordMesg.PositionLatFieldNum, 0.0)
//            record1.setFieldValue(RecordMesg.PositionLongFieldNum, 0.0)
//            encoder.write(record1)
//
//            val record2 = RecordMesg()
//            record2.timestamp = DateTime(System.currentTimeMillis() / 1000 + 100)
//            record2.setFieldValue(RecordMesg.PositionLatFieldNum, 1)
//            record2.setFieldValue(RecordMesg.PositionLongFieldNum, 1)
//            encoder.write(record2)

            val record = RecordMesg()
            for ((timestamp, dataRow) in rec.iterDataRows(rec.features)) {
                record.timestamp = DateTime(timestamp)
                for ((actFeat, dataVal) in rec.features.zip(dataRow))
                    writeField(record, ActivityData(actFeat, OpType.ID), dataVal)
                encoder.write(record)
            }

            try {
                encoder.close()
            } catch (e: FitRuntimeException) {
                Log.e(TAG, "Error closing fit file encoder: " + e.message)
                return
            }
        }

        private fun writeField(record: RecordMesg, actFeature: ActivityData, value: Double?) {
            val fieldPos = actFeatureMsgPositions[actFeature]
            fieldPos?.let { record.setFieldValue(fieldPos, value) }
        }

        fun fromFitFile(fitFile: File): ActivityRecording? {
            val decoder = Decode()
            val msgBroadcaster = MesgBroadcaster(decoder)

            var inStream: FileInputStream? = null
            try {
                inStream = FileInputStream(fitFile)
                if (!decoder.checkFileIntegrity(inStream))
                    return null

//                val recording = ActivityRecording()

                val decodingListener = RecordMesgListener { msg ->
                    msg.timestamp
                }
                msgBroadcaster.addListener(decodingListener)


            } catch (e: java.io.IOException) {
                Log.e(TAG, "Error opening FIT file: ${e.message}")
            } catch (e: FitRuntimeException) {
                Log.e(TAG, "Error reading FIT file: ${e.message}")
            } finally {
                inStream?.close()
            }

            return null
        }

        private val actFeatureMsgPositions = HashMap<ActivityData, Int>().apply {
            put(ActivityData(ActFeature.DURATION_S, OpType.ID), RecordMesg.TimestampFieldNum)
            put(ActivityData(ActFeature.LATITUDE, OpType.ID), RecordMesg.PositionLatFieldNum)
            put(ActivityData(ActFeature.LONGITUDE, OpType.ID), RecordMesg.PositionLongFieldNum)
            put(ActivityData(ActFeature.ALTITUDE, OpType.ID), RecordMesg.AltitudeFieldNum)
            put(ActivityData(ActFeature.HEART_RATE, OpType.ID), RecordMesg.HeartRateFieldNum)
            put(ActivityData(ActFeature.CADENCE, OpType.ID), RecordMesg.CadenceFieldNum)
            put(ActivityData(ActFeature.DISTANCE_M, OpType.ID), RecordMesg.DistanceFieldNum)
            put(ActivityData(ActFeature.SPEED_MS, OpType.ID), RecordMesg.SpeedFieldNum)
            put(ActivityData(ActFeature.POWER_COMBINED, OpType.ID), RecordMesg.PowerFieldNum)
        }
    }

}