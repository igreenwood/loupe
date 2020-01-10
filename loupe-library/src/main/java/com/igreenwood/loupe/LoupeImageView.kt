package com.igreenwood.loupe

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

class LoupeImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    companion object{
        const val DEFAULT_MAX_ZOOM = 4.0f
    }

    // bitmap matrix
    private var transfrom = Matrix()
    // bitmap scale
    private var scale = 1f
    // is ready for drawing bitmap
    private var isReadyToDraw = false
    // bitmap paint
    private var bitmapPaint = Paint().apply {
        isFilterBitmap = true
    }
    // view rect - padding (recalculated on size changed)
    private var canvasBounds = RectF()
    // bitmap drawing rect (move on scroll, recalculated on scale changed)
    private var bitmapBounds = RectF()
    // displaying bitmap rect (does not move, recalculated on scale changed)
    private var viewport = RectF()
    // minimum scale of bitmap
    private var minBmScale = 1f
    // maximum scale of bitmap
    private var maxBmScale = 1f
    // max zoom (1.0~imageMaxScale)
    private var maxZoom = DEFAULT_MAX_ZOOM
    // bitmap (decoded) width
    private var imageWidth = 0f
    // bitmap (decoded) height
    private var imageHeight = 0f
    // scaling helper
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private val onScaleGestureListener: ScaleGestureDetector.OnScaleGestureListener =
        object : ScaleGestureDetector.OnScaleGestureListener {

            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                Timber.e("onScale")
                detector?.run {
                    scale = calcNewScale(scaleFactor)
                    // scale has changed, recalculate bitmap bounds
                    calcBounds()
                    // TODO should zoom into touch point
                    offsetBitmap(0f, 0f)
                    constrainBitmapBounds()
                }
                return true
            }

            override fun onScaleBegin(p0: ScaleGestureDetector?): Boolean = true

            override fun onScaleEnd(p0: ScaleGestureDetector?) {}
        }

    // translating helper
    private var gestureDetector: GestureDetector? = null
    private val onGestureListener: GestureDetector.OnGestureListener =
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean = true

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent?,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e2?.pointerCount != 1) {
                    return true
                }

                if (scale > minBmScale) {
                    offsetBitmap(-distanceX, -distanceY)
                    constrainBitmapBounds()
                } else {
                    // TODO スワイプイベントを通知
                }

                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                return super.onFling(e1, e2, velocityX, velocityY)
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                return super.onDoubleTap(e)
            }


        }

    init {
        scaleGestureDetector = ScaleGestureDetector(context, onScaleGestureListener)
        gestureDetector = GestureDetector(context, onGestureListener)
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
            Timber.e("bitmapBounds = $bitmapBounds")
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
        transfrom.apply {
            reset()
            postTranslate(-imageWidth / 2, -imageHeight / 2)
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
        if (width == 0 || height == 0 || bm == null) return
        imageWidth = bm.width.toFloat()
        imageHeight = bm.height.toFloat()
        val canvasWidth = (width - paddingLeft - paddingRight).toFloat()
        val canvasHeight = (height - paddingTop - paddingBottom).toFloat()

        calcScaleRange(canvasWidth, canvasHeight, imageWidth, imageHeight)
        calcBounds()
        constrainBitmapBounds()
        isReadyToDraw = true
        invalidate()
    }

    /**
     * constrain bitmap bounds inside max bitmap bounds
     */
    private fun constrainBitmapBounds() {
        Timber.e("constrainBitmapBounds start")
        Timber.e("bitmapBounds = $bitmapBounds")
        Timber.e("viewport = $viewport")

        var offset = PointF()

        // constrain viewport inside bitmap bounds
        if (viewport.left < bitmapBounds.left) {
            offset.x += viewport.left - bitmapBounds.left
        }

        if (viewport.top < bitmapBounds.top) {
            offset.y += viewport.top - bitmapBounds.top
        }

        if (viewport.right > bitmapBounds.right) {
            offset.x += viewport.right - bitmapBounds.right
        }

        if (viewport.bottom > bitmapBounds.bottom) {
            offset.y += viewport.bottom - bitmapBounds.bottom
        }

        bitmapBounds.offset(offset.x, offset.y)
    }

    /**
     * calc canvas/bitmap bounds
     */
    private fun calcBounds() {
        Timber.e("setupBoundsd start")
        // calc canvas bounds
        canvasBounds = RectF(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            width - paddingRight.toFloat(),
            height - paddingBottom.toFloat()
        )
        // calc bitmap bounds
        bitmapBounds = RectF(
            canvasBounds.centerX() - imageWidth * scale * 0.5f,
            canvasBounds.centerY() - imageHeight * scale * 0.5f,
            canvasBounds.centerX() + imageWidth * scale * 0.5f,
            canvasBounds.centerY() + imageHeight * scale * 0.5f
        )
        // calc viewport
        viewport = RectF(
            max(canvasBounds.left, bitmapBounds.left),
            max(canvasBounds.top, bitmapBounds.top),
            min(canvasBounds.right, bitmapBounds.right),
            min(canvasBounds.bottom, bitmapBounds.bottom)
        )
        Timber.e("setupBounds: canvasBounds = $canvasBounds, bitmapBounds = $bitmapBounds, maxBitmapBounds = $viewport")
    }

    private fun offsetBitmap(offsetX: Float, offsetY: Float) {
        bitmapBounds.offset(offsetX, offsetY)
        Timber.e("offsetX = $offsetX, offsetY = $offsetY")
        Timber.e("offsetBitmap: bitmapBounds = $bitmapBounds")
    }

    /**
     * calc min/max scale and set initial scale
     */
    private fun calcScaleRange(
        canvasWidth: Float,
        canvasHeight: Float,
        bitmapWidth: Float,
        bitmapHeight: Float
    ) {
        Timber.e("setupScale start")
        val canvasRatio = canvasHeight / canvasWidth
        val bitmapRatio = bitmapHeight / bitmapWidth
        minBmScale = if (canvasRatio > bitmapRatio) {
            canvasWidth / bitmapWidth
        } else {
            canvasHeight / bitmapHeight
        }
        scale = minBmScale
        maxBmScale = minBmScale * maxZoom
        Timber.e("setupScale: canvasWidth = $canvasWidth, bitmapWidth = $bitmapWidth, canvasHeight = $canvasHeight, bitmapHeight = $bitmapHeight")
        Timber.e("setupScale: canvasRatio = $canvasRatio, bitmapRatio = $bitmapRatio, minScale = $minBmScale")
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        val result = super.dispatchTouchEvent(event)

        if (!isEnabled) {
            return result
        }

        scaleGestureDetector?.onTouchEvent(event)
        gestureDetector?.onTouchEvent(event)

        invalidate()

        return true
    }

    private fun calcNewScale(newScale: Float): Float {
        return constrain(minBmScale, newScale * scale, maxBmScale)
    }

    private fun constrain(min: Float, value: Float, max: Float): Float {
        return max(min(value, max), min)
    }
}