package de.tritrack.recording.recording

import android.content.Context
import android.content.Context.MODE_PRIVATE

class ConfigManager private constructor(context: Context) {

    private val sharedPrefs = context.getSharedPreferences(RECORDING_PREF_FILE, MODE_PRIVATE)

    fun getStorageFeatures(): Array<ActivityData> {
        return arrayOf(ActivityData(ActFeature.DURATION_S, OpType.ID),
                ActivityData(ActFeature.DISTANCE_M, OpType.ID),
                ActivityData(ActFeature.LATITUDE, OpType.ID),
                ActivityData(ActFeature.LONGITUDE, OpType.ID))
    }

    companion object {

        private val RECORDING_PREF_FILE = "recording_preferences"

        private var instance: ConfigManager? = null

        fun getInstance(context: Context): ConfigManager {
            synchronized(this) {
                if (instance == null)
                    instance = ConfigManager(context)
                return instance!!
            }
        }
    }
}