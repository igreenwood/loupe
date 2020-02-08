package com.igreenwood.loupesample

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout


class RoundedCornerFrameLayout
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    FrameLayout(context, attrs, defStyle) {

    private var cornerRadius: Float = 0f
    private var path: Path = Path()
    private lateinit var rectF: RectF

    init {
        if (!isInEditMode) {
            attrs?.let {
                val ta = context.theme.obtainStyledAttributes(
                    it,
                    R.styleable.RoundedCornerFrameLayout,
                    0,
                    0
                )
                try {
                    cornerRadius =
                        ta.getDimension(R.styleable.RoundedCornerFrameLayout_cornerRadius, 4f)
                } finally {
                    ta.recycle()
                }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rectF = RectF(0f, 0f, w.toFloat(), h.toFloat())
        resetPath()
    }

    override fun draw(canvas: Canvas) {
        val save = canvas.save()
        canvas.clipPath(path)
        super.draw(canvas)
        canvas.restoreToCount(save)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val save = canvas.save()
        canvas.clipPath(path)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(save)
    }

    private fun resetPath() {
        path.reset()
        path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)
        path.close()
    }
}