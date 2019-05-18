package de.tritrack.tritrack

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.*
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.ImageButton

import de.tritrack.recording.recording.Recorder
import de.tritrack.recording.ui.DataScreenPagerAdapter


class MainActivity : AppCompatActivity() {

    private var mRec: Recorder? = null

    private var mViewPager: ViewPager? = null
    private var mDataAdapter: DataScreenPagerAdapter? = null
    private var mStartStopButton: ImageButton? = null
    private var mPauseResumeButton: ImageButton? = null
    private var mLapButton: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navigationView = findViewById<NavigationView>(R.id.main_drawer)
        navigationView!!.setNavigationItemSelectedListener {
            it.setChecked(true)
            when (it.itemId) {
                R.id.action_settings -> true
                R.id.action_ble_device_selection -> {
                    val bleDeviceSelectionIntent = Intent(this, BleDeviceSelectionActivity::class.java)
                    startActivity(bleDeviceSelectionIntent)
                }
                R.id.action_overview_activity -> {
                    val overviewIntent = Intent(this, RecordingOverviewActivity::class.java)
                    startActivity(overviewIntent)
                }
                R.id.action_end_app -> {
                    finish()
                }
            }
            true
        }

        mRec = Recorder.getInstance(applicationContext)

        mDataAdapter = DataScreenPagerAdapter(supportFragmentManager, this)
        mViewPager = findViewById(R.id.data_screen_pager)
        mViewPager!!.adapter = mDataAdapter
        // TODO: this is a hack because otherwise the first lap fragment starts delayed only when
        // it is scrolled to the first time. Fix that
//        mViewPager!!.setCurrentItem(2)

        mStartStopButton = findViewById(R.id.button_start_stop)
        mPauseResumeButton = findViewById(R.id.button_pause_resume)
        mLapButton = findViewById(R.id.button_lap)

        mStartStopButton!!.setOnClickListener { startStopTracking() }
        mPauseResumeButton!!.setOnClickListener {
            // TODO: listen for auto-pause and update the button
            // TODO: maybe reenable screen timeout when paused
            val isResumed = mRec!!.togglePause()
            if (isResumed) {
                mPauseResumeButton!!.setImageResource(R.drawable.pause_sym)
            } else {
                mPauseResumeButton!!.setImageResource(R.drawable.start_sym)
            }
        }
        mLapButton!!.setOnClickListener {
            mRec!!.segmentManager.nextSegment()
            // TODO
            //mDataAdapter!!.addLabView()
        }

        if (mRec!!.isRecording) {
            mStartStopButton!!.setImageResource(R.drawable.stop_sym)
            if (mRec!!.isRunning)
                mPauseResumeButton!!.setImageResource(R.drawable.pause_sym)
        } else {
            mPauseResumeButton!!.isEnabled = false
            mLapButton!!.isEnabled = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
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
        val isRecording = mRec!!.isRecording
        if (!isRecording) {
            // start recording
            if (mRec!!.wasStarted)
                // TODO: reset doesn't properly clear all data and Lap one sticks around in a paused way
                mDataAdapter!!.reset()
            mRec!!.toggleRecording()
            mStartStopButton!!.setImageResource(R.drawable.stop_sym)
            mPauseResumeButton!!.isEnabled = true
            mLapButton!!.isEnabled = true

            // disable screen timeout
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            // end recording
            val dialogBuilder = AlertDialog.Builder(this)
            // TODO: use string resources
            dialogBuilder.setMessage("End the activity?")
            dialogBuilder.setPositiveButton("OK", { dialog: DialogInterface, id: Int ->
                mRec!!.toggleRecording()
                mStartStopButton!!.setImageResource(R.drawable.start_sym)
                mPauseResumeButton!!.setImageResource(R.drawable.pause_sym)
                mPauseResumeButton!!.isEnabled = false
                mLapButton!!.isEnabled = false

                // re-enable screen timeout
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            })
            dialogBuilder.setNegativeButton("Cancel", { dialog: DialogInterface, id: Int ->
                // do nothing
            })
            val stopConfirmationDialog = dialogBuilder.create()
            // TODO: maybe pause the recording while showing the dialog
            stopConfirmationDialog.show()
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

    // end copy paste from wear

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        if (id == R.id.action_settings) {
            return true
        } else if (id == R.id.action_ble_device_selection) {
            val bleDeviceSelectionIntent = Intent(this, BleDeviceSelectionActivity::class.java)
            startActivity(bleDeviceSelectionIntent)
            return true
        } else if (id == R.id.action_overview_activity) {
            val overviewIntent = Intent(this, RecordingOverviewActivity::class.java)
            startActivity(overviewIntent)
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {

        private val REQUEST_CODE_LOCATION = 0
        private val REQUEST_CODE_EXTERNAL_STORAGE = 1
        private val REQUEST_CODE_BOTH = 2
    }

}
