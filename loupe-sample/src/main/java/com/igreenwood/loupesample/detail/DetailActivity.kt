package com.igreenwood.loupesample.detail

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.SharedElementCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.igreenwood.loupe.Loupe
import com.igreenwood.loupesample.R
import com.igreenwood.loupesample.databinding.ActivityDetailBinding
import com.igreenwood.loupesample.databinding.ItemImageBinding
import com.igreenwood.loupesample.util.Pref
import kotlinx.android.synthetic.main.activity_detail.*
import kotlin.math.roundToInt

class DetailActivity : AppCompatActivity() {

    companion object {
        private const val ARGS_IMAGE_URLS = "ARGS_IMAGE_URLS"
        private const val ARGS_INITIAL_POSITION = "ARGS_INITIAL_POSITION"

        fun createIntent(context: Context, urls: ArrayList<String>, initialPos: Int): Intent {
            return Intent(context, DetailActivity::class.java).apply {
                putExtra(ARGS_IMAGE_URLS, urls)
                putExtra(ARGS_INITIAL_POSITION, initialPos)
            }
        }
    }

    private lateinit var binding: ActivityDetailBinding
    private val urls: List<String> by lazy { intent.getSerializableExtra(ARGS_IMAGE_URLS) as List<String> }
    private val initialPos: Int by lazy { intent.getIntExtra(ARGS_INITIAL_POSITION, 0) }
    private lateinit var adapter: ImageAdapter

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
        adapter = ImageAdapter(this, urls)
        binding.viewpager.adapter = adapter
        binding.viewpager.currentItem = initialPos
        if (Pref.useSharedElements) {

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

    inner class ImageAdapter(var context: Context, var urls: List<String>) : PagerAdapter() {

        private var loupeMap = hashMapOf<Int, Loupe>()
        private var views = hashMapOf<Int, ImageView>()

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val binding = ItemImageBinding.inflate(LayoutInflater.from(context))
            container.addView(binding.root)
            loadImage(binding.image, position)
            views[position] = binding.image
            return binding.root
        }

        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
            container.removeView(obj as View)
        }

        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view == obj
        }

        override fun getCount() = urls.size

        private fun loadImage(image: ImageView, position: Int) {
            if (Pref.useSharedElements) {
                // shared elements
                Glide.with(image.context)
                    .load(urls[position])
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
                            image.transitionName =
                                context.getString(R.string.shared_image_transition, position)
                            val loupe = Loupe(image).apply {
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

                            loupeMap[position] = loupe

                            if (position == initialPos) {
                                setEnterSharedElementCallback(object : SharedElementCallback() {
                                    override fun onMapSharedElements(
                                        names: MutableList<String>?,
                                        sharedElements: MutableMap<String, View>?
                                    ) {
                                        names ?: return
                                        val view = views[viewpager.currentItem] ?: return
                                        view.transitionName = context.getString(
                                            R.string.shared_image_transition,
                                            viewpager.currentItem
                                        )
                                        sharedElements?.clear()
                                        sharedElements?.put(view.transitionName, view)
                                    }
                                })
                                startPostponedEnterTransition()
                            }
                            return false
                        }

                    })
                    .into(image)
            } else {
                // swipe to dismiss
                Glide.with(image.context).load(urls[position])
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
                            val loupe = Loupe(image).apply {
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
                            loupeMap[position] = loupe

                            if (position == initialPos) {
                                startPostponedEnterTransition()
                            }
                            return false
                        }

                    }).into(image)
            }
        }

    }
}
