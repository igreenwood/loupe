package com.igreenwood.loupesample.detail

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.igreenwood.loupe.Loupe
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
    private var loupe: Loupe? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Pref.useSharedElements) {
            postponeEnterTransition()
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail)

        //android O fix orientation bug
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        initToolbar()

        binding.root.background = ColorDrawable(
            ContextCompat.getColor(
                this@DetailActivity,
                R.color.black_alpha_87
            )
        )

        if (Pref.useSharedElements) {
            // shared elements
            Glide.with(binding.image.context)
                .load(url)
                .onlyRetrieveFromCache(true)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        startPostponedEnterTransition()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        loupe = Loupe(binding.image).apply {
                            useDismissAnimation = false
                            onDismissListener = object : Loupe.OnViewTranslateListener {

                                override fun onStart(view: ImageView) {
                                    hideToolbar()
                                }

                                override fun onViewTranslate(view: ImageView, amount: Float) {
                                    changeBackgroundAlpha(amount)
                                }

                                override fun onRestore(view: ImageView) {
                                    showToolbar()
                                }

                                override fun onDismiss(view: ImageView) {
                                    finishAfterTransition()
                                }
                            }
                        }
                        startPostponedEnterTransition()
                        return false
                    }

                })
                .into(binding.image)
        } else {
            // swipe to dismiss
            Glide.with(binding.image.context).load(url)
                .onlyRetrieveFromCache(true)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        startPostponedEnterTransition()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        loupe = Loupe(binding.image).apply {
                            onDismissListener = object : Loupe.OnViewTranslateListener {

                                override fun onStart(view: ImageView) {
                                    hideToolbar()
                                }

                                override fun onViewTranslate(view: ImageView, amount: Float) {
                                    changeBackgroundAlpha(amount)
                                }

                                override fun onRestore(view: ImageView) {
                                    showToolbar()
                                }

                                override fun onDismiss(view: ImageView) {
                                    finish()
                                }
                            }
                        }
                        startPostponedEnterTransition()
                        return false
                    }

                }).into(binding.image)
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
        if (Pref.useSharedElements) {
            supportFinishAfterTransition()
        } else {
            finish()
        }
        return true
    }

    override fun finish() {
        super.finish()
        if (!Pref.useSharedElements) {
            overridePendingTransition(0, R.anim.fade_out_fast)
        }
    }
}
