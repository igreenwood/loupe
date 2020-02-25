package com.igreenwood.loupe

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class Loupe(var imageView: ImageView) : View.OnTouchListener, View.OnLayoutChangeListener {

    companion object {
        const val DEFAULT_MAX_ZOOM = 10.0f
        const val DEFAULT_ANIM_DURATION = 250L
        const val DEFAULT_VIEW_DRAG_FRICTION = 1f
        const val DEFAULT_DRAG_DISMISS_DISTANCE_IN_VIEW_HEIGHT_RATIO = 0.25f
        const val DEFAULT_FLING_DISMISS_ACTION_THRESHOLD_IN_DP = 96
        val DEFAULT_INTERPOLATOR = DecelerateInterpolator()
    }

    interface OnViewTranslateListener {
        fun onStart(view: ImageView)
        fun onViewTranslate(view: ImageView, amount: Float)
        fun onDismiss(view: ImageView)
        fun onRestore(view: ImageView)
    }

    // max zoom(> 1f)
    var maxZoom = DEFAULT_MAX_ZOOM
    // dismiss animation flag
    var useDismissAnimation = true
    // duration millis for dismiss animation
    var dismissAnimationDuration = DEFAULT_ANIM_DURATION
    // duration millis for restore animation
    var restoreAnimationDuration = DEFAULT_ANIM_DURATION
    // duration millis for image animation
    var flingAnimationDuration = DEFAULT_ANIM_DURATION
    // duration millis for double tap scaling animation
    var doubleTapScaleAnimationDuration = DEFAULT_ANIM_DURATION
    // duration millis for over scaling animation
    var overScaleAnimationDuration = DEFAULT_ANIM_DURATION
    // duration millis for over scrolling animation
    var overScrollAnimationDuration = DEFAULT_ANIM_DURATION
    // view drag friction for swipe to dismiss(1f : drag distance == view move distance. Smaller value, view is moving more slower)
    var viewDragFriction = DEFAULT_VIEW_DRAG_FRICTION
    // distance threshold for swipe to dismiss(If the view drag distance is bigger than threshold, view will be dismissed. Otherwise view position will be restored to initial position.)
    var dragDismissDistanceInViewHeightRatio = DEFAULT_DRAG_DISMISS_DISTANCE_IN_VIEW_HEIGHT_RATIO
    // fling threshold for dismiss action(If the user fling the view and the view drag distance is smaller than threshold, fling dismiss action will be triggered)
    var flingDismissActionThresholdInDp = DEFAULT_FLING_DISMISS_ACTION_THRESHOLD_IN_DP
    // dismiss action listener
    var onDismissListener: OnViewTranslateListener? = null

    var dismissAnimationInterpolator = DEFAULT_INTERPOLATOR

    var restoreAnimationInterpolator = DEFAULT_INTERPOLATOR

    var flingAnimationInterpolator = DEFAULT_INTERPOLATOR

    var doubleTapScaleAnimationInterpolator = DEFAULT_INTERPOLATOR

    var overScaleAnimationInterpolator = DEFAULT_INTERPOLATOR

    var overScrollAnimationInterpolator = DEFAULT_INTERPOLATOR

    // bitmap matrix
    private var transfrom = Matrix()
    // bitmap scale
    private var scale = 1f
    // is ready for drawing bitmap
    private var isReadyToDraw = false
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
    // bitmap (decoded) width
    private var imageWidth = 0f
    // bitmap (decoded) height
    private var imageHeight = 0f

    private val scroller: OverScroller
    private var originalViewBounds = Rect()
    private var dragDismissDistance = 0f
    private var flingDismissActionThreshold = 0f
    private var isDismissing = false
    private var isVerticalScrollEnabled = true
    private var isHorizontalScrollEnabled = true
    private var isFlinging = false
    private var isFlingDismissProcessRunning = false
    private var isAnimating = false
    private var initialY = 0f
    // scaling helper
    private var scaleGestureDetector: ScaleGestureDetector? = null
    // translating helper
    private var gestureDetector: GestureDetector? = null
    private val onScaleGestureListener: ScaleGestureDetector.OnScaleGestureListener =
        object : ScaleGestureDetector.OnScaleGestureListener {

            override fun onScale(detector: ScaleGestureDetector?): Boolean {
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
                    if (shouldTriggerFlingDismissAction()) {
                        processFlingDismissAction(velocityY)
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

    private fun processFlingDismissAction(velocityY: Float) {
        isFlingDismissProcessRunning = true

        if (useDismissAnimation) {
            startDismissWithFling(velocityY)
        } else {
            onDismissListener?.onDismiss(imageView)
        }
    }

    private fun shouldTriggerFlingDismissAction() = abs(viewOffsetY()) < flingDismissActionThreshold

    init {
        imageView.apply {
            setOnTouchListener(this@Loupe)
            addOnLayoutChangeListener(this@Loupe)
            scaleGestureDetector = ScaleGestureDetector(context, onScaleGestureListener)
            gestureDetector = GestureDetector(context, onGestureListener)
            scroller = OverScroller(context)
            flingDismissActionThreshold = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                flingDismissActionThresholdInDp.toFloat(),
                resources.displayMetrics
            )
            scaleType = ImageView.ScaleType.MATRIX
        }
    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        event ?: return false

        imageView.parent.requestDisallowInterceptTouchEvent(scale != minScale)

        imageView.run {
            if (!isEnabled) {
                return false
            }

            if (isDismissing) {
                return false
            }

            val scaleEvent = scaleGestureDetector?.onTouchEvent(event)
            val isScaleAnimationIsRunning = scale < minScale
            if (scaleEvent != scaleGestureDetector?.isInProgress && !isScaleAnimationIsRunning) {
                // handle single touch gesture when scaling process is not running
                gestureDetector?.onTouchEvent(event)
            }

            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    when {
                        scale == minScale -> {
                            if (!isFlingDismissProcessRunning) {
                                dismissOrRestoreIfNeeded()
                            }
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

            setTransform()
            postInvalidate()
        }
        return true
    }

    override fun onLayoutChange(
        view: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        imageView.run {
            setupLayout(left, top, right, bottom)
            initialY = y
            dragDismissDistance = height * dragDismissDistanceInViewHeightRatio
            setTransform()
            postInvalidate()
        }
    }

    private fun startDismissWithFling(velY: Float) {
        if (velY == 0f) {
            return
        }

        isDismissing = true

        imageView.run {
            val translationY = if (velY > 0) {
                originalViewBounds.top + height - top
            } else {
                originalViewBounds.top - height - top
            }
            animate()
                .setDuration(dismissAnimationDuration)
                .setInterpolator(dismissAnimationInterpolator)
                .translationY(translationY.toFloat())
                .setUpdateListener {
                    onDismissListener?.onViewTranslate(imageView, calcTranslationAmount())
                }
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(p0: Animator?) {

                    }

                    override fun onAnimationEnd(p0: Animator?) {
                        isDismissing = false
                        onDismissListener?.onDismiss(imageView)
                    }

                    override fun onAnimationCancel(p0: Animator?) {
                        isDismissing = false
                    }

                    override fun onAnimationRepeat(p0: Animator?) {
                        // no op
                    }
                })
        }
    }

    private fun processFling(velocityX: Float, velocityY: Float) {
        val (velX, velY) = velocityX to velocityY

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

        ViewCompat.postInvalidateOnAnimation(imageView)

        val toX = scroller.finalX.toFloat()
        val toY = scroller.finalY.toFloat()

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = flingAnimationDuration
            interpolator = flingAnimationInterpolator
            addUpdateListener {
                val amount = it.animatedValue as Float
                val newLeft = lerp(amount, fromX, toX)
                val newTop = lerp(amount, fromY, toY)
                bitmapBounds.offsetTo(newLeft, newTop)
                ViewCompat.postInvalidateOnAnimation(imageView)
                setTransform()
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
        setTransform()
    }

    private fun jumpToMediumScale(e: MotionEvent) {
        val startScale = scale
        val endScale = minScale * maxZoom * 0.5f
        val focalX = e.x
        val focalY = e.y
        ValueAnimator.ofFloat(startScale, endScale).apply {
            duration = doubleTapScaleAnimationDuration
            interpolator = doubleTapScaleAnimationInterpolator
            addUpdateListener {
                zoom(it.animatedValue as Float, focalX, focalY)
                ViewCompat.postInvalidateOnAnimation(imageView)
                setTransform()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {
                    isAnimating = true
                }

                override fun onAnimationEnd(p0: Animator?) {
                    isAnimating = false
                    if (endScale == minScale) {
                        zoom(minScale, focalX, focalY)
                        imageView.postInvalidate()
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

    private fun jumpToMinimumScale(isOverScaling: Boolean = false) {
        val startScale = scale
        val endScale = minScale
        val startLeft = bitmapBounds.left
        val startTop = bitmapBounds.top
        val endLeft = canvasBounds.centerX() - imageWidth * minScale * 0.5f
        val endTop = canvasBounds.centerY() - imageHeight * minScale * 0.5f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (isOverScaling) {
                overScaleAnimationDuration
            } else {
                doubleTapScaleAnimationDuration
            }
            interpolator = if(isOverScaling) {
                overScaleAnimationInterpolator
            } else {
                doubleTapScaleAnimationInterpolator
            }
            addUpdateListener {
                val value = it.animatedValue as Float
                scale = lerp(value, startScale, endScale)
                val newLeft = lerp(value, startLeft, endLeft)
                val newTop = lerp(value, startTop, endTop)
                calcBounds()
                bitmapBounds.offsetTo(newLeft, newTop)
                constrainBitmapBounds()
                ViewCompat.postInvalidateOnAnimation(imageView)
                setTransform()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {
                    isAnimating = true
                }

                override fun onAnimationEnd(p0: Animator?) {
                    isAnimating = false
                    if (endScale == minScale) {
                        scale = minScale
                        calcBounds()
                        constrainBitmapBounds()
                        imageView.postInvalidate()
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

    private var lastDistY = Float.NaN

    private fun processDrag(distanceY: Float) {
        if (lastDistY.isNaN()) {
            lastDistY = distanceY
            return
        }

        if (imageView.y == initialY) {
            onDismissListener?.onStart(imageView)
        }

        val distY = (lastDistY + distanceY) / 2f
        lastDistY = distanceY

        imageView.y -= distY * viewDragFriction // if viewDragRatio is 1.0f, view translation speed is equal to user scrolling speed.
        onDismissListener?.onViewTranslate(imageView, calcTranslationAmount())
    }

    private fun dismissOrRestoreIfNeeded() {
        if (!isDragging() || isDismissing) {
            return
        }
        dismissOrRestore()
    }

    private fun dismissOrRestore() {
        if (abs(viewOffsetY()) > dragDismissDistance) {
            if (useDismissAnimation) {
                startDismissWithDrag()
            } else {
                onDismissListener?.onDismiss(imageView)
            }
        } else {
            restoreViewTransform()
        }
    }

    private fun restoreViewTransform() {
        imageView.run {
            animate()
                .setDuration(restoreAnimationDuration)
                .setInterpolator(restoreAnimationInterpolator)
                .translationY((originalViewBounds.top - top).toFloat())
                .setUpdateListener {
                    onDismissListener?.onViewTranslate(this, calcTranslationAmount())
                }
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(p0: Animator?) {
                        // no op
                    }

                    override fun onAnimationEnd(p0: Animator?) {
                        onDismissListener?.onRestore(imageView)
                    }

                    override fun onAnimationCancel(p0: Animator?) {
                        // no op
                    }

                    override fun onAnimationRepeat(p0: Animator?) {
                        // no op
                    }
                })
        }
    }

    private fun startDismissWithDrag() {
        imageView.run {
            val translationY = if (y - initialY > 0) {
                originalViewBounds.top + height - top
            } else {
                originalViewBounds.top - height - top
            }
            animate()
                .setDuration(dismissAnimationDuration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .translationY(translationY.toFloat())
                .setUpdateListener {
                    onDismissListener?.onViewTranslate(this, calcTranslationAmount())
                }
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(p0: Animator?) {
                        isDismissing = true
                    }

                    override fun onAnimationEnd(p0: Animator?) {
                        isDismissing = false
                        onDismissListener?.onDismiss(imageView)
                    }

                    override fun onAnimationCancel(p0: Animator?) {
                        isDismissing = false
                    }

                    override fun onAnimationRepeat(p0: Animator?) {
                        // no op
                    }
                })
        }
    }

    private fun calcTranslationAmount() =
        constrain(
            0f,
            norm(abs(viewOffsetY()), 0f, originalViewBounds.height().toFloat()),
            1f
        )

    private fun isDragging() = viewOffsetY() != 0f

    private fun viewOffsetY() = imageView.y - initialY

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

    private fun setTransform() {
        transfrom.apply {
            reset()
            postTranslate(-imageWidth / 2, -imageHeight / 2)
            postScale(scale, scale)
            postTranslate(bitmapBounds.centerX(), bitmapBounds.centerY())
        }
        imageView.imageMatrix = transfrom
    }

    private fun getBitmap(): Bitmap? {
        return (imageView.drawable as? BitmapDrawable)?.bitmap
    }

    /**
     * setup layout
     */
    private fun setupLayout(left: Int, top: Int, right: Int, bottom: Int) {
        originalViewBounds.set(left, top, right, bottom)
        imageView.run {
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
                duration = overScrollAnimationDuration
                interpolator = overScrollAnimationInterpolator
                addUpdateListener {
                    val amount = it.animatedValue as Float
                    val newLeft = lerp(amount, start.left, end.left)
                    val newTop = lerp(amount, start.top, end.top)
                    bitmapBounds.offsetTo(newLeft, newTop)
                    ViewCompat.postInvalidateOnAnimation(imageView)
                    setTransform()
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
        imageView.run {
            // calc canvas bounds
            canvasBounds = RectF(
                paddingLeft.toFloat(),
                paddingTop.toFloat(),
                width - paddingRight.toFloat(),
                height - paddingBottom.toFloat()
            )
        }
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