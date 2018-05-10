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
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

import java.util.ArrayList

import de.tritrack.recording.recording.ActFeature
import de.tritrack.recording.recording.OpType
import de.tritrack.recording.recording.Recorder
import de.tritrack.recording.recording.UICommunication
import de.tritrack.recording.ui.DataTableProvider
import io.reactivex.disposables.Disposable

class MainActivity : AppCompatActivity() {

    private var mRec: Recorder? = null

    private var mViewPager: ViewPager? = null
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
                R.id.action_end_app -> {
                    finish()
                }
            }
            true
        }

        mRec = Recorder.getInstance(applicationContext)

        val adapter = InfoScreenPagerAdapter(supportFragmentManager)
        mViewPager = findViewById<ViewPager>(R.id.info_screen_pager)
        mViewPager!!.adapter = adapter

        mStartStopButton = findViewById<ImageButton>(R.id.button_start_stop)
        mStartStopButton!!.setOnClickListener { startStopTracking() }
        mPauseResumeButton = findViewById<ImageButton>(R.id.button_pause_resume)
        mPauseResumeButton!!.isEnabled = false
        mLapButton = findViewById<ImageButton>(R.id.button_lap)
        mLapButton!!.isEnabled = false
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
            mRec!!.toggleRecording()
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
            val dialogBuilder = AlertDialog.Builder(this)
            // TODO: use string resources
            dialogBuilder.setMessage("End the activity?")
            dialogBuilder.setPositiveButton("OK", { dialog: DialogInterface, id: Int ->
                mRec!!.toggleRecording()
                mStartStopButton!!.setImageResource(R.drawable.start_sym)
                mPauseResumeButton!!.setImageResource(R.drawable.pause_sym)
                mPauseResumeButton!!.isEnabled = false
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
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {

        private val REQUEST_CODE_LOCATION = 0
        private val REQUEST_CODE_EXTERNAL_STORAGE = 1
        private val REQUEST_CODE_BOTH = 2
    }

    class InfoScreenPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return InfoScreenFragment.newInstance(position)
        }

        override fun getCount(): Int {
            // TODO: make configurable
            return 2
        }

    }

    class InfoScreenFragment : Fragment() {

        companion object {

            private val ARG_SCREEN_ID = "screen_id"

            fun newInstance(id: Int): InfoScreenFragment {
                val args = Bundle()
                args.putInt(ARG_SCREEN_ID, id)
                val fragment = InfoScreenFragment()
                fragment.arguments = args
                return fragment
            }
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val contentLayout: LinearLayout = inflater.inflate(R.layout.content_main, container, false) as LinearLayout
//            return contentLayout

            val screenId = arguments!!.getInt(ARG_SCREEN_ID)

            // TODO: make configurable
            val featureLayout: Array<Array<Pair<ActFeature, OpType>>> = when(screenId) {
                0 -> arrayOf(arrayOf(Pair(ActFeature.TIME_S, OpType.ID), Pair(ActFeature.DISTANCE_KM, OpType.ID)),
                        arrayOf(Pair(ActFeature.SPEED_KMH, OpType.ID), Pair(ActFeature.SPEED_KMH, OpType.AVG)),
                        arrayOf(Pair(ActFeature.ELEVATION_GAIN, OpType.ID), Pair(ActFeature.SPEED_KMH, OpType.MAX)),
                        arrayOf(Pair(ActFeature.HEART_RATE, OpType.ID), Pair(ActFeature.HEART_RATE, OpType.AVG)),
                        arrayOf(Pair(ActFeature.POWER_COMBINED, OpType.ID), Pair(ActFeature.POWER_COMBINED, OpType.NORM_AVG)),
                        arrayOf(Pair(ActFeature.CADENCE, OpType.ID), Pair(ActFeature.CADENCE, OpType.NORM_AVG)))
                1 -> arrayOf(arrayOf(Pair(ActFeature.DISTANCE_KM_REV, OpType.ID), Pair(ActFeature.SPEED_KMH_REV, OpType.ID)),
                        arrayOf(Pair(ActFeature.HEART_RATE, OpType.MAX), Pair(ActFeature.POWER_COMBINED, OpType.MAX)),
                        arrayOf(Pair(ActFeature.POWER_LEFT, OpType.ID), Pair(ActFeature.POWER_RIGHT, OpType.ID)),
                        arrayOf(Pair(ActFeature.POWER_COMBINED, OpType.AVG), Pair(ActFeature.CADENCE, OpType.AVG)))
                else -> {
                    TODO("implement this")
                }
            }
            //val contentLayout = findViewById<LinearLayout>(R.id.content_main)
            val textViews = ArrayList<TextView>()
            val listeners = DataTableProvider.getTableView(featureLayout, inflater, contentLayout, textViews)

            val recorder = Recorder.getInstance(activity!!.applicationContext)
            recorder.addDataListeners(listeners)

            return contentLayout
        }
    }

}
