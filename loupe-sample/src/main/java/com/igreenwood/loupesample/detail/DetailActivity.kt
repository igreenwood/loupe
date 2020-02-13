package com.igreenwood.loupesample.detail

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.igreenwood.loupe.LoupeImageView
import com.igreenwood.loupesample.R
import com.igreenwood.loupesample.databinding.ActivityDetailBinding
import com.igreenwood.loupesample.util.Pref
import kotlin.math.roundToInt

class DetailActivity : AppCompatActivity() {

    companion object {
        private const val ARGS_IMAGE_URL = "ARGS_IMAGE_URL"

        fun createIntent(context: Context, imageUrl: String): Intent {
            return Intent(context, DetailActivity::class.java).apply {
                putExtra(ARGS_IMAGE_URL, imageUrl)
            }
        }
    }

    private lateinit var binding: ActivityDetailBinding
    private val url: String by lazy { intent.getStringExtra(ARGS_IMAGE_URL) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(Pref.useSharedElements){
            supportPostponeEnterTransition()
        }

        binding = DataBindingUtil.setContentView(this,
            R.layout.activity_detail
        )

        //android O fix orientation bug
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        initToolbar()

        binding.root.background = ColorDrawable(ContextCompat.getColor(this@DetailActivity,
            R.color.black_alpha_87
        ))

        if(Pref.useSharedElements){
            // shared elements
            Glide.with(binding.image.context)
                .load(url)
                .apply(RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE))
                .thumbnail(
                    Glide.with(binding.image.context)
                        .load(url)
                        .listener(
                            object: RequestListener<Drawable>{
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    supportStartPostponedEnterTransition()
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    supportStartPostponedEnterTransition()
                                    return false
                                }

                            }
                        )
                )
                .into(binding.image)
            binding.image.useDismissAnimation = false
            binding.image.onDismissListener = object : LoupeImageView.OnViewTranslateListener {

                override fun onStart(view: LoupeImageView) {
                    hideToolbar()
                }

                override fun onViewTranslate(view: LoupeImageView, amount: Float) {
                    changeBackgroundAlpha(amount)
                }

                override fun onRestore(view: LoupeImageView) {
                    showToolbar()
                }

                override fun onDismiss(view: LoupeImageView) {
                    supportFinishAfterTransition()
                }
            }
        } else {
            // swipe to dismiss
            Glide.with(binding.image.context).load(url).override(binding.image.height).into(binding.image)

            binding.image.onDismissListener = object : LoupeImageView.OnViewTranslateListener {

                override fun onStart(view: LoupeImageView) {
                    hideToolbar()
                }

                override fun onViewTranslate(view: LoupeImageView, amount: Float) {
                    changeBackgroundAlpha(amount)
                }

                override fun onRestore(view: LoupeImageView) {
                    showToolbar()
                }

                override fun onDismiss(view: LoupeImageView) {
                    finish()
                }
            }
        }
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            title = ""
        }
    }

    private fun changeBackgroundAlpha(amount: Float) {
        val newAlpha = ((1.0f - amount) * 255).roundToInt()
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
        if(Pref.useSharedElements){
            supportFinishAfterTransition()
        } else {
            finish()
        }
        return true
    }

    override fun finish() {
        super.finish()
        if(!Pref.useSharedElements){
            overridePendingTransition(0, R.anim.fade_out_fast)
        }
    }
}
