package com.igreenwood.loupesample

import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.igreenwood.loupesample.databinding.RowMultipleImageBinding
import com.xwray.groupie.databinding.BindableItem

class MultipleImageItem(var urls: List<String>, var listener: Listener) :
    BindableItem<RowMultipleImageBinding>() {

    interface Listener {
        fun onClick(urls: List<String>)
    }

    override fun getLayout() = R.layout.row_multiple_image

    override fun bind(binding: RowMultipleImageBinding, position: Int) {
        Glide.with(binding.topLeftImage.context)
            .load(urls[0])
            .override(binding.topLeftImage.width)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.topLeftImage)
        Glide.with(binding.topRightImage.context)
            .load(urls[1])
            .override(binding.topRightImage.width)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.topRightImage)
        Glide.with(binding.bottomLeftImage.context)
            .load(urls[2])
            .override(binding.bottomLeftImage.width)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.bottomLeftImage)
        Glide.with(binding.bottomRightImage.context)
            .load(urls[3])
            .override(binding.bottomRightImage.width)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.bottomRightImage)
        binding.root.setOnClickListener {
            listener.onClick(urls)
        }
    }
}