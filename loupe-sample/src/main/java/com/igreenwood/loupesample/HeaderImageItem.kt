package com.igreenwood.loupesample

import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.igreenwood.loupesample.databinding.RowHeaderImageBinding
import com.xwray.groupie.databinding.BindableItem

class HeaderImageItem(var url: String, var listener: Listener): BindableItem<RowHeaderImageBinding>() {

    interface Listener {
        fun onClick(url: String)
    }

    override fun getLayout() = R.layout.row_header_image

    override fun bind(binding: RowHeaderImageBinding, position: Int) {
        Glide.with(binding.image.context)
            .load(url)
            .override(binding.image.width)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.image)
        binding.root.setOnClickListener {
            listener.onClick(url)
        }
    }
}