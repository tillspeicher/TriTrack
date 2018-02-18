package de.tritrack.tritrack

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import de.tritrack.recording.recording.BlePool

import de.tritrack.recording.recording.Recorder
import de.tritrack.recording.ui.SensorListFragment

class BleDeviceSelectionActivity : AppCompatActivity() {

    private var mRecorder: Recorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_device_selection)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

}
