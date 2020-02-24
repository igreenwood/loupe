package com.igreenwood.loupesample.master.item

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.igreenwood.loupesample.R
import com.igreenwood.loupesample.databinding.RowMultipleImageBinding
import com.igreenwood.loupesample.util.Pref
import com.xwray.groupie.databinding.BindableItem

class MultipleImageItem(var urls: List<String>, var listener: Listener) :
    BindableItem<RowMultipleImageBinding>() {

    interface Listener {
        fun onClick(adapterPosition: Int, sharedElement: ImageView, urls: List<String>, index: Int)
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

        binding.topLeftImage.setOnClickListener {
            setTransitionNameIfNeeded(binding)
            listener.onClick(position, binding.topLeftImage, urls, 0)
        }
        binding.topRightImage.setOnClickListener {
            setTransitionNameIfNeeded(binding)
            listener.onClick(position, binding.topRightImage, urls, 1)
        }
        binding.bottomLeftImage.setOnClickListener {
            setTransitionNameIfNeeded(binding)
            listener.onClick(position, binding.bottomLeftImage, urls, 2)
        }
        binding.bottomRightImage.setOnClickListener {
            setTransitionNameIfNeeded(binding)
            listener.onClick(position, binding.bottomRightImage, urls, 3)
        }
    }

    private fun setTransitionNameIfNeeded(binding: RowMultipleImageBinding){
        if (Pref.useSharedElements) {
            setTransitionName(binding)
        }
    }

    private fun setTransitionName(binding: RowMultipleImageBinding) {
        binding.topLeftImage.transitionName =
            binding.topLeftImage.context.getString(R.string.shared_image_transition, 0)
        binding.topRightImage.transitionName =
            binding.topRightImage.context.getString(R.string.shared_image_transition, 1)
        binding.bottomLeftImage.transitionName =
            binding.bottomLeftImage.context.getString(R.string.shared_image_transition, 2)
        if (Pref.useSharedElements) {
            binding.bottomRightImage.transitionName =
                binding.bottomRightImage.context.getString(R.string.shared_image_transition, 3)
        }
    }
}