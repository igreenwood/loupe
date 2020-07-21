package com.igreenwood.loupe.extensions

import android.view.ViewGroup
import android.widget.ImageView
import com.igreenwood.loupe.Loupe

fun createLoupe(
    imageView: ImageView,
    container: ViewGroup,
    config: Loupe.() -> Unit = { }
): Loupe {
    return Loupe.create(imageView, container, config)
}

fun Loupe.setOnScaleChangedListener(onScaleChangedListener: (scaleFactor: Float, focusX: Float, focusY: Float) -> Unit) {
    this.onScaleChangedListener = object : Loupe.OnScaleChangedListener {
        override fun onScaleChange(scaleFactor: Float, focusX: Float, focusY: Float) {
            onScaleChangedListener(scaleFactor, focusX, focusY)
        }
    }
}

fun Loupe.setOnViewTranslateListener(
    onStart: (view: ImageView) -> Unit = {},
    onViewTranslate: (view: ImageView, amount: Float) -> Unit = { _, _ -> },
    onRestore: (view: ImageView) -> Unit = {},
    onDismiss: (view: ImageView) -> Unit = {}
) {
    this.onViewTranslateListener = object : Loupe.OnViewTranslateListener {
        override fun onDismiss(view: ImageView) {
            onDismiss(view)
        }

        override fun onRestore(view: ImageView) {
            onRestore(view)
        }

        override fun onStart(view: ImageView) {
            onStart(view)
        }

        override fun onViewTranslate(view: ImageView, amount: Float) {
            onViewTranslate(view, amount)
        }
    }
}