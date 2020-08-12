package com.igreenwood.loupesample.master

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.SharedElementCallback
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.igreenwood.loupesample.R
import com.igreenwood.loupesample.databinding.ActivityMasterBinding
import com.igreenwood.loupesample.detail.DetailActivity
import com.igreenwood.loupesample.master.item.MultipleImageItem
import com.igreenwood.loupesample.master.item.SingleImageItem
import com.igreenwood.loupesample.util.ImageUrls
import com.igreenwood.loupesample.util.Pref
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder

class MasterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMasterBinding
    private var adapter = GroupAdapter<GroupieViewHolder>()

    private val singleImageListener = object : SingleImageItem.Listener {
        override fun onClick(adapterPosition: Int, sharedElement: ImageView, url: String) {
            goToDetail(adapterPosition, sharedElement, arrayListOf(url), 0)
        }
    }

    private val multipleImageListener = object :
        MultipleImageItem.Listener {
        override fun onClick(
            adapterPosition: Int,
            sharedElement: ImageView,
            urls: List<String>,
            index: Int
        ) {
            goToDetail(adapterPosition, sharedElement, ArrayList(urls), index)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_master
        )
        initToolbar()
        initRecyclerView()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun initRecyclerView() {
        adapter.apply {
            addAll(ImageUrls.urlGifs.map {
                SingleImageItem(
                    it,
                    singleImageListener
                )
            })
            add(
                MultipleImageItem(
                    ImageUrls.urls1,
                    multipleImageListener
                )
            )
            addAll(ImageUrls.urls2.map {
                SingleImageItem(
                    it,
                    singleImageListener
                )
            })
            add(
                MultipleImageItem(
                    ImageUrls.urls3,
                    multipleImageListener
                )
            )
            addAll(ImageUrls.urls4.map {
                SingleImageItem(
                    it,
                    singleImageListener
                )
            })
            add(
                MultipleImageItem(
                    ImageUrls.urls5,
                    multipleImageListener
                )
            )
            addAll(ImageUrls.urls1.map {
                SingleImageItem(
                    it,
                    singleImageListener
                )
            })
            add(
                MultipleImageItem(
                    ImageUrls.urls2,
                    multipleImageListener
                )
            )
            addAll(ImageUrls.urls3.map {
                SingleImageItem(
                    it,
                    singleImageListener
                )
            })
            add(
                MultipleImageItem(
                    ImageUrls.urls4,
                    multipleImageListener
                )
            )
            addAll(ImageUrls.urls5.map {
                SingleImageItem(
                    it,
                    singleImageListener
                )
            })
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this@MasterActivity)
        binding.recyclerView.adapter = adapter
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            title = if (Pref.useSharedElements) {
                "shared elements sample"
            } else {
                "swipe to dismiss sample"
            }
        }
    }

    private fun getActivityOption(targetView: View): ActivityOptionsCompat {
        return ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            targetView,
            targetView.transitionName
        )
    }

    private fun goToDetail(
        position: Int,
        sharedElement: ImageView,
        urls: ArrayList<String>,
        initialPos: Int
    ) {
        if (Pref.useSharedElements) {
            setExitSharedElementCallback(object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: MutableList<String>?,
                    sharedElements: MutableMap<String, View>?
                ) {
                    val transitionName = names?.get(0) ?: return
                    val ids = listOf(
                        R.id.image,
                        R.id.top_left_image,
                        R.id.top_right_image,
                        R.id.bottom_left_image,
                        R.id.bottom_right_image
                    )
                    val views = mutableListOf<ImageView>()
                    ids.forEach {
                        val view =
                            binding.recyclerView.layoutManager?.findViewByPosition(position)?.findViewById<ImageView>(
                                it
                            ) ?: return@forEach
                        views.add(view)
                    }
                    val view = views.find { transitionName == it.transitionName } ?: return
                    sharedElements?.put(transitionName, view)
                }
            })
            startActivity(
                DetailActivity.createIntent(
                    this@MasterActivity,
                    urls,
                    initialPos
                ),
                getActivityOption(sharedElement).toBundle()
            )
        } else {
            startActivity(
                DetailActivity.createIntent(
                    this@MasterActivity,
                    urls,
                    initialPos
                )
            )
            overridePendingTransition(R.anim.fade_in_fast, 0)
        }
    }
}
