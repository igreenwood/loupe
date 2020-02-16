package com.igreenwood.loupe

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
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
        const val DEFAULT_VIEW_DRAG_RATIO = 1f
        const val DEFAULT_DISMISS_THRESHOLD_RATIO = 0.25f
        const val DEFAULT_DISMISS_WITH_FLING_THRESHOLD_DP = 96
    }

    interface OnViewTranslateListener {
        fun onStart(view: LoupeImageView)
        fun onViewTranslate(view: LoupeImageView, amount: Float)
        fun onDismiss(view: LoupeImageView)
        fun onRestore(view: LoupeImageView)
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
    private var minScale = 1f
    // maximum scale of bitmap
    private var maxScale = 1f
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
                Timber.e("onScale: isDragging = ${isDragging()}, isFlinging = $isFlinging, $isAnimating")
                if (isDragging() || isFlinging || isAnimating) {
                    return true
                }

                val scaleFactor = detector?.scaleFactor ?: 1.0f
                val focalX = detector?.focusX ?: bitmapBounds.centerX()
                val focalY = detector?.focusY ?: bitmapBounds.centerY()

                if (detector?.scaleFactor == 1.0f) {
                    // scale is not changing
                    return true
                }

                zoom(calcNewScale(scaleFactor), focalX, focalY)

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
                Timber.e("onScroll: distanceX = $distanceX, distanceY = $distanceY")

                if (scale > minScale) {
                    processScroll(distanceX, distanceY)
                } else if (scale == minScale) {
                    processDrag(distanceY)
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

                if (scale > minScale) {
                    processFling(velocityX, velocityY)
                } else {
                    if(abs(y - initialY) < dismissWithFlingThreshold){
                        startDismissWithFling(velocityY)
                    }
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                e ?: return false

                if (isAnimating) {
                    return true
                }

                if (scale > minScale) {
                    jumpToMinimumScale()
                } else {
                    jumpToMediumScale(e)
                }
                return true
            }


        }

    private fun startDismissWithFling(velY: Float) {
        if (velY == 0f) {
            return
        }

        isDismissing = true

        val view = this@LoupeImageView

        val translateY = if (velY > 0) {
            originalViewBounds.top + view.height - view.top
        } else {
            originalViewBounds.top - view.height - view.top
        }
        animate()
            .setDuration(ANIM_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .translationY(translateY.toFloat())
            .setUpdateListener {
                onDismissListener?.onViewTranslate(this@LoupeImageView, calcTranslationAmount())
            }
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {

                }

                override fun onAnimationEnd(p0: Animator?) {
                    isDismissing = false
                    onDismissListener?.onDismiss(this@LoupeImageView)
                }

                override fun onAnimationCancel(p0: Animator?) {
                    isDismissing = false
                }

                override fun onAnimationRepeat(p0: Animator?) {
                    // no op
                }
            })
    }

    private val scroller: OverScroller
    private var originalViewBounds = Rect()

    private var dismissWithDragThreshold = 0f
    private var dismissWithFlingThreshold = 0f

    var onDismissListener: OnViewTranslateListener? = null
    var isDismissing = false
    private var viewDragRatio = DEFAULT_VIEW_DRAG_RATIO
    private var isVerticalScrollEnabled = true
    private var isHorizontalScrollEnabled = true
    private var isFlinging = false
    private var isAnimating = false
    private var initialY = 0f
    var useDismissAnimation = true

    init {
        scaleGestureDetector = ScaleGestureDetector(context, onScaleGestureListener)
        gestureDetector = GestureDetector(context, onGestureListener)
        scroller = OverScroller(context)
        dismissWithFlingThreshold = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            DEFAULT_DISMISS_WITH_FLING_THRESHOLD_DP.toFloat(),
            resources.displayMetrics
        )
        scaleType = ScaleType.MATRIX
    }

    private fun processFling(velocityX: Float, velocityY: Float) {
        val (velX, velY) = velocityX to velocityY

        Timber.e("vel = ($velX, $velY)")

        if (velX == 0f && velY == 0f) {
            return
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
                val amount = it.animatedValue as Float
                val newLeft = lerp(amount, fromX, toX)
                val newTop = lerp(amount, fromY, toY)
                bitmapBounds.offsetTo(newLeft, newTop)
                ViewCompat.postInvalidateOnAnimation(this@LoupeImageView)
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {
                    isFlinging = true
                }

                override fun onAnimationEnd(p0: Animator?) {
                    isFlinging = false
                    constrainBitmapBounds()
                }

                override fun onAnimationCancel(p0: Animator?) {
                    isFlinging = false
                }

                override fun onAnimationRepeat(p0: Animator?) {
                    // no op
                }
            })
        }.start()
    }

    private fun processScroll(distanceX: Float, distanceY: Float) {
        val distX = if (isHorizontalScrollEnabled) {
            -distanceX
        } else {
            0f
        }
        val distY = if (isVerticalScrollEnabled) {
            -distanceY
        } else {
            0f
        }
        offsetBitmap(distX, distY)
    }

    private fun jumpToMediumScale(e: MotionEvent) {
        val startScale = scale
        val endScale = minScale * maxZoom * 0.5f
        val focalX = e.x
        val focalY = e.y
        ValueAnimator.ofFloat(startScale, endScale).apply {
            duration = ANIM_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                zoom(it.animatedValue as Float, focalX, focalY)
                ViewCompat.postInvalidateOnAnimation(this@LoupeImageView)
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {
                    isAnimating = true
                }

                override fun onAnimationEnd(p0: Animator?) {
                    isAnimating = false
                    if (endScale == minScale) {
                        zoom(minScale, focalX, focalY)
                        postInvalidate()
                    }
                }

                override fun onAnimationCancel(p0: Animator?) {
                    isAnimating = false
                }

                override fun onAnimationRepeat(p0: Animator?) {
                    // no op
                }
            })
        }.start()
    }

    private fun jumpToMinimumScale() {
        val startScale = scale
        val endScale = minScale
        val startLeft = bitmapBounds.left
        val startTop = bitmapBounds.top
        val endLeft = canvasBounds.centerX() - imageWidth * minScale * 0.5f
        val endTop = canvasBounds.centerY() - imageHeight * minScale * 0.5f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIM_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                scale = lerp(value, startScale, endScale)
                val newLeft = lerp(value, startLeft, endLeft)
                val newTop = lerp(value, startTop, endTop)
                calcBounds()
                bitmapBounds.offsetTo(newLeft, newTop)
                constrainBitmapBounds()
                ViewCompat.postInvalidateOnAnimation(this@LoupeImageView)
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {
                    Timber.e("animationStart: scale = $scale, targetScale = $endScale")
                    isAnimating = true
                }

                override fun onAnimationEnd(p0: Animator?) {
                    isAnimating = false
                    if (endScale == minScale) {
                        scale = minScale
                        calcBounds()
                        constrainBitmapBounds()
                        postInvalidate()
                    }
                }

                override fun onAnimationCancel(p0: Animator?) {
                    isAnimating = false
                }

                override fun onAnimationRepeat(p0: Animator?) {
                    // no op
                }
            })
        }.start()
    }

    private var lastDistY = 0f

    private fun processDrag(distanceY: Float) {
        if (y == initialY) {
            onDismissListener?.onStart(this)
        }
        Timber.e("processDrag: y = $y, distanceY = $distanceY")
        val distY = (lastDistY + distanceY) / 2f
        lastDistY = distanceY

        y -= distY * viewDragRatio // if viewDragRatio is 1.0f, view translation speed is equal to user scrolling speed.
        Timber.e("dist = ${abs(y - initialY)}, viewHeight = ${originalViewBounds.height()}")
        onDismissListener?.onViewTranslate(this, calcTranslationAmount())
    }

    private fun dismissOrRestoreIfNeeded() {
        if (!isDragging() || isDismissing) {
            return
        }
        Timber.e("dismissOrRestore: isDragging = ${isDragging()}, isDismissing = ${isDismissing}")
        dismissOrRestore()
    }

    private fun dismissOrRestore() {
        if (abs(y - initialY) > dismissWithDragThreshold) {
            if (useDismissAnimation) {
                startDismissWithDrag()
            } else {
                onDismissListener?.onDismiss(this)
            }
        } else {
            restoreViewTransform()
        }
    }

    private fun restoreViewTransform() {
        val view = this
        animate()
            .setDuration(ANIM_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .translationY((originalViewBounds.top - view.top).toFloat())
            .setUpdateListener {
                onDismissListener?.onViewTranslate(this, calcTranslationAmount())
            }
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {
                    // no op
                }

                override fun onAnimationEnd(p0: Animator?) {
                    onDismissListener?.onRestore(this@LoupeImageView)
                }

                override fun onAnimationCancel(p0: Animator?) {
                    // no op
                }

                override fun onAnimationRepeat(p0: Animator?) {
                    // no op
                }
            })
    }

    private fun startDismissWithDrag() {
        val view = this
        val translateY = if (y - initialY > 0) {
            originalViewBounds.top + view.height - view.top
        } else {
            originalViewBounds.top - view.height - view.top
        }
        animate()
            .setDuration(ANIM_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .translationY(translateY.toFloat())
            .setUpdateListener {
                onDismissListener?.onViewTranslate(this, calcTranslationAmount())
            }
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {
                    isDismissing = true
                }

                override fun onAnimationEnd(p0: Animator?) {
                    isDismissing = false
                    onDismissListener?.onDismiss(this@LoupeImageView)
                }

                override fun onAnimationCancel(p0: Animator?) {
                    isDismissing = false
                }

                override fun onAnimationRepeat(p0: Animator?) {
                    // no op
                }
            })
    }

    private fun calcTranslationAmount() =
        constrain(0f, norm(abs(y - initialY), 0f, originalViewBounds.height().toFloat()), 1f)

    private fun isDragging() = (y - initialY) != 0f

    /**
     * targetScale: new scale
     * focalX: focal x in current bitmapBounds
     * focalY: focal y in current bitmapBounds
     */
    private fun zoom(targetScale: Float, focalX: Float, focalY: Float) {
        scale = targetScale
        val lastBounds = RectF(bitmapBounds)
        // scale has changed, recalculate bitmap bounds
        calcBounds()
        // offset to focalPoint
        offsetToZoomFocalPoint(focalX, focalY, lastBounds, bitmapBounds)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        isReadyToDraw = false
        super.setImageDrawable(drawable)
        setupLayout()
    }

    override fun setImageResource(resId: Int) {
        isReadyToDraw = false
        super.setImageResource(resId)
        setupLayout()
    }

    override fun setImageURI(uri: Uri?) {
        isReadyToDraw = false
        super.setImageURI(uri)
        setupLayout()
    }

    override fun onDraw(canvas: Canvas?) {
        if (isReadyToDraw) {
            val bm = getBitmap()
            if (bm != null) {
                setTransform()
                super.onDraw(canvas)
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
        imageMatrix = transfrom
    }

    private fun getBitmap(): Bitmap? {
        return (drawable as? BitmapDrawable)?.bitmap
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupLayout()
        initialY = y
        dismissWithDragThreshold = height * DEFAULT_DISMISS_THRESHOLD_RATIO
    }

    /**
     * setup layout
     */
    private fun setupLayout() {
        originalViewBounds.set(left, top, right, bottom)
        val bm = getBitmap()
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

    private fun constrainBitmapBounds(animate: Boolean = false) {
        if (isFlinging || isAnimating) {
            return
        }

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

        if (offset.equals(0f, 0f)) {
            return
        }

        if (animate) {
            if (!isVerticalScrollEnabled) {
                bitmapBounds.offset(0f, offset.y)
                offset.y = 0f
            }

            if (!isHorizontalScrollEnabled) {
                bitmapBounds.offset(offset.x, 0f)
                offset.x = 0f
            }

            val start = RectF(bitmapBounds)
            val end = RectF(bitmapBounds).apply {
                offset(offset.x, offset.y)
            }
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = ANIM_DURATION
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    val amount = it.animatedValue as Float
                    val newLeft = lerp(amount, start.left, end.left)
                    val newTop = lerp(amount, start.top, end.top)
                    bitmapBounds.offsetTo(newLeft, newTop)
                    ViewCompat.postInvalidateOnAnimation(this@LoupeImageView)
                }
            }.start()
        } else {
            bitmapBounds.offset(offset.x, offset.y)
        }
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
        // check scroll availability
        isHorizontalScrollEnabled = true
        isVerticalScrollEnabled = true

        if (bitmapBounds.width() < canvasBounds.width()) {
            isHorizontalScrollEnabled = false
        }

        if (bitmapBounds.height() < canvasBounds.height()) {
            isVerticalScrollEnabled = false
        }
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
        minScale = if (canvasRatio > bitmapRatio) {
            canvasWidth / bitmapWidth
        } else {
            canvasHeight / bitmapHeight
        }
        scale = minScale
        maxScale = minScale * maxZoom
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val result = super.onTouchEvent(event)

        event ?: return result

        if (!isEnabled) {
            return result
        }

        if (isDismissing) {
            return true
        }

        val scaleEvent = scaleGestureDetector?.onTouchEvent(event)
        val isScaleAnimationIsRunning = scale < minScale
        if (scaleEvent != scaleGestureDetector?.isInProgress && !isScaleAnimationIsRunning) {
            // handle single touch gesture when scaling process is not running
            gestureDetector?.onTouchEvent(event)
        }

        when(event.action){
            MotionEvent.ACTION_UP -> {
                when {
                    scale == minScale -> {
                        dismissOrRestoreIfNeeded()
                    }
                    scale > minScale -> {
                        constrainBitmapBounds(true)
                    }
                    else -> {
                        jumpToMinimumScale()
                    }
                }
            }
        }

        postInvalidate()
        return true
    }

    private fun calcNewScale(newScale: Float): Float {
        return min(maxScale, newScale * scale)
    }

    private fun constrain(min: Float, value: Float, max: Float): Float {
        return max(min(value, max), min)
    }

    private fun offsetToZoomFocalPoint(
        focalX: Float,
        focalY: Float,
        oldBounds: RectF,
        newBounds: RectF
    ): PointF {
        val oldX = constrain(viewport.left, focalX, viewport.right)
        val oldY = constrain(viewport.top, focalY, viewport.bottom)
        val newX = map(oldX, oldBounds.left, oldBounds.right, newBounds.left, newBounds.right)
        val newY = map(oldY, oldBounds.top, oldBounds.bottom, newBounds.top, newBounds.bottom)
        offsetBitmap(oldX - newX, oldY - newY)
        return PointF(newX, newY)
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