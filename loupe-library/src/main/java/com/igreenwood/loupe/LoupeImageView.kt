package com.igreenwood.loupe

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.widget.ImageView
import timber.log.Timber

class LoupeImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private var transfrom = Matrix()
    private var scale = 1f
    private var isReadyToDraw = false
    private var bitmapPaint = Paint().apply {
        isFilterBitmap = true
    }
    private var canvasBounds = RectF()
    private var bitmapBounds = RectF()
    private var maxBitmapBounds = RectF()
    private var offset = PointF()
    private var minScale = 1f
    private var maxScale = 1f
    private var imageWidth = 0f
    private var imageHeight = 0f
    private var drawable = null

    init {

    }

    override fun setImageDrawable(drawable: Drawable?) {
        isReadyToDraw = false
        super.setImageDrawable(drawable)
        updateLayout()
    }

    override fun setImageResource(resId: Int) {
        Timber.e("setImageResource")
        isReadyToDraw = false
        super.setImageResource(resId)
        updateLayout()
    }

    override fun setImageURI(uri: Uri?) {
        isReadyToDraw = false
        super.setImageURI(uri)
        updateLayout()
    }

    private fun updateLayout() {
        Timber.e("updateLayout")
        setupLayout()
    }

    override fun onDraw(canvas: Canvas?) {
        Timber.e("onDraw: isReadyToDraw = $isReadyToDraw")
        if (isReadyToDraw) {
            val bm = getBitmap()
            if (bm != null) {
                setTransform()
                canvas?.drawBitmap(bm, transfrom, bitmapPaint)
            }
        }
    }

    private fun setTransform() {
        Timber.e("setMatrix: canvasBounds = $canvasBounds")
        Timber.e("setMatrix: bitmapBounds = $bitmapBounds")
        Timber.e("scale = $scale")
        Timber.e("scaleX = $scaleX, scaleY = $scaleY")
        transfrom.apply {
            reset()
            postTranslate(-imageWidth/2, -imageHeight/2)
            postScale(scale, scale)
            postTranslate(bitmapBounds.centerX(), bitmapBounds.centerY())
        }
    }

    private fun getBitmap(): Bitmap? {
        return (getDrawable() as? BitmapDrawable)?.bitmap
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Timber.e("onSizeChanged")
        setupLayout()
    }

    /**
     * setup layout
     */
    private fun setupLayout() {
        val bm = getBitmap()
        Timber.e("setupLayout start: width = $width, height = $height, bm = $bm")
        if(width == 0 || height == 0 || bm == null) return
        imageWidth = bm.width.toFloat()
        imageHeight = bm.height.toFloat()
        val canvasWidth = (width - paddingLeft - paddingRight).toFloat()
        val canvasHeight = (height - paddingTop - paddingBottom).toFloat()

        setupScale(canvasWidth, canvasHeight, imageWidth, imageHeight)
        setupBounds(imageWidth, imageHeight)
//        constrainBitmapBounds()
        isReadyToDraw = true
        invalidate()
    }

    /**
     * constrain bitmap bounds inside max bitmap bounds
     */
    private fun constrainBitmapBounds() {
        // ensure inside max bitmap bounds
        if (bitmapBounds.left < maxBitmapBounds.left) {
            bitmapBounds.offset(maxBitmapBounds.left - bitmapBounds.left, 0f)
        }
        if (bitmapBounds.right > maxBitmapBounds.right) {
            bitmapBounds.offset(maxBitmapBounds.right - bitmapBounds.right, 0f)
        }
        if (bitmapBounds.top < maxBitmapBounds.top) {
            bitmapBounds.offset(0f, maxBitmapBounds.top - bitmapBounds.top)
        }
        if (bitmapBounds.bottom > maxBitmapBounds.bottom) {
            bitmapBounds.offset(0f, maxBitmapBounds.bottom - bitmapBounds.bottom)
        }
    }

    /**
     * calc canvas/bitmap bounds
     */
    private fun setupBounds(imgWidth: Float, imgHeight: Float) {
        Timber.e("setupBoundsd start")
        canvasBounds = RectF(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            width -paddingRight.toFloat(),
            height -paddingBottom.toFloat()
        )
        bitmapBounds = RectF(
            canvasBounds.centerX() - imgWidth * scale * 0.5f + offset.x,
            canvasBounds.centerY() - imgHeight * scale * 0.5f + offset.y,
            canvasBounds.centerX() + imgWidth * scale * 0.5f + offset.x,
            canvasBounds.centerY() + imgHeight * scale * 0.5f + offset.y
        )
        // calc max bitmap bounds
        maxBitmapBounds = RectF(bitmapBounds)
        when {
            bitmapBounds.width() <= canvasBounds.width() && bitmapBounds.height() > canvasBounds.height() -> {
                val diffY = bitmapBounds.height() - canvasBounds.height()
                maxBitmapBounds.top -= diffY * 0.5f
                maxBitmapBounds.bottom += diffY * 0.5f
            }
            bitmapBounds.height() <= canvasBounds.height() && bitmapBounds.width() > canvasBounds.width() -> {
                val diffX = bitmapBounds.width() - canvasBounds.width()
                maxBitmapBounds.left -= diffX * 0.5f
                maxBitmapBounds.right += diffX * 0.5f
            }
            bitmapBounds.width() > canvasBounds.width() && bitmapBounds.height() > canvasBounds.height() -> {
                val diffX = bitmapBounds.width() - canvasBounds.width()
                maxBitmapBounds.left -= diffX * 0.5f
                maxBitmapBounds.right += diffX * 0.5f
                val diffY = bitmapBounds.height() - canvasBounds.height()
                maxBitmapBounds.top -= diffY * 0.5f
                maxBitmapBounds.bottom += diffY * 0.5f
            }
        }
        Timber.e("setupBounds: canvasBounds = $canvasBounds, bitmapBounds = $bitmapBounds, maxBitmapBounds = $maxBitmapBounds")
    }

    /**
     * calc min/max scale and set initial scale
     */
    private fun setupScale(
        canvasWidth: Float,
        canvasHeight: Float,
        bitmapWidth: Float,
        bitmapHeight: Float
    ) {
        Timber.e("setupScale start")
        val canvasRatio = canvasHeight / canvasWidth
        val bitmapRatio = bitmapHeight / bitmapWidth
        minScale = if (canvasRatio > bitmapRatio) {
            canvasWidth / bitmapWidth
        } else {
            canvasHeight / bitmapHeight
        }
        scale = minScale
        maxScale = minScale * 2f
        Timber.e("setupScale: canvasWidth = $canvasWidth, bitmapWidth = $bitmapWidth, canvasHeight = $canvasHeight, bitmapHeight = $bitmapHeight")
        Timber.e("setupScale: canvasRatio = $canvasRatio, bitmapRatio = $bitmapRatio, minScale = $minScale")
    }
}