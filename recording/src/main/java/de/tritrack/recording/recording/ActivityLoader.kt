package de.tritrack.recording.recording

class ActivityLoader {

    var recordings: List<ActivityRecording> = ArrayList()
        private set

    fun loadRecordings() {
        val storageDir = StorageManager.getStorageDir()
        storageDir?.let {
            val recordingFiles = storageDir.listFiles()
            recordings = recordingFiles.mapNotNull { ActivityRecording.fromFitFile(it) }
        }
    }

    fun numRecordings(): Int {
        return recordings.size ?: 0
    }

//    fun getRecordings(): List<ActivityRecording> {
//        val dummyRecording = ActivityRecording(System.currentTimeMillis(),
//                arrayOf(ActFeature.TIME_S, ActFeature.LATITUDE, ActFeature.LONGITUDE)).apply {
//            addData(0, emptyMap())
//        }
//        return listOf(dummyRecording)
//    }

    companion object {

        private var instance: ActivityLoader? = null

        public fun getInstance(): ActivityLoader {
            synchronized(this) {
                if (instance == null)
                    instance = ActivityLoader()
                return instance!!
            }
        }
    }

}