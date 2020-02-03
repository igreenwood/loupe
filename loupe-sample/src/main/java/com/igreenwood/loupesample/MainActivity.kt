package com.igreenwood.loupesample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.igreenwood.loupe.LoupeImageView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageView = findViewById<LoupeImageView>(R.id.image)
        imageView.setImageResource(R.drawable.sample4)
        imageView.onDismissListener = object : LoupeImageView.OnViewTranslateListener {
            override fun onViewTranslate(view: LoupeImageView, progress: Float) {

            }

            override fun onRestore(view: LoupeImageView) {

            }

            override fun onDismiss(view: LoupeImageView) {
                finish()
            }
        }

//        Glide.with(this).load("https://raw.githubusercontent.com/igreenwood/SimpleCropView/master/simplecropview-sample/src/main/res/drawable/sample4.png").into(imageView)
    }
}
