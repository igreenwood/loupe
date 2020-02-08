package com.igreenwood.loupesample

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.igreenwood.loupe.LoupeImageView
import com.igreenwood.loupesample.databinding.ActivityImageDetailBinding
import timber.log.Timber
import java.io.File
import java.lang.Math.round
import kotlin.math.roundToInt

class ImageDetailActivity : AppCompatActivity() {

    companion object {
        private const val ARGS_IMAGE_URL = "ARGS_IMAGE_URL"

        fun createIntent(context: Context, imageUrl: String): Intent {
            return Intent(context, ImageDetailActivity::class.java).apply {
                putExtra(ARGS_IMAGE_URL, imageUrl)
            }
        }
    }

    private lateinit var binding: ActivityImageDetailBinding
    private val url: String by lazy { intent.getStringExtra(ARGS_IMAGE_URL) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_image_detail)

        //android O fix bug orientation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            title = ""
        }

        Glide.with(this).load(url).override(binding.image.height).into(binding.image)
        binding.root.background = ColorDrawable(ContextCompat.getColor(this@ImageDetailActivity, R.color.black_alpha_87))
        binding.image.onDismissListener = object : LoupeImageView.OnViewTranslateListener {

            override fun onStart(view: LoupeImageView) {
                hideToolbar()
            }

            override fun onViewTranslate(view: LoupeImageView, progress: Float) {
                changeBackgroundAlpha(progress)
            }

            override fun onRestore(view: LoupeImageView) {
                showToolbar()
            }

            override fun onDismiss(view: LoupeImageView) {
                finish()
            }
        }
    }

    private fun changeBackgroundAlpha(progress: Float) {
        val newAlpha = ((1.0f - progress) * 255).roundToInt()
        binding.root.background.alpha = newAlpha
    }

    private fun showToolbar() {
        binding.toolbar.animate()
            .setInterpolator(AccelerateDecelerateInterpolator())
            .translationY(0f)
    }

    private fun hideToolbar() {
        binding.toolbar.animate()
            .setInterpolator(AccelerateDecelerateInterpolator())
            .translationY(-binding.toolbar.height.toFloat())
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.fade_out_fast)
    }
}
