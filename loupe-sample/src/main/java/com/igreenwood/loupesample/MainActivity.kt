package com.igreenwood.loupesample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.igreenwood.loupesample.databinding.ActivityMainBinding
import com.igreenwood.loupesample.master.MasterActivity
import com.igreenwood.loupesample.util.Pref

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initToolbar()
        binding.sharedElementSampleButton.setOnClickListener {
            Pref.useSharedElements = true
            goToMaster()
        }
        binding.swipeDownSampleButton.setOnClickListener {
            Pref.useSharedElements = false
            goToMaster()
        }
    }

    private fun goToMaster() {
        startActivity(Intent(this, MasterActivity::class.java))
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setLogo(R.drawable.logo)
            title = "loupe"
        }
    }
}
