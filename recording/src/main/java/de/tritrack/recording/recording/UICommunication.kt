package de.tritrack.recording.recording

import android.os.Handler
import android.os.Looper

/**
 * Created by till on 03.06.17.
 */

object UICommunication {

    private val mUiHandler = Handler(Looper.getMainLooper())

    fun runOnUiThread(r: Runnable) {
        mUiHandler.post(r)
    }

}
