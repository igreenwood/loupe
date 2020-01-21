package com.igreenwood.loupe

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class LoupeImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_MAX_ZOOM = 10.0f
        const val ANIM_DURATION = 250L
        const val DEFAULT_DISMISS_THRESHOLD_RATIO = 0.15f
    }

    interface OnDismissListener {
        fun onDismiss(view: LoupeImageView)
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
                val scaleFactor = detector?.scaleFactor ?: 1.0f
                val focusX = detector?.focusX ?: bitmapBounds.centerX()
                val focusY = detector?.focusY ?: bitmapBounds.centerY()

                if (detector?.scaleFactor == 1.0f) {
                    // scale is not changing
                    return true
                }

                scale = calcNewScale(scaleFactor)

                if (scale >= minBmScale) {
                    zoomTo(focusX, focusY)
                } else {
                    val startScale = scale
                    ValueAnimator.ofFloat(startScale, minBmScale).apply {
                        duration = ANIM_DURATION
                        interpolator = DecelerateInterpolator()
                        addUpdateListener {
                            val newScale = it.animatedValue as Float
                            scale = newScale
                            zoomTo(viewport.centerX(), viewport.centerY())
                            ViewCompat.postInvalidateOnAnimation(this@LoupeImageView)
                        }
                    }.start()
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
                } else if (scale == minBmScale) {
                    applyDismissEffect(distanceY)
                }
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                e1 ?: return true

                Timber.e("onFling: velocity = ($velocityX, $velocityY}) ======================================")

                Timber.e("viewport = $viewport")
                Timber.e("bitmapBounds = $bitmapBounds")

                if (scale > minBmScale) {
                    val (velX, velY) = velocityX to velocityY

                    Timber.e("vel = ($velX, $velY)")

                    if (velX == 0f && velY == 0f) {
                        return true
                    }

                    val (fromX, fromY) = bitmapBounds.left to bitmapBounds.top

                    scroller.forceFinished(true)
                    scroller.fling(
                        fromX.roundToInt(),
                        fromY.roundToInt(),
                        velX.roundToInt(),
                        velY.roundToInt(),
                        (viewport.right - bitmapBounds.width()).roundToInt(),
                        viewport.left.roundToInt(),
                        (viewport.bottom - bitmapBounds.height()).roundToInt(),
                        viewport.top.roundToInt()
                    )

                    ViewCompat.postInvalidateOnAnimation(this@LoupeImageView)

                    val toX = scroller.finalX.toFloat()
                    val toY = scroller.finalY.toFloat()

                    ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = ANIM_DURATION
                        interpolator = DecelerateInterpolator()
                        addUpdateListener {
                            val progress = it.animatedValue as Float
                            val newLeft = lerp(progress, fromX, toX)
                            val newTop = lerp(progress, fromY, toY)
                            bitmapBounds.offsetTo(newLeft, newTop)
                            if (progress == 1.0f) {
                                constrainBitmapBounds()
                            }
                            ViewCompat.postInvalidateOnAnimation(this@LoupeImageView)
                        }
                    }.start()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                e ?: return false
                var zoomFocalPointX = e.x
                var zoomFocalPointY = e.y
                val targetScale: Float

                if (scale > minBmScale) {
                    targetScale = minBmScale
                    zoomFocalPointX = canvasBounds.centerX()
                    zoomFocalPointY = canvasBounds.centerY()
                } else {
                    targetScale = minBmScale * maxZoom * 0.5f
                }

                ValueAnimator.ofFloat(scale, targetScale).apply {
                    duration = ANIM_DURATION
                    interpolator = DecelerateInterpolator()
                    addUpdateListener {
                        scale = it.animatedValue as Float
                        zoomTo(zoomFocalPointX, zoomFocalPointY)
                        ViewCompat.postInvalidateOnAnimation(this@LoupeImageView)
                    }
                }.start()
                return true
            }


        }

    private fun applyDismissEffect(distanceY: Float) {
        val view = this
        view.y = view.y - distanceY * 0.7f
        view.alpha = map(abs(view.y), 0f, dismissThreshold, 1.0f, 0.6f)
        val scale = map(abs(view.y), 0f, dismissThreshold, 1.0f, 0.95f)
        view.scaleX = scale
        view.scaleY = scale
    }

    private fun dismissOrRestoreIfNeeded() {
        if(y == 0f){
            return
        }
        dismissOrRestore()
    }

    private fun dismissOrRestore() {
        val view = this

        if (abs(y) > dismissThreshold) {
            Timber.e("over threshold")
            val translationY = if (y > 0) {
                originalViewBounds.top + view.height - view.top
            } else {
                originalViewBounds.top - view.height - view.top
            }
            isDismissing = true
            animate()
                .setDuration(ANIM_DURATION)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .alpha(0f)
                .translationY(translationY.toFloat())
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(p0: Animator?) {
                        // no op
                    }

                    override fun onAnimationEnd(p0: Animator?) {
                        isDismissing = false
                        onDismissListener?.onDismiss(this@LoupeImageView)
                    }

                    override fun onAnimationCancel(p0: Animator?) {
                        // no op
                    }

                    override fun onAnimationStart(p0: Animator?) {
                        // no op
                    }
                })
        } else {
            Timber.e("within threshold")
            animate()
                .setDuration(ANIM_DURATION)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .translationY((originalViewBounds.top - view.top).toFloat())
        }
    }

    private val scroller: OverScroller
    private var originalViewBounds = Rect()

    private var dismissThreshold = 0f

    var onDismissListener: OnDismissListener? = null
    var isDismissing = false

    init {
        scaleGestureDetector = ScaleGestureDetector(context, onScaleGestureListener)
        gestureDetector = GestureDetector(context, onGestureListener)
        scroller = OverScroller(context)
    }

    private fun zoomTo(focalX: Float, focalY: Float) {
        val oldBounds = RectF(bitmapBounds)
        // scale has changed, recalculate bitmap bounds
        calcBounds()
        // offset to focalPoint
        offsetToZoomFocalPoint(focalX, focalY, oldBounds, bitmapBounds)
        constrainBitmapBounds()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        isReadyToDraw = false
        super.setImageDrawable(drawable)
        updateLayout()
    }

    override fun setImageResource(resId: Int) {
//        Timber.e("setImageResource")
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
//        Timber.e("updateLayout")
        setupLayout()
    }

    override fun onDraw(canvas: Canvas?) {
        if (isReadyToDraw) {
            val bm = getBitmap()
            if (bm != null) {
                setTransform()
                canvas?.drawBitmap(bm, transfrom, bitmapPaint)
            }
        }
    }

    private fun setTransform() {
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
//        Timber.e("onSizeChanged")
        originalViewBounds.set(left, top, right, bottom)
        dismissThreshold = originalViewBounds.height() * DEFAULT_DISMISS_THRESHOLD_RATIO

        setupLayout()
    }

    /**
     * setup layout
     */
    private fun setupLayout() {
        val bm = getBitmap()
//        Timber.e("setupLayout start: width = $width, height = $height, bm = $bm")
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
        val offset = PointF()

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
    }

    private fun offsetBitmap(offsetX: Float, offsetY: Float) {
        bitmapBounds.offset(offsetX, offsetY)
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
        val canvasRatio = canvasHeight / canvasWidth
        val bitmapRatio = bitmapHeight / bitmapWidth
        minBmScale = if (canvasRatio > bitmapRatio) {
            canvasWidth / bitmapWidth
        } else {
            canvasHeight / bitmapHeight
        }
        scale = minBmScale
        maxBmScale = minBmScale * maxZoom
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        val result = super.dispatchTouchEvent(event)

        if (!isEnabled) {
            return result
        }

        if(isDismissing) {
            return true
        }

        val scaleEvent = scaleGestureDetector?.onTouchEvent(event)
        val isScaleAnimationIsRunning = scale < minBmScale
        if (scaleEvent != scaleGestureDetector?.isInProgress && !isScaleAnimationIsRunning) {
            // handle single touch gesture when scaling process is not running
            gestureDetector?.onTouchEvent(event)
        }

        if(event?.action == MotionEvent.ACTION_UP){
            dismissOrRestoreIfNeeded()
        }

        invalidate()
        return true
    }

    private fun calcNewScale(newScale: Float): Float {
        return min(maxBmScale, newScale * scale)
    }

    private fun constrain(min: Float, value: Float, max: Float): Float {
        return max(min(value, max), min)
    }

    private fun offsetToZoomFocalPoint(
        focalX: Float,
        focalY: Float,
        oldBounds: RectF,
        newBounds: RectF
    ) {
        val oldX = constrain(viewport.left, focalX, viewport.right)
        val oldY = constrain(viewport.top, focalY, viewport.bottom)
        val newX = map(oldX, oldBounds.left, oldBounds.right, newBounds.left, newBounds.right)
        val newY = map(oldY, oldBounds.top, oldBounds.bottom, newBounds.top, newBounds.bottom)
        offsetBitmap(oldX - newX, oldY - newY)
    }

    private fun map(
        value: Float,
        srcStart: Float,
        srcStop: Float,
        dstStart: Float,
        dstStop: Float
    ): Float {
        if (srcStop - srcStart == 0f) {
            return 0f
        }
        return ((value - srcStart) * (dstStop - dstStart) / (srcStop - srcStart)) + dstStart
    }

    private fun lerp(amt: Float, start: Float, stop: Float): Float {
        return start + (stop - start) * amt
    }

    private fun norm(value: Float, start: Float, stop: Float): Float {
        return value / (stop - start)
    }
}