package com.igreenwood.loupesample.master

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.igreenwood.loupesample.*
import com.igreenwood.loupesample.databinding.ActivityMasterBinding
import com.igreenwood.loupesample.detail.DetailActivity
import com.igreenwood.loupesample.master.item.MultipleImageItem
import com.igreenwood.loupesample.master.item.SingleImageItem
import com.igreenwood.loupesample.util.ImageUrls
import com.igreenwood.loupesample.util.Pref
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import timber.log.Timber

class MasterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMasterBinding
    private var adapter = GroupAdapter<GroupieViewHolder>()

    private val singleImageListener = object : SingleImageItem.Listener {
        override fun onClick(view: View, url: String) {
            goToDetail(view, url)
        }
    }

    private val multipleImageListener = object :
        MultipleImageItem.Listener {
        override fun onClick(view: View, urls: List<String>, index: Int) {
            goToDetail(view, urls[index])
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,
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
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this@MasterActivity)
        binding.recyclerView.adapter = adapter
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            title = if(Pref.useSharedElements){
                "shared elements sample"
            } else {
                "swipe to dismiss sample"
            }
        }
    }

    private fun getActivityOption(targetView: View): ActivityOptionsCompat {
        return ActivityOptionsCompat.makeSceneTransitionAnimation(this, targetView, targetView.transitionName)
    }

    private fun goToDetail(view: View, url: String) {
        if (Pref.useSharedElements) {
            startActivity(
                DetailActivity.createIntent(
                    this@MasterActivity,
                    url
                ),
                getActivityOption(view).toBundle()
            )
        } else {
            startActivity(
                DetailActivity.createIntent(
                    this@MasterActivity,
                    url
                )
            )
            overridePendingTransition(R.anim.fade_in_fast, 0)
        }
    }
}
