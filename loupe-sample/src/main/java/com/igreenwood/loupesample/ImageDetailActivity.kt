package com.igreenwood.loupesample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.igreenwood.loupesample.databinding.ActivityImageDetailBinding

class ImageDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_image_detail)
    }
}
