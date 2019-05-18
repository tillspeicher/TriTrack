package de.tritrack.recording.ui

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter
import de.tritrack.recording.recording.Recorder
import de.tritrack.recording.recording.SegmentManager

class DataScreenPagerAdapter(fm: FragmentManager, context: Context) : FragmentPagerAdapter(fm) {

    // TODO: make the number of overall and per lap pages dynamic
    private val lapFragments = ArrayList<DataScreenFragment>()
    private val segmentIds: MutableList<Int> = ArrayList()

    init {
        // TODO: hardcoded and hacky
        segmentIds.add(SegmentManager.GLOBAL_SEGMENT_ID)
        segmentIds.add(SegmentManager.GLOBAL_SEGMENT_ID)
        val segmentManager = Recorder.getInstance(context).segmentManager
        val totalFeatures = DataScreenFragment.getAllDataDescriptors()
        // listen to new segments
        segmentManager.monitorFeatures(totalFeatures, { segmentId ->
            segmentIds.add(segmentId)
            notifyDataSetChanged()
        })
    }

    override fun getItem(position: Int): Fragment {
        // TODO: check why restarting doesn't work
        assert(position < segmentIds.size)
        // finds the "original" position
        val remappedPosition = remapPosition(position)
        return if (remappedPosition < lapFragments.size) {
            lapFragments[remappedPosition]
        } else {
            val segmentId = segmentIds[remappedPosition]
            val fragment = DataScreenFragment.newInstance(remappedPosition, segmentId)
            lapFragments.add(fragment)
            fragment
        }
    }

    override fun getCount(): Int {
        return segmentIds.size
    }

    override fun getItemId(position: Int): Long {
        return remapPosition(position).toLong()
    }

    override fun getItemPosition(fragment: Any): Int {
        val fragmentPos = lapFragments.indexOf(fragment)
        return if (fragmentPos < 0)
            PagerAdapter.POSITION_NONE
        else if (fragmentPos < OVERALL_PAGES_COUNT)
            PagerAdapter.POSITION_UNCHANGED
        else
            remapPosition(fragmentPos)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        val remappedPosition = remapPosition(position)
        return when (remappedPosition) {
            0, 1 -> "Overall stats ${remappedPosition + 1}"
            else -> "Lap ${remappedPosition - 1}"
        }
    }

    private fun remapPosition(position: Int): Int {
        return if (position < OVERALL_PAGES_COUNT)
            position
        else
            (OVERALL_PAGES_COUNT - 1) + segmentIds.size - position
    }

    fun reset() {
        lapFragments.clear()
        segmentIds.clear()
        // TODO: hacky, code duplication
        segmentIds.add(SegmentManager.GLOBAL_SEGMENT_ID)
        segmentIds.add(SegmentManager.GLOBAL_SEGMENT_ID)
        notifyDataSetChanged()
    }

    companion object {

        private const val INIT_COUNT = 2
        // number of overall data pages (not laps)
        private const val OVERALL_PAGES_COUNT = 2

    }

}