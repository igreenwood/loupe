package com.igreenwood.loupesample.master.item

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.igreenwood.loupesample.R
import com.igreenwood.loupesample.databinding.RowSingleImageBinding
import com.xwray.groupie.databinding.BindableItem

class SingleImageItem(var url: String, var listener: Listener) :
    BindableItem<RowSingleImageBinding>() {

    interface Listener {
        fun onClick(view: View, url: String)
    }

    override fun getLayout() = R.layout.row_single_image

    override fun bind(binding: RowSingleImageBinding, position: Int) {
        Glide.with(binding.image.context)
            .load(url)
            .override(binding.image.width)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.image)
        binding.root.setOnClickListener {
            listener.onClick(it, url)
        }
    }
}