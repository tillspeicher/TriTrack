package de.tritrack.recording.ui

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

class DataScreenPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    private var count = 2
    private val labFragments = ArrayList<DataScreenFragment>()

    override fun getItem(position: Int): Fragment {
        assert(position < count)
        return if (position < labFragments.size) {
            labFragments[position]
        } else {
            val fragment = DataScreenFragment.newInstance(position)
            if (labFragments.size < 2)
                labFragments.add(fragment)
            else
                labFragments.add(2, fragment)
            fragment
        }
    }

    override fun getCount(): Int {
        return count
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0, 1 -> "Overall activity stats $position"
            else -> "Lap $position"
        }
    }

    fun addLabView() {
        count += 1
        notifyDataSetChanged()
    }

}