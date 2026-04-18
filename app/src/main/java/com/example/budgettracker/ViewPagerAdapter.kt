package com.example.budgettracker

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TransactionsFragment()
            1 -> LoansFragment()
            2 -> SettingsFragment()
            else -> AboutFragment()
        }
    }
}
