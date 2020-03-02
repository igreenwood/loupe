[logo](logo-url)

ImageView Helper for Android that supports zooming and swipe-to-dismiss action.
Source code is written with full Kotlin. Supported on API Level 21 and above.

[preview](preview-url)

loupe provides modern image viewer ui with simple code.

You can add Twitter-like image viewer that supports swipe-to-dismiss action.

In your Activity, add the following code. (It is also working with Fragments.)
```kotlin
val imageView: ImageView // normal ImageView
val loupe = Loupe(imageView).apply {
  onDismissListener = object : Loupe.OnViewTranslateListener {

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

[zooming-preview](zooming-preview-url)
[dismissing-preview](dismissing-preview-url)

## Download
```groovy
dependencies {
  implementation 'com.igreenwood:loupe:0.3.0'
}
```

## OnViewTranslateListener
If you want to do some action while dimissing ImageView, use `OnViewTranslateListener`.

```kotlin
val loupe = Loupe(imageView).apply {
  onDismissListener = object : Loupe.OnViewTranslateListener {

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

## Using with some image loader libraries.
loupe is just a touch helper of the ImageView.
You can use any image loader libraries.
If you use with some libraries, it would be better that initialize `Loupe` after the image loading has finished.
with Glide, something like this.

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

            onDismissListener = object : Loupe.OnViewTranslateListener {

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

## Two type dismiss action

- Fling dismiss animation
The default dismiss animation is vertical fling animation.

[fling-dismiss-animation](fling-dismiss-animation-url)

- Use SharedElemtns Transition
You can use Shared Elements Transition.

[shared-elements-transition-animatijon](shared-elements-transition-animation-url)

If you use Shared Elements Transition, set `useDismissAnimation` to `false`.

```kotlin
val loupe = Loupe(imageView).apply {
  useDismissAnimation = false
  onDismissListener = object : Loupe.OnViewTranslateListener {

    override fun onStart(view: ImageView) {}

    override fun onViewTranslate(view: ImageView, amount: Float) {}

    override fun onRestore(view: ImageView) {}

    override fun onDismiss(view: ImageView) {}
  }
}
```

## Customization

Here is the customizable parameters.

### Customizable parameters
```kotlin
Loupe(image).apply {
  useDismissAnimation = !Pref.useSharedElements
  maxZoom = Pref.maxZoom
  flingAnimationDuration = Pref.flingAnimationDuration
  scaleAnimationDuration = Pref.scaleAnimationDuration
  overScaleAnimationDuration = Pref.overScaleAnimationDuration
  overScrollAnimationDuration = Pref.overScrollAnimationDuration
  dismissAnimationDuration = Pref.dismissAnimationDuration
  restoreAnimationDuration = Pref.restoreAnimationDuration
  viewDragFriction = Pref.viewDragFriction

  onDismissListener = object : Loupe.OnViewTranslateListener {

    override fun onStart(view: ImageView) {}

    override fun onViewTranslate(view: ImageView, amount: Float) {}

    override fun onRestore(view: ImageView) {}

    override fun onDismiss(view: ImageView) {}
  }
}
```
You can try parameters with [the sample program](https://github.com/igreenwood/loupe/tree/master/loupe-sample).

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
