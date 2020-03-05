# Loupe [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Loupe-lightgrey.svg?style=flat)](https://android-arsenal.com/details/1/8047) [![](https://jitpack.io/v/igreenwood/loupe.svg)](https://jitpack.io/#igreenwood/loupe) [![CircleCI](https://circleci.com/gh/igreenwood/loupe/tree/master.svg?style=svg)](https://circleci.com/gh/igreenwood/loupe/tree/master)

Loupe is an ImageView Helper for Android that supports zooming and swipe-to-dismiss action.

<img src="art/logo.png" width="300">

Loupe provides modern image viewer ui with super simple code.
You can implement the Twitter-like image viewer in 10 minutes.

<img src="art/preview-zooming.gif" width="260"><img src="art/preview-dismiss-animation.gif" width="260"><img src="art/preview-shared-elements.gif" width="260">

## Download
Loupe is available on `jCenter()`
```groovy
dependencies {
  implementation 'com.igreenwood:loupe:LATEST_VERSION'
}
```
`LATEST_VERSION` is [![](https://jitpack.io/v/igreenwood/loupe.svg)](https://jitpack.io/#igreenwood/loupe).

## Quick Start

In your Activity, add the following code. (Loupe is also working with Fragments.)
```kotlin
val loupe = Loupe(imageView).apply { // imageView is normal ImageView
  onViewTranslateListener = object : Loupe.OnViewTranslateListener {

    override fun onStart(view: ImageView) {
      
    }

    override fun onViewTranslate(view: ImageView, amount: Float) {
      
    }

    override fun onRestore(view: ImageView) {
      
    }

    override fun onDismiss(view: ImageView) {
      finish()
    }
  }
}
```
That's all. Now your ImageView supports zooming and swipe-to-dismiss action :smile:

## Dismiss Animation

In default, Loupe uses vertical translate animation on dismissing the ImageView.

If you use Shared Elements Transition, set `useDismissAnimation` to `false`.

```kotlin
val loupe = Loupe(imageView).apply {
  useDismissAnimation = false
  onViewTranslateListener = object : Loupe.OnViewTranslateListener {

    override fun onStart(view: ImageView) {}

    override fun onViewTranslate(view: ImageView, amount: Float) {}

    override fun onRestore(view: ImageView) {}

    override fun onDismiss(view: ImageView) {}
  }
}
```

Vertical Translate Animation | Shared Elements Transition
:-- | :--
<img src="art/dismiss-animation.gif" width="260"> | <img src="art/shared-elements-transition.gif" width="260">

## OnViewTranslateListener
If you want to do some action while dimissing ImageView, use `OnViewTranslateListener`.

```kotlin
val loupe = Loupe(imageView).apply {
  onViewTranslateListener = object : Loupe.OnViewTranslateListener {

    override fun onStart(view: ImageView) {
      // called when the user start swiping down/up the view.
      hideToolbar()
    }

    override fun onViewTranslate(view: ImageView, amount: Float) {
      // called when every time the view y position is updated.
      changeBackgroundAlpha(amount)
    }

    override fun onRestore(view: ImageView) {
      // called when user cancelled the swiping.
      showToolbar()
    }

    override fun onDismiss(view: ImageView) {
      // called when the view translating animation has ended.
      finish()
    }
  }
}
```
For more details, see [the sample program](https://github.com/igreenwood/loupe/tree/master/loupe-sample).

## Using Loupe with image loader libraries
Loupe is just a helper of the ImageView.
You can use Loupe with any image loader libraries.
If you use with the image loader library, it would be better that initialize `Loupe` after the image loading has finished.
With Glide, something like this.

```kotlin
Glide.with(imageView.context).load(url)
  .listener(object : RequestListener<Drawable> {
      override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<Drawable>?,
        isFirstResource: Boolean
      ): Boolean {
        return false
      }

      override fun onResourceReady(
        resource: Drawable?,
        model: Any?,
        target: Target<Drawable>?,
        dataSource: DataSource?,
        isFirstResource: Boolean
      ): Boolean {
        val loupe = Loupe(image).apply {

            onViewTranslateListener = object : Loupe.OnViewTranslateListener {

            override fun onStart(view: ImageView) {}

            override fun onViewTranslate(view: ImageView, amount: Float) {}

            override fun onRestore(view: ImageView) {}

            override fun onDismiss(view: ImageView) {}
        }
      return false
    }
  }
).into(imageView)
```

## Customization
Here is the customizable parameters.

### Customizable parameters
```kotlin
Loupe(image).apply {
  useDismissAnimation = true // If you use shared elements transition, set false
  maxZoom = 8f
  dismissAnimationDuration = 250L // duration millis for dismiss animation
  restoreAnimationDuration = 250L // duration millis for restore animation
  flingAnimationDuration = 250L // duration millis for image fling animation
  scaleAnimationDuration = 250L // duration millis for double tap scale animation
  overScaleAnimationDuration = 250L // duration millis for over scale animation
  overScrollAnimationDuration = 250L // duration millis for over scrolling animation
  viewDragFriction = 1f // view drag friction for swipe to dismiss(1f : drag distance == view move distance. Smaller value, view is moving more slower)
  dragDismissDistanceInViewHeightRatio = 0.3f // distance threshold for swipe to dismiss(If the view drag distance is bigger than threshold, view will be dismissed. Otherwise view position will be restored to initial position.)
  flingDismissActionThresholdInDp = 48 // fling threshold for dismiss action(If the user fling the view and the view drag distance is smaller than threshold, fling dismiss action will be triggered)
  dismissAnimationInterpolator = DecelerateInterpolator() // animationn interpolator
  restoreAnimationInterpolator = DecelerateInterpolator() // animationn interpolator
  flingAnimationInterpolator = DecelerateInterpolator() // animationn interpolator
  doubleTapScaleAnimationInterpolator = DecelerateInterpolator() // animationn interpolator
  overScaleAnimationInterpolator = DecelerateInterpolator() // animationn interpolator
  overScrollAnimationInterpolator = DecelerateInterpolator() // animationn interpolator

  onViewTranslateListener = object : Loupe.OnViewTranslateListener {

    override fun onStart(view: ImageView) {}

    override fun onViewTranslate(view: ImageView, amount: Float) {}

    override fun onRestore(view: ImageView) {}

    override fun onDismiss(view: ImageView) {}
  }
}
```
You can try parameters with [the sample program](https://github.com/igreenwood/loupe/tree/master/loupe-sample).

<img src="art/setting-view.png" width="260">

## Requirements
Supported on API Level 21 and above.

## Proguard
```
-dontwarn com.igreenwood.loupe**
-keep class com.igreenwood.loupe** { *; }
-keep interface com.igreenwood.loupe** { *; }
```

## Developed By
Issei Aoki - <i.greenwood.dev@gmail.com>

## Apps using loupe
If you are using my library, please let me know your app name :smile:

## License
```
The MIT License (MIT)

Copyright (c) 2020 Issei Aoki

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
