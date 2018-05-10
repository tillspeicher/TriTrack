package de.tritrack.tritrack

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.wearable.activity.WearableActivity
import android.support.wearable.view.BoxInsetLayout
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

import java.util.ArrayList

import de.tritrack.recording.recording.ActFeature
import de.tritrack.recording.recording.OpType
import de.tritrack.recording.recording.Recorder
import de.tritrack.recording.recording.UICommunication
import de.tritrack.recording.ui.DataTableProvider

class MainActivity : WearableActivity() {

    private var mRec: Recorder? = null

    private var mContainerView: BoxInsetLayout? = null
    private var mStartStopButton: ImageButton? = null
    private var mPauseResumeButton: ImageButton? = null
    private var mTextViews: MutableList<TextView>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setAmbientEnabled()

        mContainerView = findViewById<View>(R.id.container) as BoxInsetLayout

        mRec = Recorder.getInstance(applicationContext)
        val featureLayout = arrayOf(arrayOf(Pair(ActFeature.TIME_S, OpType.ID), Pair(ActFeature.DISTANCE_KM, OpType.ID)),
                arrayOf(Pair(ActFeature.PACE, OpType.ID), Pair(ActFeature.PACE, OpType.AVG)),
                //                new ActFeature[]{ActFeature.HEART_RATE, ActFeature.AVG_HEART_RATE}
                arrayOf(Pair(ActFeature.SPEED_MS, OpType.ID), Pair(ActFeature.SPEED_KMH, OpType.ID)))
        val contentLayout = findViewById<View>(R.id.content_main) as LinearLayout
        mTextViews = ArrayList()
        val inflater = LayoutInflater.from(this)
        val listeners = DataTableProvider.getTableView(featureLayout, inflater, contentLayout, mTextViews!!)
        mRec!!.addDataListeners(listeners)

        mStartStopButton = findViewById<View>(R.id.button_start_stop) as ImageButton
        mStartStopButton!!.setOnClickListener { startStopTracking() }
        mPauseResumeButton = findViewById<View>(R.id.button_pause_resume) as ImageButton
        mPauseResumeButton!!.isEnabled = false

    }

    override fun onDestroy() {
        super.onDestroy()
        if (mRec!!.isRecording)
            toggleTracking()
    }

    // TODO: copy past from wear

    fun startStopTracking() {
        //        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        //                != PackageManager.PERMISSION_GRANTED) {
        //            // TODO: show rationale?
        //            ActivityCompat.requestPermissions(this,
        //                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
        //                    REQUEST_CODE_LOCATION);
        //            return;
        //        }
        //        // TODO: revise this and try to remove it
        //        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        //                != PackageManager.PERMISSION_GRANTED) {
        //            // TODO: show rationale?
        //            ActivityCompat.requestPermissions(this,
        //                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
        //                    REQUEST_CODE_EXTERNAL_STORAGE);
        //            return;
        //        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: show rationale?
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_BOTH)
            return
        }
        toggleTracking()
    }

    private fun toggleTracking() {
        val isRecording = mRec!!.toggleRecording()
        if (isRecording) {
            // TODO: add Lap button functionality
            mStartStopButton!!.setImageResource(R.drawable.stop_sym)
            mPauseResumeButton!!.setOnClickListener {
                val isResumed = mRec!!.togglePause()
                if (isResumed)
                    mPauseResumeButton!!.setImageResource(R.drawable.pause_sym)
                else
                    mPauseResumeButton!!.setImageResource(R.drawable.start_sym)
            }
            mPauseResumeButton!!.isEnabled = true
        } else {
            mStartStopButton!!.setImageResource(R.drawable.start_sym)
            mPauseResumeButton!!.setImageResource(R.drawable.pause_sym)
            mPauseResumeButton!!.isEnabled = false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
        //            case REQUEST_CODE_LOCATION: {
        //                if (grantResults.length > 0
        //                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        //                    // TODO: revise this behavior
        //                    toggleTracking();
        //                } else {
        //                    // TODO
        //                    throw new IllegalStateException();
        //                }
        //            }
            REQUEST_CODE_BOTH -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // TODO: revise this behavior
                toggleTracking()
            } else {
                // TODO
                throw IllegalStateException()
            }
        }
    }

    override fun onEnterAmbient(ambientDetails: Bundle?) {
        super.onEnterAmbient(ambientDetails)
        updateDisplay()
    }

    override fun onUpdateAmbient() {
        super.onUpdateAmbient()
        updateDisplay()
    }

    override fun onExitAmbient() {
        updateDisplay()
        super.onExitAmbient()
    }

    private fun updateDisplay() {
        if (isAmbient) {
            mContainerView!!.setBackgroundColor(Color.BLACK)
            for (tv in mTextViews!!) {
                tv.setTextColor(Color.WHITE)
            }
        } else {
            mContainerView!!.background = null
            for (tv in mTextViews!!) {
                tv.setTextColor(Color.BLACK)
            }
            //            mStatsView.setTextColor(getResources().getColor(android.R.color.black));
            //mClockView.setVisibility(View.GONE);
        }
    }

    companion object {

        private val REQUEST_CODE_BOTH = 2
    }
}
