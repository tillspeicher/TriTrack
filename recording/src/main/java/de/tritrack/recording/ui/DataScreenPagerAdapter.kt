package de.tritrack.recording.ui

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter

class DataScreenPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    // TODO: make the number of overall and per lap pages dynamic
    private var count = INIT_COUNT
    private val lapFragments = ArrayList<DataScreenFragment>()

    override fun getItem(position: Int): Fragment {
        assert(position < count)
        val remappedPosition = remapPosition(position)
        return if (remappedPosition < lapFragments.size) {
            lapFragments[remappedPosition]
        } else {
            val fragment = DataScreenFragment.newInstance(remappedPosition)
            lapFragments.add(fragment)
            fragment
        }
    }

    override fun getCount(): Int {
        return count
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
            (OVERALL_PAGES_COUNT - 1) + count - position
    }

    fun addLabView() {
        if (count > OVERALL_PAGES_COUNT)
            lapFragments.last().stop()
        count += 1
        notifyDataSetChanged()
    }

    fun reset() {
        lapFragments.clear()
        count = INIT_COUNT
        notifyDataSetChanged()
    }

    companion object {

        private const val INIT_COUNT = 3
        // number of overall data pages (not laps)
        private const val OVERALL_PAGES_COUNT = 2

    }

}