package com.app.meditec.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.app.meditec.R
import com.app.meditec.adapters.OnBoardingViewPagerAdapter
import com.app.meditec.databinding.ActivityStartupBinding
import com.app.meditec.repository.MapsRepository
import com.app.meditec.ui.maps.MapsActivity
import com.app.meditec.utils.PermissionUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy

class StartUpActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityStartupBinding
    private var mPagerAdapter: OnBoardingViewPagerAdapter? = null
    private var position = 0
    private var mIsLoadLastScreen = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        if (checkSharePref()){
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            finish()
        }

        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_startup)


        if (PermissionUtils.isGoogleServicesAvailable(this))
            mBinding.getStartedBtn.setOnClickListener {
                startActivity(Intent(this, MapsActivity::class.java))
            }

        mPagerAdapter = OnBoardingViewPagerAdapter(this, MapsRepository.getOnBoardingItems())
        mBinding.viewPager.adapter = mPagerAdapter
        mBinding.isLoadLastScreen = mIsLoadLastScreen
        val tabLayoutMediator = TabLayoutMediator(mBinding.tabLayout, mBinding.viewPager,
                TabConfigurationStrategy { _: TabLayout.Tab?, _: Int -> })
        tabLayoutMediator.attach()

        mBinding.nextButton.setOnClickListener {
            position = mBinding.viewPager.currentItem
            if (position < MapsRepository.getOnBoardingItems().size) {
                position++
                mBinding.viewPager.currentItem = position
            }
            if (position == MapsRepository.getOnBoardingItems().size - 1)
                loadLastScreen()
        }

        mBinding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == MapsRepository.getOnBoardingItems().size - 1)
                    loadLastScreen()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        mBinding.skipButton.setOnClickListener {
            if (position != MapsRepository.getOnBoardingItems().size - 1)
                loadLastScreen()
        }
    }

    private fun loadLastScreen() {
        mBinding.viewPager.currentItem = MapsRepository.getOnBoardingItems().size- 1
        mIsLoadLastScreen = true
        mBinding.isLoadLastScreen = mIsLoadLastScreen
        saveSharedPref()
    }

    private fun saveSharedPref() {
        val sharedPreferences = applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isIntroOpened", true)
        editor.apply()
    }

    private fun checkSharePref(): Boolean {
        val sharedPreferences = applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isIntroOpened", false)
    }

    companion object {
        private const val TAG = "StartUpActivity"
        const val ERROR_DIALOG_REQUEST = 401
    }
}