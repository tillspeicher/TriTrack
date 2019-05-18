package de.tritrack.tritrack

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import de.tritrack.recording.ui.RecordingOverviewFragment

class RecordingOverviewActivity : AppCompatActivity(), RecordingOverviewFragment.OnListFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recording_overview_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, RecordingOverviewFragment.newInstance(1))
                    .commitNow()
        }
    }

    override fun onListFragmentInteraction(recordingId: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
