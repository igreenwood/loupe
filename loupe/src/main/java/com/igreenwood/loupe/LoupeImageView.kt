package com.igreenwood.loupe

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.widget.ImageView

class LoupeImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private var transfrom = Matrix()
    private var scale = 1f
    private var degrees = 0f
    private var imageCenter = PointF()
    private var initialized = false
    private var bitmapPaint = Paint().apply {
        isFilterBitmap = true
    }

    init {

    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        reset()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        reset()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        reset()
    }

    private fun reset() {

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if(initialized){
            val bm = getBitmap()
            if(bm != null){
                setMatrix()
                canvas?.drawBitmap(bm, transfrom, bitmapPaint)
            }
        }
    }

    private fun setMatrix() {
        transfrom.apply {
            reset()
            postTranslate(imageCenter.x, imageCenter.y)
            postScale(scale, scale)
            postRotate(degrees)
        }
    }

    private fun getBitmap(): Bitmap? {
        return (drawable as? BitmapDrawable)?.bitmap
    }
}