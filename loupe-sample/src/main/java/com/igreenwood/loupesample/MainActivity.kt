package com.igreenwood.loupesample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.igreenwood.loupesample.databinding.ActivityMainBinding
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var adapter = GroupAdapter<GroupieViewHolder>()
    private val singleImageListener = object : SingleImageItem.Listener {
        override fun onClick(url: String) {

        }

    }
    private val multipleImageListener = object : MultipleImageItem.Listener {
        override fun onClick(urls: List<String>, index: Int) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        adapter.apply {
            addAll(ImageUrls.singleImageUrls.map { SingleImageItem(it, singleImageListener) })
            add(MultipleImageItem(ImageUrls.multipleImageUrls, multipleImageListener))
            addAll(ImageUrls.singleImageUrls.map { SingleImageItem(it, singleImageListener) })
            add(MultipleImageItem(ImageUrls.multipleImageUrls, multipleImageListener))
            addAll(ImageUrls.singleImageUrls.map { SingleImageItem(it, singleImageListener) })
            add(MultipleImageItem(ImageUrls.multipleImageUrls, multipleImageListener))
            addAll(ImageUrls.singleImageUrls.map { SingleImageItem(it, singleImageListener) })
            add(MultipleImageItem(ImageUrls.multipleImageUrls, multipleImageListener))
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
        binding.recyclerView.adapter = adapter
    }
}
