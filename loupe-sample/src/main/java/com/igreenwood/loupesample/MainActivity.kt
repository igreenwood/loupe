package com.igreenwood.loupesample

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.igreenwood.loupesample.databinding.ActivityMainBinding
import com.igreenwood.loupesample.master.MasterActivity
import com.igreenwood.loupesample.util.Pref
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initToolbar()
        initView()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.start -> {
                goToMaster()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun goToMaster() {
        startActivity(Intent(this, MasterActivity::class.java))
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setLogo(R.drawable.logo)
            title = "loupe"
        }
    }

    private fun initView() {
        binding.useSharedElementsSwitch.apply {
            isChecked = Pref.useSharedElements
            setOnCheckedChangeListener { _, checked ->
                Pref.useSharedElements = checked
                binding.dismissAnimationSettings.visibility = if(checked){
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
        }
        val progressMin = 0f
        val progressMax = 100f
        val zoomMin = 1.5f
        val zoomMax = 8f
        binding.maxZoomSeekBar.apply {
            progress = map(Pref.maxZoom, zoomMin, zoomMax, progressMin, progressMax).roundToInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                    val value = map(progress.toFloat(), progressMin, progressMax, zoomMin, zoomMax)
                    binding.maxZoomTextView.text = getString(R.string.max_zoom, value)
                    Pref.maxZoom = value
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
        binding.maxZoomTextView.text = getString(R.string.max_zoom, Pref.maxZoom)
        val durationMin = 100f
        val durationMax = 3000f
        binding.flingDurationSeekBar.apply {
            progress =
                map(Pref.flingAnimationDuration.toFloat(), durationMin, durationMax, progressMin, progressMax).roundToInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                    val value = map(progress.toFloat(), progressMin, progressMax, durationMin, durationMax).toLong()
                    binding.flingDurationTextView.text =
                        getString(R.string.fling_animation_duration, value)
                    Pref.flingAnimationDuration = value
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
        binding.flingDurationTextView.text = getString(R.string.fling_animation_duration, Pref.flingAnimationDuration)
        binding.scaleDurationSeekBar.apply {
            progress =
                map(Pref.scaleAnimationDuration.toFloat(), durationMin, durationMax, progressMin, progressMax).roundToInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                    val value = map(progress.toFloat(), progressMin, progressMax, durationMin, durationMax).toLong()
                    binding.scaleDurationTextView.text =
                        getString(R.string.scale_animation_duration, value)
                    Pref.scaleAnimationDuration = value
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
        binding.scaleDurationTextView.text = getString(R.string.scale_animation_duration, Pref.scaleAnimationDuration)
        binding.overScaleSeekBar.apply {
            progress =
                map(Pref.overScaleAnimationDuration.toFloat(), durationMin, durationMax, progressMin, progressMax).roundToInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                    val value = map(progress.toFloat(), progressMin, progressMax, durationMin, durationMax).toLong()
                    binding.overScaleDurationTextView.text =
                        getString(R.string.over_scale_animation_duration, value)
                    Pref.overScaleAnimationDuration = value
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
        binding.overScaleDurationTextView.text = getString(R.string.over_scale_animation_duration, Pref.overScaleAnimationDuration)
        binding.overScrollDurationSeekBar.apply {
            progress =
                map(Pref.overScrollAnimationDuration.toFloat(), durationMin, durationMax, progressMin, progressMax).roundToInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                    val value = map(progress.toFloat(), progressMin, progressMax, durationMin, durationMax).toLong()
                    binding.overScrollDurationTextView.text =
                        getString(R.string.over_scroll_animation_duration, value)
                    Pref.overScrollAnimationDuration = value
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
        binding.overScrollDurationTextView.text = getString(R.string.over_scroll_animation_duration, Pref.overScrollAnimationDuration)
        binding.dismissDurationSeekBar.apply {
            progress =
                map(Pref.dismissAnimationDuration.toFloat(), durationMin, durationMax, progressMin, progressMax).roundToInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                    val value = map(progress.toFloat(), progressMin, progressMax, durationMin, durationMax).toLong()
                    binding.dismissDurationTextView.text =
                        getString(R.string.dismiss_animation_duration, value)
                    Pref.dismissAnimationDuration = value
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
        binding.dismissDurationTextView.text = getString(R.string.dismiss_animation_duration, Pref.dismissAnimationDuration)
        binding.restoreDurationSeekBar.apply {
            progress =
                map(Pref.restoreAnimationDuration.toFloat(), durationMin, durationMax, progressMin, progressMax).roundToInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                    val value = map(progress.toFloat(), progressMin, progressMax, durationMin, durationMax).toLong()
                    binding.restoreDurationTextView.text =
                        getString(R.string.restore_animation_duration, value)
                    Pref.restoreAnimationDuration = value
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
        binding.restoreDurationTextView.text = getString(R.string.restore_animation_duration, Pref.restoreAnimationDuration)
        val frictionMin = 0.1f
        val frictionMax = 1.0f
        binding.viewDragFrictionSeekBar.apply {
            progress = map(Pref.viewDragFriction, frictionMin, frictionMax, progressMin, progressMax).roundToInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                    val value = map(progress.toFloat(), progressMin, progressMax, frictionMin, frictionMax)
                    binding.viewDragFrictionTextView.text =
                        getString(R.string.view_drag_friction, value)
                    Pref.viewDragFriction = value
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        }
        binding.viewDragFrictionTextView.text = getString(R.string.view_drag_friction, Pref.viewDragFriction)
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
}
